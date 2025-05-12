package com.example.saplingsales.adapters

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
import com.example.saplingsales.models.Order
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.NumberFormat
import android.os.Handler
import android.os.Looper
import android.util.Log

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private val firestore = FirebaseFirestore.getInstance()
    private val ratedProducts = mutableMapOf<String, Int>()
    private val handler = Handler(Looper.getMainLooper())
    private val pendingRatings = mutableMapOf<String, Runnable>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view, onOrderClick) { order, rating ->
            submitRating(order, rating, view.context)
        }
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        val productId = order.items.firstOrNull()?.productId
        val rating = if (productId != null) ratedProducts[productId] else null
        holder.bind(order, rating ?: 0)
    }

    private fun submitRating(order: Order, rating: Int, context: android.content.Context) {
        order.items.firstOrNull()?.let { item ->
            // Remove any pending rating update
            pendingRatings[item.productId]?.let { handler.removeCallbacks(it) }

            // Update local state immediately for responsive UI
            ratedProducts[item.productId] = rating
            notifyItemChanged(currentList.indexOf(order))

            // Create new rating runnable
            val ratingRunnable = Runnable {
                val productRef = firestore.collection("saplingProducts").document(item.productId)
                
                productRef.get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val ratings = when (val ratingsData = doc.get("ratings")) {
                                is List<*> -> ratingsData.filterIsInstance<Map<String, Any>>()
                                else -> listOf()
                            }
                            
                            // Remove existing rating if any
                            val existingRating = ratings.find { it["userId"] == order.userId }
                            if (existingRating != null) {
                                productRef.update("ratings", FieldValue.arrayRemove(existingRating))
                                    .addOnSuccessListener {
                                        addNewRating(productRef, order, rating, item.productId, context)
                                    }
                                    .addOnFailureListener { e ->
                                        handleRatingError(item.productId, order, context, e.message ?: "Error removing existing rating")
                                    }
                            } else {
                                addNewRating(productRef, order, rating, item.productId, context)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        handleRatingError(item.productId, order, context, e.message ?: "Error fetching product")
                    }
            }

            // Store and schedule the new rating update
            pendingRatings[item.productId] = ratingRunnable
            handler.postDelayed(ratingRunnable, 300) // 300ms debounce
        }
    }

    private fun addNewRating(
        productRef: com.google.firebase.firestore.DocumentReference,
        order: Order,
        rating: Int,
        productId: String,
        context: android.content.Context
    ) {
        val ratingData = hashMapOf(
            "rating" to rating,
            "userId" to order.userId,
            "orderId" to order.orderId,
            "timestamp" to System.currentTimeMillis()
        )

        productRef.update("ratings", FieldValue.arrayUnion(ratingData))
            .addOnSuccessListener {
                updateAverageRating(productRef)
                handler.post {
                    Toast.makeText(context, "Rating updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                handleRatingError(productId, order, context, e.message ?: "Error updating rating")
            }
    }

    private fun handleRatingError(productId: String, order: Order, context: android.content.Context, errorMessage: String) {
        handler.post {
            ratedProducts.remove(productId)
            notifyItemChanged(currentList.indexOf(order))
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAverageRating(productRef: com.google.firebase.firestore.DocumentReference) {
        productRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val ratingsArray = document.get("ratings") as? List<*>
                        if (ratingsArray != null && ratingsArray.isNotEmpty()) {
                            val validRatings = ratingsArray.mapNotNull { rating ->
                                when (rating) {
                                    is Map<*, *> -> (rating["rating"] as? Number)?.toDouble()
                                    else -> null
                                }
                            }
                            
                            if (validRatings.isNotEmpty()) {
                                val average = validRatings.average()
                                productRef.update(
                                    mapOf(
                                        "averageRating" to average,
                                        "totalRatings" to validRatings.size
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersAdapter", "Error updating average rating: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OrdersAdapter", "Error fetching product for rating update: ${e.message}")
            }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Remove all pending rating updates
        pendingRatings.values.forEach { handler.removeCallbacks(it) }
        pendingRatings.clear()
    }

    class OrderViewHolder(
        itemView: View,
        private val onOrderClick: (Order) -> Unit,
        private val onRatingSubmit: (Order, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvDeliveryStatus: TextView = itemView.findViewById(R.id.tvDeliveryStatus)
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvRatingLabel: TextView = itemView.findViewById(R.id.tvRatingLabel)
        private val starViews = listOf<ImageView>(
            itemView.findViewById(R.id.star1),
            itemView.findViewById(R.id.star2),
            itemView.findViewById(R.id.star3),
            itemView.findViewById(R.id.star4),
            itemView.findViewById(R.id.star5)
        )

        fun bind(order: Order, existingRating: Int) {
            // Set order ID
            tvOrderId.text = "Order #${order.orderId}"

            // Set delivery status
            val deliveryDate = formatDate(order.createdAt)
            tvDeliveryStatus.text = "Delivered on $deliveryDate"

            order.items.firstOrNull()?.let { firstItem ->
                // Load and set product image
                val imageLoadingRunnable = Runnable {
                    try {
                        // First try loading from productImageUrl
                        if (!firstItem.productImageUrl.isNullOrEmpty()) {
                            try {
                                val url = java.net.URL(firstItem.productImageUrl)
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.doInput = true
                                connection.connect()
                                val inputStream = connection.inputStream
                                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                itemView.post {
                                    if (bitmap != null) {
                                        ivProductImage.setImageBitmap(bitmap)
                                    } else {
                                        // If URL image fails, try base64
                                        loadBase64Image(firstItem.productImage, ivProductImage)
                                    }
                                }
                                inputStream.close()
                                connection.disconnect()
                            } catch (e: Exception) {
                                Log.e("OrdersAdapter", "Error loading URL image: ${e.message}")
                                itemView.post {
                                    // If URL fails, try base64
                                    loadBase64Image(firstItem.productImage, ivProductImage)
                                }
                            }
                        } else if (!firstItem.productImage.isNullOrEmpty()) {
                            // If no URL, try base64
                            itemView.post {
                                loadBase64Image(firstItem.productImage, ivProductImage)
                            }
                        } else {
                            // If both are empty, show placeholder
                            itemView.post {
                                ivProductImage.setImageResource(R.drawable.box_icon)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersAdapter", "Error loading image: ${e.message}")
                        itemView.post {
                            ivProductImage.setImageResource(R.drawable.box_icon)
                        }
                    }
                }
                Thread(imageLoadingRunnable).start()

                // Set product name
                tvProductName.text = firstItem.productName

                // Set quantity
                tvQuantity.text = "Quantity: ${firstItem.quantity}"

                // Set total amount
                val formattedAmount = String.format("%.1f", order.totalAmount)
                tvTotalAmount.text = "₹${formattedAmount}"

                if (existingRating > 0) {
                    setupStarRating(order, existingRating)
                } else {
                    // Check Firestore for rating
                    checkIfRated(firstItem.productId, order.userId) { rating ->
                        setupStarRating(order, rating)
                    }
                }
            }

            // Set click listener for the entire item
            itemView.setOnClickListener { onOrderClick(order) }
        }

        private fun checkIfRated(productId: String, userId: String, callback: (Int) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection("saplingProducts")
                .document(productId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        val ratings = when (val ratingsData = document.get("ratings")) {
                            is List<*> -> ratingsData.filterIsInstance<Map<String, Any>>()
                            null -> listOf()
                            else -> listOf()
                        }
                        val userRating = ratings.find { it["userId"] == userId }
                        val rating = (userRating?.get("rating") as? Number)?.toInt() ?: 0
                        callback(rating)
                    } catch (e: Exception) {
                        callback(0)
                    }
                }
                .addOnFailureListener {
                    callback(0)
                }
        }

        private fun setupStarRating(order: Order, currentRating: Int) {
            tvRatingLabel.text = if (currentRating > 0) "Your rating" else "Rate this product"
            
            // Update star visuals
            updateStarVisuals(currentRating)
            
            // Setup click listeners for all stars
            starViews.forEachIndexed { index, starView ->
                starView.setOnClickListener {
                    val newRating = index + 1
                    if (newRating != currentRating) { // Only update if rating changed
                        onRatingSubmit(order, newRating)
                        updateStarVisuals(newRating)
                        tvRatingLabel.text = "Your rating"
                    }
                }
            }
        }

        private fun updateStarVisuals(rating: Int) {
            starViews.forEachIndexed { index, star ->
                star.setImageResource(if (index < rating) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
            val day = SimpleDateFormat("dd", Locale.getDefault()).format(date)
            return "$month $day"
        }

        private fun loadBase64Image(base64String: String?, imageView: ImageView) {
            if (base64String.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.box_icon)
                return
            }

            try {
                val imageData = if (base64String.contains(",")) {
                    base64String.split(",")[1]
                } else {
                    base64String
                }

                val decodedBytes = android.util.Base64.decode(imageData, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(R.drawable.box_icon)
                }
            } catch (e: Exception) {
                Log.e("OrdersAdapter", "Error decoding base64 image: ${e.message}")
                imageView.setImageResource(R.drawable.box_icon)
            }
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
} 