package com.example.saplingsales.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.URL
import android.util.Log

class UserProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onAddToCartClick: (Product) -> Unit
) : ListAdapter<Product, UserProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val nameText: TextView = itemView.findViewById(R.id.tvProductName)
        private val priceText: TextView = itemView.findViewById(R.id.tvPrice)
        private val categoryText: TextView = itemView.findViewById(R.id.tvCategory)
        private val ratingText: TextView = itemView.findViewById(R.id.tvRating)
        private val favoriteButton: ImageView = itemView.findViewById(R.id.btnFavorite)

        fun bind(product: Product) {
            nameText.text = product.name
            priceText.text = currencyFormatter.format(product.price)
            categoryText.text = product.category

            // Set rating
            if (product.totalRatings > 0) {
                val formattedRating = String.format("%.1f", product.averageRating)
                ratingText.text = "$formattedRating (${product.totalRatings})"
                ratingText.visibility = View.VISIBLE
            } else {
                ratingText.text = "No ratings"
                ratingText.visibility = View.VISIBLE
            }

            // Set default placeholder image
            imageView.setImageResource(R.drawable.placeholder_image)

            // Load product image
            when {
                !product.imageBase64.isNullOrEmpty() -> {
                    try {
                        val imageBytes = Base64.decode(product.imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                }
                product.imageUrls.isNotEmpty() -> {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val url = URL(product.imageUrls[0])
                            val connection = url.openConnection()
                            connection.doInput = true
                            connection.connect()
                            val input: InputStream = connection.getInputStream()
                            val bitmap = BitmapFactory.decodeStream(input)
                            withContext(Dispatchers.Main) {
                                imageView.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                imageView.setImageResource(R.drawable.placeholder_image)
                            }
                        }
                    }
                }
                product.images.isNotEmpty() -> {
                    val imageString = product.images[0]
                    if (imageString.startsWith("data:image")) {
                        try {
                            val base64Image = imageString.substring(imageString.indexOf(",") + 1)
                            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            imageView.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            imageView.setImageResource(R.drawable.placeholder_image)
                        }
                    } else {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL(imageString)
                                val connection = url.openConnection()
                                connection.doInput = true
                                connection.connect()
                                val input: InputStream = connection.getInputStream()
                                val bitmap = BitmapFactory.decodeStream(input)
                                withContext(Dispatchers.Main) {
                                    imageView.setImageBitmap(bitmap)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    imageView.setImageResource(R.drawable.placeholder_image)
                                }
                            }
                        }
                    }
                }
            }

            // Check if product is in favorites
            checkFavoriteStatus(product.id)

            itemView.setOnClickListener { onProductClick(product) }
            
            favoriteButton.setOnClickListener {
                toggleFavorite(product)
            }
        }

        private fun checkFavoriteStatus(productId: String) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_border)
                return
            }

            val favoriteId = "${userId}_${productId}"
            firestore.collection("favouriteProduct")
                .document(favoriteId)
                .get()
                .addOnSuccessListener { document ->
                    favoriteButton.setImageResource(
                        if (document.exists()) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_border
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("UserProductAdapter", "Error checking favorite status: ${e.message}")
                    favoriteButton.setImageResource(R.drawable.ic_favorite_border)
                }
        }

        private fun toggleFavorite(product: Product) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(itemView.context, "Please login to add favorites", Toast.LENGTH_SHORT).show()
                return
            }

            // Show loading state
            favoriteButton.isEnabled = false

            // Create the document ID using userId and productId to ensure uniqueness
            val favoriteId = "${userId}_${product.id}"

            firestore.collection("favouriteProduct")
                .document(favoriteId)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        // Add to favorites
                        val favorite = hashMapOf(
                            "id" to favoriteId,
                            "userId" to userId,
                            "productId" to product.id,
                            "productName" to product.name,
                            "productPrice" to product.price,
                            "productDescription" to (product.description ?: ""),
                            "productCategory" to (product.category ?: ""),
                            "addedAt" to System.currentTimeMillis()
                        )

                        // Add image data based on availability
                        when {
                            !product.imageBase64.isNullOrEmpty() -> 
                                favorite["productImage"] = product.imageBase64
                            product.imageUrls.isNotEmpty() -> 
                                favorite["productImageUrl"] = product.imageUrls[0]
                            product.images.isNotEmpty() -> 
                                favorite["productImage"] = product.images[0]
                        }

                        // Use set with merge to handle document creation
                        firestore.collection("favouriteProduct")
                            .document(favoriteId)
                            .set(favorite)
                            .addOnSuccessListener {
                                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                                Toast.makeText(itemView.context, "Added to favorites", Toast.LENGTH_SHORT).show()
                                favoriteButton.isEnabled = true
                            }
                            .addOnFailureListener { e ->
                                Log.e("UserProductAdapter", "Error adding to favorites: ${e.message}")
                                Toast.makeText(itemView.context, "Error adding to favorites", Toast.LENGTH_SHORT).show()
                                favoriteButton.isEnabled = true
                                favoriteButton.setImageResource(R.drawable.ic_favorite_border)
                            }
                    } else {
                        // Remove from favorites
                        firestore.collection("favouriteProduct")
                            .document(favoriteId)
                            .delete()
                            .addOnSuccessListener {
                                favoriteButton.setImageResource(R.drawable.ic_favorite_border)
                                Toast.makeText(itemView.context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                favoriteButton.isEnabled = true
                            }
                            .addOnFailureListener { e ->
                                Log.e("UserProductAdapter", "Error removing from favorites: ${e.message}")
                                Toast.makeText(itemView.context, "Error removing from favorites", Toast.LENGTH_SHORT).show()
                                favoriteButton.isEnabled = true
                                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UserProductAdapter", "Error checking favorite status: ${e.message}")
                    Toast.makeText(itemView.context, "Error updating favorites", Toast.LENGTH_SHORT).show()
                    favoriteButton.isEnabled = true
                }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.cancel()
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
} 