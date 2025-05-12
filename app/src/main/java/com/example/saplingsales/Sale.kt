package com.example.saplingsales

data class Sale(
    var id: String = "",
    var productName: String = "",
    var quantity: Int = 0,
    var totalPrice: Double = 0.0,
    var userName: String = "",
    var userMobile: String = "",
    var productImage: String = "" // Base64 or URL
)
