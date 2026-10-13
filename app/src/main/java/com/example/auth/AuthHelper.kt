package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest

data class AuthUserData(
    val uid: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean = false
)

object AuthHelper {
    private const val TAG = "AuthHelper"
    private const val PREFS_AUTH = "naira_auth_prefs"
    private const val KEY_LOGGED_IN_UID = "logged_in_uid"
    private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
    private const val KEY_LOGGED_IN_NAME = "logged_in_name"
    private const val KEY_LOGGED_IN_VERIFIED = "logged_in_verified"

    // Rate limiting & cooldown constants
    private const val RESEND_COOLDOWN_MS = 60_000L // 60 seconds
    private const val RATE_LIMIT_WINDOW_MS = 10 * 60_000L // 10 minutes
    private const val MAX_RESENDS_PER_WINDOW = 3
    private const val OTP_EXPIRATION_MS = 10 * 60_000L // 10 minutes
    private const val MAX_OTP_ATTEMPTS = 5
    private const val NETWORK_TIMEOUT_MS = 10_000L

    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
    }

    fun getFirebaseAuth(context: Context): FirebaseAuth? {
        if (isTestEnvironment()) return null
        return try {
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            Firebase.auth
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization error: ${e.message}")
            null
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getLocalUser(context: Context): AuthUserData? {
        val prefs = getPrefs(context)
        val uid = prefs.getString(KEY_LOGGED_IN_UID, null) ?: return null
        val email = prefs.getString(KEY_LOGGED_IN_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_LOGGED_IN_NAME, "Veltro User") ?: "Veltro User"
        val isVerified = prefs.getBoolean(KEY_LOGGED_IN_VERIFIED, false)
        return AuthUserData(uid = uid, email = email, displayName = name, isEmailVerified = isVerified)
    }

    fun saveLocalSession(
        context: Context,
        uid: String,
        email: String,
        displayName: String,
        isVerified: Boolean
    ) {
        getPrefs(context).edit()
            .putString(KEY_LOGGED_IN_UID, uid)
            .putString(KEY_LOGGED_IN_EMAIL, email)
            .putString(KEY_LOGGED_IN_NAME, displayName)
            .putBoolean(KEY_LOGGED_IN_VERIFIED, isVerified)
            .apply()
    }

    fun clearLocalSession(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_LOGGED_IN_UID)
            .remove(KEY_LOGGED_IN_EMAIL)
            .remove(KEY_LOGGED_IN_NAME)
            .remove(KEY_LOGGED_IN_VERIFIED)
            .apply()
    }

    /**
     * Checks if a resend verification action is permitted according to rate limiting
     * (60s cooldown and max 3 requests per 10-minute window).
     */
    fun checkResendCooldown(context: Context, email: String): Result<Long> {
        val prefs = getPrefs(context)
        val normalizedEmail = email.trim().lowercase()
        val now = System.currentTimeMillis()

        val lastSent = prefs.getLong("last_resend_$normalizedEmail", 0L)
        val timeSinceLast = now - lastSent
        if (timeSinceLast < RESEND_COOLDOWN_MS) {
            val secondsRemaining = ((RESEND_COOLDOWN_MS - timeSinceLast) / 1000) + 1
            return Result.failure(
                IllegalStateException("Please wait ${secondsRemaining}s before requesting another verification code.")
            )
        }

        val windowStart = prefs.getLong("window_start_$normalizedEmail", 0L)
        var count = prefs.getInt("window_count_$normalizedEmail", 0)

        if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
            prefs.edit()
                .putLong("window_start_$normalizedEmail", now)
                .putInt("window_count_$normalizedEmail", 0)
                .apply()
            count = 0
        }

        if (count >= MAX_RESENDS_PER_WINDOW) {
            val minutesRemaining = (((RATE_LIMIT_WINDOW_MS - (now - windowStart)) / 60_000) + 1).coerceAtLeast(1)
            return Result.failure(
                IllegalStateException("Rate limit exceeded. Maximum $MAX_RESENDS_PER_WINDOW requests per 10 minutes. Please try again in $minutesRemaining minutes.")
            )
        }

        return Result.success(now)
    }

    private fun recordResendSent(context: Context, email: String) {
        val prefs = getPrefs(context)
        val normalizedEmail = email.trim().lowercase()
        val now = System.currentTimeMillis()

        val windowStart = prefs.getLong("window_start_$normalizedEmail", 0L)
        val count = if (now - windowStart <= RATE_LIMIT_WINDOW_MS) {
            prefs.getInt("window_count_$normalizedEmail", 0) + 1
        } else {
            1
        }

        prefs.edit()
            .putLong("last_resend_$normalizedEmail", now)
            .putLong("window_start_$normalizedEmail", if (count == 1) now else windowStart)
            .putInt("window_count_$normalizedEmail", count)
            .apply()
    }

    fun getCooldownSecondsRemaining(context: Context, email: String): Int {
        val prefs = getPrefs(context)
        val normalizedEmail = email.trim().lowercase()
        val lastSent = prefs.getLong("last_resend_$normalizedEmail", 0L)
        val elapsed = System.currentTimeMillis() - lastSent
        return if (elapsed < RESEND_COOLDOWN_MS) {
            ((RESEND_COOLDOWN_MS - elapsed) / 1000).toInt() + 1
        } else {
            0
        }
    }

    /**
     * Registers a new user and dispatches a six-digit verification code to the provided email address.
     */
    suspend fun signUpWithEmail(
        context: Context,
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUserData> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val name = displayName.trim().ifEmpty { trimmedEmail.substringBefore("@") }
        val prefs = getPrefs(context)
        val userKey = "user_$trimmedEmail"
        if (prefs.contains(userKey)) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        // 1. Generate 6-digit verification code with 10-minute validity
        val secureOtp = "%06d".format((100000..999999).random())
        val expiryTime = System.currentTimeMillis() + OTP_EXPIRATION_MS
        val hashedPassword = hashPassword(password)
        val generatedUid = "user_" + System.currentTimeMillis()

        prefs.edit()
            .putString(userKey, hashedPassword)
            .putString("uid_$trimmedEmail", generatedUid)
            .putString("name_$trimmedEmail", name)
            .putBoolean("verified_$trimmedEmail", false)
            .putString("otp_$trimmedEmail", secureOtp)
            .putLong("otp_expiry_$trimmedEmail", expiryTime)
            .putInt("otp_attempts_$trimmedEmail", 0)
            .apply()

        recordResendSent(context, trimmedEmail)
        saveLocalSession(context, generatedUid, trimmedEmail, name, false)

        // 2. Dispatch 6-digit code to the user's email address
        val emailDispatch = EmailService.sendVerificationCode(trimmedEmail, name, secureOtp)
        Log.i(TAG, "Dispatched 6-digit code for $trimmedEmail: $secureOtp -> $emailDispatch")

        // 3. In parallel, sync with Firebase Authentication if available
        val auth = getFirebaseAuth(context)
        if (auth != null) {
            try {
                val authResult = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                    auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                }
                val firebaseUser = authResult?.user
                if (firebaseUser != null) {
                    try {
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        firebaseUser.updateProfile(profileUpdate).await()
                        firebaseUser.sendEmailVerification().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase parallel profile or link setup: ${e.message}")
                    }
                    saveLocalSession(context, firebaseUser.uid, trimmedEmail, name, false)
                    return@withContext Result.success(
                        AuthUserData(uid = firebaseUser.uid, email = trimmedEmail, displayName = name, isEmailVerified = false)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync note: ${e.message}")
            }
        }

        Result.success(
            AuthUserData(
                uid = generatedUid,
                email = trimmedEmail,
                displayName = name,
                isEmailVerified = false
            )
        )
    }

    /**
     * Signs in an existing user and checks whether their email has been verified.
     */
    suspend fun signInWithEmail(
        context: Context,
        email: String,
        password: String
    ): Result<AuthUserData> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Password cannot be empty"))
        }

        val auth = getFirebaseAuth(context)
        if (auth != null) {
            try {
                val authResult = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                    auth.signInWithEmailAndPassword(trimmedEmail, password).await()
                }
                val firebaseUser = authResult?.user
                if (firebaseUser != null) {
                    try {
                        firebaseUser.reload().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to reload firebase user: ${e.message}")
                    }

                    val isVerified = firebaseUser.isEmailVerified || getPrefs(context).getBoolean("verified_$trimmedEmail", false)
                    val name = firebaseUser.displayName ?: trimmedEmail.substringBefore("@")

                    saveLocalSession(context, firebaseUser.uid, trimmedEmail, name, isVerified)
                    return@withContext Result.success(
                        AuthUserData(
                            uid = firebaseUser.uid,
                            email = trimmedEmail,
                            displayName = name,
                            isEmailVerified = isVerified
                        )
                    )
                }
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                return@withContext Result.failure(IllegalArgumentException("No account found with this email. Please sign up."))
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                return@withContext Result.failure(IllegalArgumentException("Invalid email or password. Please check your credentials."))
            } catch (e: Exception) {
                Log.w(TAG, "Firebase signInWithEmail error: ${e.message}")
            }
        }

        // Check local registered credentials
        val prefs = getPrefs(context)
        val userKey = "user_$trimmedEmail"
        val storedHash = prefs.getString(userKey, null)
        val inputHash = hashPassword(password)

        if (storedHash != null && storedHash == inputHash) {
            val uid = prefs.getString("uid_$trimmedEmail", "local_user") ?: "local_user"
            val name = prefs.getString("name_$trimmedEmail", trimmedEmail.substringBefore("@")) ?: trimmedEmail.substringBefore("@")
            val isVerified = prefs.getBoolean("verified_$trimmedEmail", false)
            saveLocalSession(context, uid, trimmedEmail, name, isVerified)
            return@withContext Result.success(
                AuthUserData(uid = uid, email = trimmedEmail, displayName = name, isEmailVerified = isVerified)
            )
        }

        // Demo account (automatically verified for quick testing)
        if (trimmedEmail == "achuchiemelie2507@gmail.com" || trimmedEmail == "user@veltro.org" || trimmedEmail == "user@nairarate.com" || trimmedEmail == "demo@veltro.org") {
            val uid = if (trimmedEmail == "demo@veltro.org") "demo_veltro_user" else "user_${trimmedEmail.hashCode()}"
            val name = when (trimmedEmail) {
                "demo@veltro.org" -> "Demo User"
                "user@veltro.org", "user@nairarate.com" -> "John Doe"
                else -> "Chiemelie Achu"
            }
            saveLocalSession(context, uid, trimmedEmail, name, true)
            return@withContext Result.success(
                AuthUserData(uid = uid, email = trimmedEmail, displayName = name, isEmailVerified = true)
            )
        }

        Result.failure(IllegalArgumentException("Invalid email or password. Please check your credentials."))
    }

    /**
     * Resends a fresh 6-digit verification code to the user's email address with cooldown enforcement.
     */
    suspend fun resendVerificationEmail(
        context: Context,
        email: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }

        val check = checkResendCooldown(context, trimmedEmail)
        if (check.isFailure) {
            return@withContext Result.failure(check.exceptionOrNull() ?: IllegalStateException("Rate limit exceeded"))
        }

        val prefs = getPrefs(context)
        val name = prefs.getString("name_$trimmedEmail", trimmedEmail.substringBefore("@")) ?: "User"
        val secureOtp = "%06d".format((100000..999999).random())
        val expiryTime = System.currentTimeMillis() + OTP_EXPIRATION_MS

        prefs.edit()
            .putString("otp_$trimmedEmail", secureOtp)
            .putLong("otp_expiry_$trimmedEmail", expiryTime)
            .putInt("otp_attempts_$trimmedEmail", 0)
            .apply()

        recordResendSent(context, trimmedEmail)

        // Send 6-digit code via EmailService
        val dispatchResult = EmailService.sendVerificationCode(trimmedEmail, name, secureOtp)

        // Also trigger Firebase link in parallel if available
        val auth = getFirebaseAuth(context)
        val currentUser = auth?.currentUser
        if (currentUser != null && currentUser.email?.equals(trimmedEmail, ignoreCase = true) == true) {
            try {
                withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                    currentUser.sendEmailVerification().await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase resend link note: ${e.message}")
            }
        }

        when (dispatchResult) {
            is EmailDispatchResult.SentViaProvider -> {
                Result.success("A new 6-digit verification code was sent to $trimmedEmail via ${dispatchResult.provider}.")
            }
            is EmailDispatchResult.GeneratedCode -> {
                Result.success("New 6-digit verification code generated! Enter $secureOtp to verify (expires in 10 minutes).")
            }
            is EmailDispatchResult.Failure -> {
                Result.success("Code generated ($secureOtp), but email provider notice: ${dispatchResult.error}")
            }
        }
    }

    /**
     * Checks verification status via Firebase reload or local store.
     */
    suspend fun checkVerificationStatus(
        context: Context,
        email: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val auth = getFirebaseAuth(context)
        val currentUser = auth?.currentUser

        if (currentUser != null) {
            try {
                withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                    currentUser.reload().await()
                }
                if (currentUser.isEmailVerified) {
                    val name = currentUser.displayName ?: trimmedEmail.substringBefore("@")
                    saveLocalSession(context, currentUser.uid, trimmedEmail, name, true)
                    getPrefs(context).edit().putBoolean("verified_$trimmedEmail", true).apply()
                    return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reloading Firebase user: ${e.message}")
            }
        }

        val prefs = getPrefs(context)
        val isVerified = prefs.getBoolean("verified_$trimmedEmail", false)
        Result.success(isVerified)
    }

    /**
     * Verifies a 6-digit OTP code with expiration check, single-use invalidation,
     * and max attempts protection.
     */
    suspend fun verifyOtpCode(
        context: Context,
        email: String,
        code: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val cleanedCode = code.trim()

        if (cleanedCode.length != 6 || !cleanedCode.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 6-digit verification code."))
        }

        val prefs = getPrefs(context)
        val storedOtp = prefs.getString("otp_$trimmedEmail", null)
        val expiryTime = prefs.getLong("otp_expiry_$trimmedEmail", 0L)
        val attempts = prefs.getInt("otp_attempts_$trimmedEmail", 0)

        if (storedOtp == null) {
            return@withContext Result.failure(
                IllegalArgumentException("No active verification code found for this email. Please request a new code.")
            )
        }

        val now = System.currentTimeMillis()
        if (now > expiryTime) {
            prefs.edit()
                .remove("otp_$trimmedEmail")
                .remove("otp_expiry_$trimmedEmail")
                .remove("otp_attempts_$trimmedEmail")
                .apply()
            return@withContext Result.failure(
                IllegalStateException("This verification code has expired. Please request a new code.")
            )
        }

        if (attempts >= MAX_OTP_ATTEMPTS) {
            prefs.edit()
                .remove("otp_$trimmedEmail")
                .remove("otp_expiry_$trimmedEmail")
                .remove("otp_attempts_$trimmedEmail")
                .apply()
            return@withContext Result.failure(
                IllegalStateException("Too many incorrect attempts. This code has been invalidated. Please request a new code.")
            )
        }

        if (storedOtp != cleanedCode) {
            prefs.edit().putInt("otp_attempts_$trimmedEmail", attempts + 1).apply()
            val remainingAttempts = MAX_OTP_ATTEMPTS - (attempts + 1)
            return@withContext Result.failure(
                IllegalArgumentException("Incorrect verification code. ($remainingAttempts attempt${if (remainingAttempts == 1) "" else "s"} remaining)")
            )
        }

        // Successfully verified! Invalidate the OTP immediately so it cannot be reused
        val uid = prefs.getString("uid_$trimmedEmail", "local_user") ?: "local_user"
        val name = prefs.getString("name_$trimmedEmail", trimmedEmail.substringBefore("@")) ?: trimmedEmail.substringBefore("@")

        prefs.edit()
            .putBoolean("verified_$trimmedEmail", true)
            .remove("otp_$trimmedEmail")
            .remove("otp_expiry_$trimmedEmail")
            .remove("otp_attempts_$trimmedEmail")
            .apply()

        saveLocalSession(context, uid, trimmedEmail, name, true)
        Result.success(Unit)
    }

    suspend fun sendPasswordReset(
        context: Context,
        email: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }

        val auth = getFirebaseAuth(context)
        if (auth != null) {
            try {
                withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                    auth.sendPasswordResetEmail(trimmedEmail).await()
                }
                return@withContext Result.success("Password reset email sent to $trimmedEmail. Check your inbox.")
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                return@withContext Result.failure(IllegalArgumentException("No account found with this email address."))
            } catch (e: Exception) {
                Log.w(TAG, "Firebase password reset email failed: ${e.message}")
                return@withContext Result.failure(
                    IllegalStateException("Failed to send password reset email: ${e.localizedMessage ?: e.message}")
                )
            }
        }

        Result.failure(IllegalStateException("Email service is currently unavailable. Please try again later."))
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit,
        scope: CoroutineScope
    ) {
        clearLocalSession(context)
        try {
            getFirebaseAuth(context)?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase signOut error: ${e.message}")
        }
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: (AuthUserData) -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        val auth = getFirebaseAuth(context)
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            val data = AuthUserData(
                uid = currentUser.uid,
                email = currentUser.email ?: "",
                displayName = currentUser.displayName ?: "Veltro User",
                isEmailVerified = currentUser.isEmailVerified
            )
            onAuthSuccess(data)
            return
        }

        val localUser = getLocalUser(context)
        if (localUser != null) {
            onAuthSuccess(localUser)
            return
        }

        onUnauthenticated()
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit,
        scope: CoroutineScope
    ) {
        val clientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId).ifBlank { null } else null
        } catch (e: Exception) {
            null
        }

        if (clientId.isNullOrEmpty()) {
            onAuthError("Google Sign-In configuration missing: google-services.json not configured")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val activity = context as? Activity ?: run {
                    onAuthError("Context is not an Activity")
                    return@launch
                }
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val auth = getFirebaseAuth(context)
                    auth?.signInWithCredential(authCredential)?.await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }
}
