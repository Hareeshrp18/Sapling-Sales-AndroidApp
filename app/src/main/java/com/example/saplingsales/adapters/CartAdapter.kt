package com.example.saplingsales.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.CartItem
import android.graphics.BitmapFactory
import android.util.Base64
import android.graphics.Bitmap
import java.net.URL

class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onRemoveItem: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val productName: TextView = itemView.findViewById(R.id.tvProductName)
        private val productPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        private val availableText: TextView = itemView.findViewById(R.id.tvAvailable)
        private val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        private val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(cartItem: CartItem) {
            productName.text = cartItem.productName
            productPrice.text = "₹${cartItem.price}"
            quantityText.text = cartItem.quantity.toString()
            availableText.text = "Available: ${cartItem.availableQuantity}"

            // Enable/disable buttons based on quantity limits
            btnIncrease.isEnabled = cartItem.quantity < cartItem.availableQuantity
            btnDecrease.isEnabled = cartItem.quantity > 1

            // Update button alpha for visual feedback
            btnIncrease.alpha = if (btnIncrease.isEnabled) 1.0f else 0.5f
            btnDecrease.alpha = if (btnDecrease.isEnabled) 1.0f else 0.5f

            btnIncrease.setOnClickListener {
                if (cartItem.quantity < cartItem.availableQuantity) {
                    val newQuantity = cartItem.quantity + 1
                    onQuantityChanged(cartItem, newQuantity)
                }
            }

            btnDecrease.setOnClickListener {
                if (cartItem.quantity > 1) {
                    val newQuantity = cartItem.quantity - 1
                    onQuantityChanged(cartItem, newQuantity)
                }
            }

            btnRemove.setOnClickListener {
                onRemoveItem(cartItem)
            }

            // Load product image
            if (!cartItem.productImage.isNullOrEmpty()) {
                try {
                    if (cartItem.productImage.startsWith("data:image") || cartItem.productImage.length > 200) {
                        // Base64 image
                        val base64Image = if (cartItem.productImage.contains(",")) {
                            cartItem.productImage.split(",")[1]
                        } else {
                            cartItem.productImage
                        }
                        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        productImage.setImageBitmap(bitmap)
                    } else {
                        // URL image
                        Thread {
                            try {
                                val url = URL(cartItem.productImage)
                                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                                itemView.post {
                                    productImage.setImageBitmap(bitmap)
                                }
                            } catch (e: Exception) {
                                itemView.post {
                                    productImage.setImageResource(R.drawable.placeholder_image)
                                }
                            }
                        }.start()
                    }
                } catch (e: Exception) {
                    productImage.setImageResource(R.drawable.placeholder_image)
                }
            } else if (!cartItem.productImageUrl.isNullOrEmpty()) {
                Thread {
                    try {
                        val url = URL(cartItem.productImageUrl)
                        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                        itemView.post {
                            productImage.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        itemView.post {
                            productImage.setImageResource(R.drawable.placeholder_image)
                        }
                    }
                }.start()
            } else {
                productImage.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    private class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
} 