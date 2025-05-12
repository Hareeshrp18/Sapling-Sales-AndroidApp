package com.example.saplingsales.models

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val paymentId: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "",
    val createdAt: Long = 0,
    val items: List<OrderItem> = emptyList(),
    val shippingAddress: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = ""
)

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val productImage: String = "",
    val productImageUrl: String = "",
    val productCategory: String = ""
) 