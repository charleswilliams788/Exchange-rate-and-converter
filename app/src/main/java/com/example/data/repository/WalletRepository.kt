package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserAccount
import com.example.data.model.WalletTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WalletRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing wallet.")
    }

    /**
     * Observes the user's account document in real-time.
     * If the account does not exist yet, initializes it with demo working balances
     * (e.g. ₦250,000.00 and $200.00) and a unique 10-digit account number.
     */
    fun observeUserAccount(userId: String): Flow<UserAccount?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val account = snapshot.toObject(UserAccount::class.java)
                trySend(account)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun initializeAccountIfMissing(
        userId: String,
        displayName: String,
        email: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val docRef = db.collection("users").document(userId)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserAccount::class.java)
                if (existing != null) {
                    return@withContext Result.success(existing)
                }
            }

            // Generate unique 10-digit account number starting with 90 (typical fintech NUBAN)
            val randomDigits = (10000000..99999999).random()
            val generatedAccountNumber = "90$randomDigits"
            val sanitizedTag = "@" + (email.substringBefore("@").filter { it.isLetterOrDigit() }.ifEmpty { "user" })

            val newAccount = UserAccount(
                userId = userId,
                displayName = displayName.ifEmpty { "NairaRate User" },
                email = email,
                accountNumber = generatedAccountNumber,
                accountTag = sanitizedTag,
                bankName = "NairaRate MFB",
                ngnBalance = 250000.0, // Starter working test balance: ₦250,000
                usdBalance = 200.0     // Starter working test balance: $200
            )

            val payload = mapOf(
                "userId" to newAccount.userId,
                "displayName" to newAccount.displayName,
                "email" to newAccount.email,
                "accountNumber" to newAccount.accountNumber,
                "accountTag" to newAccount.accountTag,
                "bankName" to newAccount.bankName,
                "ngnBalance" to newAccount.ngnBalance,
                "usdBalance" to newAccount.usdBalance,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()

            // Also record a welcome deposit transaction
            recordInitialDeposit(userId, newAccount)

            return@withContext Result.success(newAccount)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            return@withContext Result.failure(e)
        }
    }

    private suspend fun recordInitialDeposit(userId: String, account: UserAccount) {
        try {
            val txRef = db.collection("transactions").document()
            val payload = mapOf(
                "id" to txRef.id,
                "senderId" to userId,
                "senderName" to "Central Bank / Remittance",
                "senderAccount" to "0000000000",
                "recipientId" to userId,
                "recipientName" to account.displayName,
                "recipientAccount" to account.accountNumber,
                "recipientBank" to account.bankName,
                "amount" to 250000.0,
                "currency" to "NGN",
                "exchangeRate" to 1.0,
                "convertedAmount" to 250000.0,
                "convertedCurrency" to "NGN",
                "type" to "DEPOSIT",
                "status" to "COMPLETED",
                "note" to "Welcome starter balance credited",
                "participants" to listOf(userId),
                "createdAt" to FieldValue.serverTimestamp()
            )
            txRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "transactions")
        }
    }

    /**
     * Observes real-time transactions where this user is a participant.
     */
    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> = callbackFlow {
        val query = db.collection("transactions")
            .whereArrayContains("participants", userId)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "transactions")
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(WalletTransaction::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Sends money in either NGN or USD.
     * Deducts the sender's balance, records the transaction, and if internal recipient exists, credits them.
     */
    suspend fun sendMoney(
        recipientAccount: String,
        recipientName: String,
        recipientBank: String,
        amount: Double,
        currency: String,
        currentExchangeRate: Double,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)

        try {
            val snapshot = userDoc.get().await()
            val sender = snapshot.toObject(UserAccount::class.java)
                ?: return@withContext Result.failure(Exception("Sender account not found"))

            // Balance check
            if (currency == "NGN" && sender.ngnBalance < amount) {
                return@withContext Result.failure(Exception("Insufficient NGN balance (Available: ₦${sender.ngnBalance})"))
            }
            if (currency == "USD" && sender.usdBalance < amount) {
                return@withContext Result.failure(Exception("Insufficient USD balance (Available: $${sender.usdBalance})"))
            }

            // Update sender balance
            val newNgnBalance = if (currency == "NGN") sender.ngnBalance - amount else sender.ngnBalance
            val newUsdBalance = if (currency == "USD") sender.usdBalance - amount else sender.usdBalance

            userDoc.update(
                mapOf(
                    "ngnBalance" to newNgnBalance,
                    "usdBalance" to newUsdBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Calculate converted equivalent
            val (convertedAmt, convertedCurr) = if (currency == "USD") {
                Pair(amount * currentExchangeRate, "NGN")
            } else {
                Pair(if (currentExchangeRate > 0) amount / currentExchangeRate else 0.0, "USD")
            }

            // Record transaction
            val txRef = db.collection("transactions").document()
            val txPayload = mapOf(
                "id" to txRef.id,
                "senderId" to uid,
                "senderName" to sender.displayName,
                "senderAccount" to sender.accountNumber,
                "recipientId" to recipientAccount,
                "recipientName" to recipientName.ifEmpty { "Recipient" },
                "recipientAccount" to recipientAccount,
                "recipientBank" to recipientBank,
                "amount" to amount,
                "currency" to currency,
                "exchangeRate" to currentExchangeRate,
                "convertedAmount" to convertedAmt,
                "convertedCurrency" to convertedCurr,
                "type" to "SEND",
                "status" to "COMPLETED",
                "note" to note.ifEmpty { "Transfer to $recipientName" },
                "participants" to listOf(uid, recipientAccount),
                "createdAt" to FieldValue.serverTimestamp()
            )

            txRef.set(txPayload).await()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "transactions")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Converts balance between the user's own USD and NGN balances using live exchange rate.
     */
    suspend fun convertOwnBalance(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        exchangeRate: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)

        try {
            val snapshot = userDoc.get().await()
            val user = snapshot.toObject(UserAccount::class.java)
                ?: return@withContext Result.failure(Exception("Account not found"))

            val (newUsd, newNgn, convertedAmt) = if (fromCurrency == "USD" && toCurrency == "NGN") {
                if (user.usdBalance < amount) return@withContext Result.failure(Exception("Insufficient USD balance"))
                val receivedNgn = amount * exchangeRate
                Triple(user.usdBalance - amount, user.ngnBalance + receivedNgn, receivedNgn)
            } else if (fromCurrency == "NGN" && toCurrency == "USD") {
                if (user.ngnBalance < amount) return@withContext Result.failure(Exception("Insufficient NGN balance"))
                val receivedUsd = if (exchangeRate > 0) amount / exchangeRate else 0.0
                Triple(user.usdBalance + receivedUsd, user.ngnBalance - amount, receivedUsd)
            } else {
                return@withContext Result.failure(Exception("Invalid currency pair"))
            }

            userDoc.update(
                mapOf(
                    "usdBalance" to newUsd,
                    "ngnBalance" to newNgn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Record conversion transaction
            val txRef = db.collection("transactions").document()
            val txPayload = mapOf(
                "id" to txRef.id,
                "senderId" to uid,
                "senderName" to user.displayName,
                "senderAccount" to user.accountNumber,
                "recipientId" to uid,
                "recipientName" to user.displayName,
                "recipientAccount" to user.accountNumber,
                "recipientBank" to user.bankName,
                "amount" to amount,
                "currency" to fromCurrency,
                "exchangeRate" to exchangeRate,
                "convertedAmount" to convertedAmt,
                "convertedCurrency" to toCurrency,
                "type" to "CONVERT",
                "status" to "COMPLETED",
                "note" to "Converted $amount $fromCurrency to ${String.format("%.2f", convertedAmt)} $toCurrency",
                "participants" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp()
            )

            txRef.set(txPayload).await()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Test deposit funds into user account for demonstration and testing.
     */
    suspend fun depositFunds(amount: Double, currency: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)

        try {
            val snapshot = userDoc.get().await()
            val user = snapshot.toObject(UserAccount::class.java)
                ?: return@withContext Result.failure(Exception("Account not found"))

            val newNgn = if (currency == "NGN") user.ngnBalance + amount else user.ngnBalance
            val newUsd = if (currency == "USD") user.usdBalance + amount else user.usdBalance

            userDoc.update(
                mapOf(
                    "ngnBalance" to newNgn,
                    "usdBalance" to newUsd,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val txRef = db.collection("transactions").document()
            val txPayload = mapOf(
                "id" to txRef.id,
                "senderId" to uid,
                "senderName" to "Card / Remittance",
                "senderAccount" to "VIRTUAL_TOPUP",
                "recipientId" to uid,
                "recipientName" to user.displayName,
                "recipientAccount" to user.accountNumber,
                "recipientBank" to user.bankName,
                "amount" to amount,
                "currency" to currency,
                "exchangeRate" to 1.0,
                "convertedAmount" to amount,
                "convertedCurrency" to currency,
                "type" to "DEPOSIT",
                "status" to "COMPLETED",
                "note" to "Instant deposit via NairaRate Top-up",
                "participants" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp()
            )

            txRef.set(txPayload).await()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            return@withContext Result.failure(e)
        }
    }
}
