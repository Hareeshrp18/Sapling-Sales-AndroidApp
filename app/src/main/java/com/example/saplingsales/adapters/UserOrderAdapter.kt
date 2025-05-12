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

class UserOrderAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, UserOrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view, onOrderClick)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(
        itemView: View,
        private val onOrderClick: (Order) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(order: Order) {
            tvOrderId.text = "Order #${order.orderId.take(8)}"
            tvOrderDate.text = formatDate(order.createdAt)
            tvTotalAmount.text = "₹${order.totalAmount}"
            tvStatus.text = order.status.capitalize()

            itemView.setOnClickListener {
                onOrderClick(order)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
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