package com.example.saplingsales.models

import java.io.Serializable

data class CartItem(
    var id: String = "",
    val userId: String = "",
    val productId: String = "",
    var quantity: Int = 0,
    val price: Double = 0.0,
    val productName: String = "",
    val productImage: String = "",
    val productImageUrl: String = "",
    val productCategory: String = "",
    val addedAt: Long = 0,
    val status: String = "pending",
    var availableQuantity: Int = 0
) : Serializable 