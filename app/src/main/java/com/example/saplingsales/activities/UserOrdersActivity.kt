package com.example.saplingsales.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.adapters.OrdersAdapter
import com.example.saplingsales.models.Order
import com.example.saplingsales.models.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserOrdersActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnCart: ImageButton
    private lateinit var tvCartCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: View
    private lateinit var ordersAdapter: OrdersAdapter
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserOrdersActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_orders)
        
        initializeViews()
        setupViews()
        loadOrders()
        updateCartCount()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSearch = findViewById(R.id.btnSearch)
        btnCart = findViewById(R.id.btnCart)
        tvCartCount = findViewById(R.id.tvCartCount)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViews() {
        // Setup click listeners
        btnBack.setOnClickListener { finish() }
        btnSearch.setOnClickListener { /* TODO: Implement search */ }
        btnCart.setOnClickListener { 
            // TODO: Navigate to cart
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Setup RecyclerView
        ordersAdapter = OrdersAdapter(
            onOrderClick = { order ->
                // Navigate to order details
                val intent = Intent(this, UserOrderDetailActivity::class.java).apply {
                    putExtra("orderId", order.orderId)
                }
                startActivity(intent)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserOrdersActivity)
            adapter = ordersAdapter
        }
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("Please login to view orders")
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        
        // First try without ordering to ensure we can at least show some data
        firestore.collection("saplingOrders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                
                if (documents.isEmpty) {
                    showEmptyView()
                    return@addOnSuccessListener
                }

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
                        null
                    }
                }.sortedByDescending { it.createdAt }

                if (orders.isEmpty()) {
                    showEmptyView()
                } else {
                    showOrders(orders)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                showError("Error loading orders")
                showEmptyView()
            }
    }

    private fun updateCartCount() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            tvCartCount.visibility = View.GONE
            return
        }

        firestore.collection("cart")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                if (count > 0) {
                    tvCartCount.visibility = View.VISIBLE
                    tvCartCount.text = count.toString()
                } else {
                    tvCartCount.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                tvCartCount.visibility = View.GONE
            }
    }

    private fun showOrders(orders: List<Order>) {
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        ordersAdapter.submitList(orders)
    }

    private fun showEmptyView() {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
} 
