package com.example.saplingsales.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.OrderItem
import java.net.URL

class OrderItemsAdapter : RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder>() {
    
    private var items = listOf<OrderItem>()

    fun updateItems(newItems: List<OrderItem>?) {
        items = newItems?.filterNotNull() ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount() = items.size

    class OrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(item: OrderItem?) {
            if (item == null) {
                tvProductName.text = "Unknown Product"
                tvQuantity.text = "Quantity: -"
                tvPrice.text = "₹0.00"
                ivProductImage.setImageResource(R.drawable.box_icon)
                return
            }
            tvProductName.text = item.productName ?: "Unknown Product"
            tvQuantity.text = "Quantity: ${item.quantity}"
            tvPrice.text = "₹${String.format("%.2f", item.price)} × ${item.quantity} = ₹${String.format("%.2f", item.price * item.quantity)}"
            loadProductImage(item)
        }

        private fun loadProductImage(item: OrderItem) {
            try {
                ivProductImage.setImageResource(R.drawable.box_icon)
                ivProductImage.alpha = 0.5f

                if (!item.productImageUrl.isNullOrEmpty()) {
                    Thread {
                        try {
                            val url = URL(item.productImageUrl)
                            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                            itemView.post {
                                ivProductImage.alpha = 1.0f
                                ivProductImage.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            Log.e("OrderItemsAdapter", "Error loading URL image: ${e.message}")
                            itemView.post { loadBase64Image(item.productImage) }
                        }
                    }.start()
                } else if (!item.productImage.isNullOrEmpty()) {
                    loadBase64Image(item.productImage)
                } else {
                    ivProductImage.alpha = 1.0f
                    ivProductImage.setImageResource(R.drawable.box_icon)
                }
            } catch (e: Exception) {
                Log.e("OrderItemsAdapter", "Error loading image: ${e.message}")
                ivProductImage.alpha = 1.0f
                ivProductImage.setImageResource(R.drawable.box_icon)
            }
        }

        private fun loadBase64Image(base64String: String?) {
            if (base64String.isNullOrEmpty()) {
                ivProductImage.alpha = 1.0f
                ivProductImage.setImageResource(R.drawable.box_icon)
                return
            }
            try {
                val imageData = if (base64String.contains(",")) {
                    base64String.split(",")[1]
                } else {
                    base64String
                }
                val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    ivProductImage.alpha = 1.0f
                    ivProductImage.setImageBitmap(bitmap)
                } else {
                    ivProductImage.alpha = 1.0f
                    ivProductImage.setImageResource(R.drawable.box_icon)
                }
            } catch (e: Exception) {
                Log.e("OrderItemsAdapter", "Error decoding base64 image: ${e.message}")
                ivProductImage.alpha = 1.0f
                ivProductImage.setImageResource(R.drawable.box_icon)
            }
        }
    }
} 