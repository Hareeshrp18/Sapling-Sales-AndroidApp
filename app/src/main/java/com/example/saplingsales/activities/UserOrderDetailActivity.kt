package com.example.saplingsales.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.adapters.OrderItemsAdapter
import com.example.saplingsales.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UserOrderDetailActivity : AppCompatActivity() {
    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvShippingAddress: TextView
    private lateinit var rvOrderItems: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: View
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserOrderDetailActivity"
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_order_detail)

        // Initialize views
        initializeViews()
        setupRecyclerView()

        // Get order ID from intent
        val orderId = intent.getStringExtra("orderId")
        if (orderId == null) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load order details
        loadOrderDetails(orderId)

        // Setup back button
        btnBack.setOnClickListener { finish() }
    }

    private fun initializeViews() {
        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvStatus = findViewById(R.id.tvStatus)
        tvShippingAddress = findViewById(R.id.tvShippingAddress)
        rvOrderItems = findViewById(R.id.rvOrderItems)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@UserOrderDetailActivity)
            adapter = orderItemsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadOrderDetails(orderId: String) {
        progressBar.visibility = View.VISIBLE

        // Get current user ID
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login to view order details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("saplingOrders")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    try {
                        // Verify this order belongs to the current user
                        val orderUserId = document.getString("userId")
                        if (orderUserId != userId) {
                            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        val order = document.toObject(Order::class.java)
                        if (order != null) {
                            displayOrderDetails(order)
                        } else {
                            Toast.makeText(this, "Error parsing order data", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading order details: ${e.message}")
                        Toast.makeText(this, "Error loading order details: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading order: ${e.message}")
                Toast.makeText(this, "Error loading order: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayOrderDetails(order: Order) {
        // Set Order ID
        tvOrderId.text = order.orderId
        
        // Set Order Date
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = Date(order.createdAt)
        tvOrderDate.text = dateFormat.format(date)
        
        // Set Status with capitalization
        tvStatus.text = order.status.capitalize()
        
        // Set Total Amount with proper formatting
        tvTotalAmount.text = "₹${String.format("%.1f", order.totalAmount)}"
        
        // Set Shipping Address with proper formatting
        tvShippingAddress.text = order.shippingAddress.replace(", ", ",\n")

        // Update the RecyclerView with order items
        orderItemsAdapter.updateItems(order.items)
    }
} 