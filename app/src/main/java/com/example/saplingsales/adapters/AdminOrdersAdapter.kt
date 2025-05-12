package com.example.saplingsales.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.Order
import java.text.SimpleDateFormat
import java.util.*

class AdminOrdersAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, AdminOrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
        holder.itemView.setOnClickListener { onOrderClick(order) }
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)

        fun bind(order: Order) {
            tvOrderId.text = "Order #${order.orderId}"
            tvUserEmail.text = order.customerEmail
            tvOrderDate.text = formatDate(order.createdAt)
            tvTotalAmount.text = "₹${order.totalAmount}"
            tvOrderStatus.text = order.status
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
} 