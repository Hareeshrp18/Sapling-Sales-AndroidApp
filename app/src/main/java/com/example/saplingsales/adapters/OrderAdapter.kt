package com.example.saplingsales.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.Order

class OrderAdapter(
    private val context: Context,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var orders = mutableListOf<Order>()

    fun updateOrders(newOrders: List<Order>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.bind(order)
    }

    override fun getItemCount() = orders.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(order: Order) {
            // Set order ID
            tvOrderId.text = "Order #${order.orderId}"
            
            // Format date
            tvOrderDate.text = formatDate(order.createdAt)

            // Set status (green pill)
            tvStatus.visibility = View.VISIBLE
            tvStatus.setBackgroundResource(R.drawable.status_background_confirmed)

            // Get first item from order for display
            order.items.firstOrNull()?.let { firstItem ->
                // Set product name
                tvProductName.text = firstItem.productName

                // Set quantity
                tvQuantity.text = "Quantity: ${firstItem.quantity}"

                // Set total amount with ₹ symbol
                tvTotalAmount.text = "₹${firstItem.price * firstItem.quantity}"
                
                // Always use the box icon
                ivProductImage.setImageResource(R.drawable.box_icon)
            }

            itemView.setOnClickListener { onOrderClick(order) }
        }

        private fun formatDate(timestamp: Long): String {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
            return format.format(date)
        }
    }
} 