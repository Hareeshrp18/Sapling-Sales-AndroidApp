package com.example.saplingsales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderHistoryAdapter(private val orderList: List<OrderItem>) :
    RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderNumber: TextView = view.findViewById(R.id.orderNumberTextView)
        val orderStatus: TextView = view.findViewById(R.id.orderStatusTextView)
        val orderDate: TextView = view.findViewById(R.id.orderDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orderList[position]
        holder.orderNumber.text = order.orderNumber
        holder.orderStatus.text = order.status
        holder.orderDate.text = order.date
    }

    override fun getItemCount(): Int = orderList.size
}
