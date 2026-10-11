package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val accountNumber: String = "",
    val accountTag: String = "",
    val bankName: String = "NairaRate MFB",
    val ngnBalance: Double = 0.0,
    val usdBalance: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
