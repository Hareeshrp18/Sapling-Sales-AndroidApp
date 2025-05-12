package com.example.saplingsales

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.activities.UserOrderDetailActivity
import com.example.saplingsales.adapters.OrdersAdapter
import com.example.saplingsales.models.Order
import com.example.saplingsales.models.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserOrderActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var ordersAdapter: OrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_order)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Orders"

        // Initialize views
        recyclerView = findViewById(R.id.rvOrders)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        // Setup RecyclerView
        ordersAdapter = OrdersAdapter { order ->
            val intent = Intent(this, UserOrderDetailActivity::class.java).apply {
                putExtra("orderId", order.orderId)
            }
            startActivity(intent)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserOrderActivity)
            adapter = ordersAdapter
        }

        // Load orders
        loadOrders()
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to view orders", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("saplingOrders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val orders = documents.mapNotNull { document ->
                    try {
                        val items = (document.get("items") as? List<Map<String, Any>>)?.map { item ->
                            OrderItem(
                                productId = item["productId"] as? String ?: "",
                                productName = item["productName"] as? String ?: "",
                                quantity = (item["quantity"] as? Long)?.toInt() ?: 0,
                                price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                                productImage = item["productImage"] as? String ?: "",
                                productCategory = item["productCategory"] as? String ?: ""
                            )
                        } ?: emptyList()

                        Order(
                            orderId = document.getString("orderId") ?: document.id,
                            userId = document.getString("userId") ?: "",
                            paymentId = document.getString("paymentId") ?: "",
                            totalAmount = document.getDouble("totalAmount") ?: 0.0,
                            status = document.getString("status") ?: "",
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                            items = items,
                            shippingAddress = document.getString("shippingAddress") ?: "",
                            customerName = document.getString("customerName") ?: "",
                            customerPhone = document.getString("customerPhone") ?: "",
                            customerEmail = document.getString("customerEmail") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("UserOrderActivity", "Error parsing order: ${e.message}")
                        null
                    }
                }
                
                ordersAdapter.submitList(orders)
                tvEmptyState.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("UserOrderActivity", "Error loading orders: ${e.message}")
                Toast.makeText(this, "Error loading orders: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 