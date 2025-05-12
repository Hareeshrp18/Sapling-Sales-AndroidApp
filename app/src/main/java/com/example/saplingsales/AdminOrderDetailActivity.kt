package com.example.saplingsales

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.OrderItemsAdapter
import com.example.saplingsales.models.Order
import com.example.saplingsales.models.OrderItem
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminOrderDetailActivity : AppCompatActivity() {

    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerEmail: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvShippingAddress: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var rvOrderItems: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.widget.Toast.makeText(this, "Crash: ${throwable.message}", android.widget.Toast.LENGTH_LONG).show()
            android.util.Log.e("GlobalException", "Uncaught exception", throwable)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_order_detail)

        // Setup Toolbar as ActionBar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Order Details"
        }

        // Initialize views
        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvShippingAddress = findViewById(R.id.tvShippingAddress)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        rvOrderItems = findViewById(R.id.rvOrderItems)

        rvOrderItems.layoutManager = LinearLayoutManager(this)
        orderItemsAdapter = OrderItemsAdapter()
        rvOrderItems.adapter = orderItemsAdapter

        val orderId = intent.getStringExtra("orderId")
        if (orderId == null) {
            finish()
            return
        }

        loadOrderDetails(orderId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadOrderDetails(orderId: String) {
        db.collection("saplingOrders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    try {
                        val itemsList = mutableListOf<OrderItem>()
                        val itemsRaw = doc.get("items")
                        if (itemsRaw is List<*>) {
                            for (item in itemsRaw) {
                                if (item is Map<*, *>) {
                                    itemsList.add(
                                        OrderItem(
                                            productId = item["productId"] as? String ?: "",
                                            productName = item["productName"] as? String ?: "",
                                            quantity = when (val q = item["quantity"]) {
                                                is Long -> q.toInt()
                                                is Int -> q
                                                is Double -> q.toInt()
                                                else -> 0
                                            },
                                            price = when (val p = item["price"]) {
                                                is Double -> p
                                                is Long -> p.toDouble()
                                                is Int -> p.toDouble()
                                                else -> 0.0
                                            },
                                            productImage = item["productImage"] as? String ?: "",
                                            productImageUrl = item["productImageUrl"] as? String ?: ""
                                        )
                                    )
                                }
                            }
                        } else {
                            Log.e("AdminOrderDetail", "Order items field is not a List: $itemsRaw")
                            android.widget.Toast.makeText(this, "Order items missing or invalid", android.widget.Toast.LENGTH_LONG).show()
                        }
                        val order = Order(
                            orderId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            paymentId = doc.getString("paymentId") ?: "",
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            status = doc.getString("status") ?: "",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            items = itemsList,
                            shippingAddress = doc.getString("shippingAddress") ?: "",
                            customerName = doc.getString("customerName") ?: "",
                            customerPhone = doc.getString("customerPhone") ?: "",
                            customerEmail = doc.getString("customerEmail") ?: ""
                        )
                        displayOrderDetails(order)
                    } catch (e: Exception) {
                        Log.e("AdminOrderDetail", "Error parsing order: ", e)
                        android.widget.Toast.makeText(this, "Error parsing order details", android.widget.Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    android.widget.Toast.makeText(this, "Order not found", android.widget.Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminOrderDetail", "Error loading order: ${e.message}")
                android.widget.Toast.makeText(this, "Error loading order", android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun displayOrderDetails(order: Order) {
        tvOrderId.text = "Order #${order.orderId}"
        tvOrderStatus.text = "Status: ${order.status.capitalize()}"
        tvOrderDate.text = "Date: ${formatDate(order.createdAt)}"
        tvCustomerName.text = "Customer: ${order.customerName}"
        tvCustomerEmail.text = "Email: ${order.customerEmail}"
        tvCustomerPhone.text = "Phone: ${order.customerPhone}"
        tvShippingAddress.text = "Address: ${order.shippingAddress}"
        tvTotalAmount.text = "Total: ₹%.2f".format(order.totalAmount)
        orderItemsAdapter.updateItems(order.items)
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
    }
} 