package com.example.saplingsales.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = "Active",
    val createdAt: Long = System.currentTimeMillis()
) 