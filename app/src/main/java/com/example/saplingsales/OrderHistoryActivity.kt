package com.example.saplingsales

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OrderHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        val recyclerView = findViewById<RecyclerView>(R.id.orderHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Dummy data for order history
        val orderList = listOf(
            OrderItem("Order #12345", "Delivered", "March 5, 2025"),
            OrderItem("Order #12346", "Shipped", "March 7, 2025"),
            OrderItem("Order #12347", "Processing", "March 8, 2025")
        )

        val adapter = OrderHistoryAdapter(orderList)
        recyclerView.adapter = adapter
    }
}
