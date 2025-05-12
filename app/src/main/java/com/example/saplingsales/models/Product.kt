package com.example.saplingsales.models

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.io.Serializable

data class Product(
    var id: String = "",
    
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",
    
    @get:PropertyName("price")
    @set:PropertyName("price")
    var price: Double = 0.0,
    
    @get:PropertyName("quantity")
    @set:PropertyName("quantity")
    var quantity: Int = 0,
    
    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "",
    
    @get:PropertyName("imageBase64")
    @set:PropertyName("imageBase64")
    var imageBase64: String = "",
    
    @get:PropertyName("images")
    @set:PropertyName("images")
    var images: List<String> = listOf(),
    
    @get:PropertyName("imageUrls")
    @set:PropertyName("imageUrls")
    var imageUrls: List<String> = listOf(),
    
    @get:PropertyName("averageRating")
    @set:PropertyName("averageRating")
    var averageRating: Double = 0.0,
    
    @get:PropertyName("totalRatings")
    @set:PropertyName("totalRatings")
    var totalRatings: Int = 0,
    
    @ServerTimestamp
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Date? = null,
    
    @ServerTimestamp
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Date? = null
) : Serializable 