package com.example.saplingsales.models

import com.google.firebase.Timestamp

data class Feedback(
    var id: String = "",
    val userId: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "feedback",
    val sender: String = "user",
    val isAdminReply: Boolean = false
) 