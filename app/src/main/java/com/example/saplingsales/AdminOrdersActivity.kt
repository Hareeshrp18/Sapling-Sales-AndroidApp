package com.example.saplingsales

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.AdminOrdersAdapter
import com.example.saplingsales.models.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.content.Intent

class AdminOrdersActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var ordersAdapter: AdminOrdersAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoOrders: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        // Initialize views
        recyclerView = findViewById(R.id.rvOrders)
        progressBar = findViewById(R.id.progressBar)
        tvNoOrders = findViewById(R.id.tvNoOrders)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        ordersAdapter = AdminOrdersAdapter { order ->
            val intent = Intent(this, AdminOrderDetailActivity::class.java)
            intent.putExtra("orderId", order.orderId)
            startActivity(intent)
        }
        recyclerView.adapter = ordersAdapter

        // Load orders
        loadOrders()
    }

    private fun loadOrders() {
        progressBar.visibility = View.VISIBLE
        tvNoOrders.visibility = View.GONE

        db.collection("saplingOrders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE

                if (e != null) {
                    Log.e("AdminOrdersActivity", "Error loading orders: ${e.message}")
                    tvNoOrders.text = "Error loading orders"
                    tvNoOrders.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            Order(
                                orderId = doc.id,
                                userId = doc.getString("userId") ?: "",
                                paymentId = doc.getString("paymentId") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                status = doc.getString("status") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                items = listOf(),  // We don't need items for the list view
                                shippingAddress = doc.getString("shippingAddress") ?: "",
                                customerName = doc.getString("customerName") ?: "",
                                customerPhone = doc.getString("customerPhone") ?: "",
                                customerEmail = doc.getString("customerEmail") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("AdminOrdersActivity", "Error parsing order: ${e.message}")
                            null
                        }
                    }
                    ordersAdapter.submitList(orders)
                    tvNoOrders.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    tvNoOrders.visibility = View.VISIBLE
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 