package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAccount: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val recipientAccount: String = "",
    val recipientBank: String = "NairaRate MFB",
    val amount: Double = 0.0,
    val currency: String = "NGN", // USD or NGN
    val exchangeRate: Double = 1.0,
    val convertedAmount: Double = 0.0,
    val convertedCurrency: String = "USD",
    val type: String = "SEND", // SEND, RECEIVE, CONVERT, DEPOSIT
    val status: String = "COMPLETED",
    val note: String = "",
    val participants: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
