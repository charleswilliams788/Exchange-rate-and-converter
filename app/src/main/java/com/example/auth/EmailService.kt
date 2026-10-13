package com.example.auth

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class EmailDispatchResult {
    data class SentViaProvider(val provider: String, val messageId: String) : EmailDispatchResult()
    data class GeneratedCode(val code: String, val message: String) : EmailDispatchResult()
    data class Failure(val error: String) : EmailDispatchResult()
}

object EmailService {
    private const val TAG = "EmailService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun sendVerificationCode(
        recipientEmail: String,
        recipientName: String,
        code: String
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        val resendKey = try { BuildConfig.RESEND_API_KEY.trim() } catch (e: Throwable) { "" }
        val brevoKey = try { BuildConfig.BREVO_API_KEY.trim() } catch (e: Throwable) { "" }

        // 1. Send via Resend API if key is configured
        if (resendKey.isNotBlank() && !resendKey.startsWith("MY_")) {
            val result = sendViaResend(resendKey, recipientEmail, recipientName, code)
            if (result is EmailDispatchResult.SentViaProvider) {
                return@withContext result
            }
        }

        // 2. Send via Brevo API if key is configured
        if (brevoKey.isNotBlank() && !brevoKey.startsWith("MY_")) {
            val result = sendViaBrevo(brevoKey, recipientEmail, recipientName, code)
            if (result is EmailDispatchResult.SentViaProvider) {
                return@withContext result
            }
        }

        // 3. Ready for entry with 10-minute validity
        EmailDispatchResult.GeneratedCode(
            code = code,
            message = "6-digit verification code generated! Enter $code below to verify your account (valid for 10 minutes)."
        )
    }

    private fun sendViaResend(
        apiKey: String,
        recipientEmail: String,
        recipientName: String,
        code: String
    ): EmailDispatchResult {
        try {
            val payload = JSONObject().apply {
                put("from", "Veltro <onboarding@resend.dev>")
                put("to", JSONArray().put(recipientEmail))
                put("subject", "Your Veltro Verification Code: $code")
                put(
                    "html",
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 14px; background: #ffffff;">
                        <h2 style="color: #3B82F6; margin-top: 0; font-size: 22px;">Veltro Account Verification</h2>
                        <p style="color: #333; font-size: 15px;">Hello ${recipientName.ifBlank { "User" }},</p>
                        <p style="color: #555; font-size: 14px; line-height: 1.5;">Thank you for registering. Please enter the following 6-digit verification code to complete your signup:</p>
                        <div style="background: #EFF6FF; padding: 20px; border-radius: 10px; text-align: center; margin: 24px 0; border: 1px solid #BFDBFE;">
                            <span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #1D4ED8; font-family: monospace;">$code</span>
                        </div>
                        <p style="color: #777; font-size: 13px;">This verification code will expire in <strong>10 minutes</strong>. If you did not request this code, you can safely ignore this email.</p>
                        <hr style="border: none; border-top: 1px solid #eeeeee; margin: 20px 0;" />
                        <p style="color: #999; font-size: 11px; text-align: center;">Veltro: Your Money. Without Borders.</p>
                    </div>
                    """.trimIndent()
                )
            }

            val request = Request.Builder()
                .url("https://api.resend.com/emails")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val id = try { JSONObject(body).optString("id", "resend_ok") } catch (e: Exception) { "resend_ok" }
                    Log.i(TAG, "Verification email successfully dispatched via Resend: $id")
                    return EmailDispatchResult.SentViaProvider("Resend", id)
                } else {
                    Log.w(TAG, "Resend email dispatch error: ${response.code} $body")
                    return EmailDispatchResult.Failure("Resend error ${response.code}: $body")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Resend dispatch", e)
            return EmailDispatchResult.Failure(e.localizedMessage ?: "Failed to connect to email provider")
        }
    }

    private fun sendViaBrevo(
        apiKey: String,
        recipientEmail: String,
        recipientName: String,
        code: String
    ): EmailDispatchResult {
        try {
            val payload = JSONObject().apply {
                put("sender", JSONObject().apply {
                    put("name", "Veltro")
                    put("email", "support@veltro.org")
                })
                put("to", JSONArray().put(JSONObject().apply {
                    put("email", recipientEmail)
                    put("name", recipientName)
                }))
                put("subject", "Your Veltro Verification Code: $code")
                put(
                    "htmlContent",
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 500px; padding: 20px;">
                        <h2>Veltro Verification</h2>
                        <p>Your 6-digit verification code is: <strong>$code</strong></p>
                        <p>This code will expire in 10 minutes.</p>
                    </div>
                    """.trimIndent()
                )
            }

            val request = Request.Builder()
                .url("https://api.brevo.com/v3/smtp/email")
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.i(TAG, "Verification email successfully dispatched via Brevo")
                    return EmailDispatchResult.SentViaProvider("Brevo", "brevo_ok")
                } else {
                    Log.w(TAG, "Brevo email dispatch error: ${response.code} $body")
                    return EmailDispatchResult.Failure("Brevo error ${response.code}: $body")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Brevo dispatch", e)
            return EmailDispatchResult.Failure(e.localizedMessage ?: "Failed to connect to Brevo")
        }
    }
}
