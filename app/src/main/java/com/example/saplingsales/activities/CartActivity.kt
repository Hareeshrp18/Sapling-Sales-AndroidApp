package com.example.saplingsales.activities

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
import com.example.saplingsales.adapters.CartAdapter
import com.example.saplingsales.models.CartItem
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.Button

class CartActivity : AppCompatActivity(), PaymentResultListener {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var totalAmountText: TextView
    private lateinit var checkoutButton: MaterialButton
    private lateinit var progressBar: View
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "CartActivity"
    private var totalAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        
        // Initialize Razorpay Checkout
        Checkout.preload(applicationContext)
        
        initializeViews()
        setupViews()
        loadCartItems()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        totalAmountText = findViewById(R.id.totalAmount)
        checkoutButton = findViewById(R.id.checkoutButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViews() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cart"

        cartAdapter = CartAdapter(
            onQuantityChanged = { cartItem, newQuantity ->
                updateCartItemQuantity(cartItem, newQuantity)
            },
            onRemoveItem = { cartItem ->
                removeCartItem(cartItem)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }

        checkoutButton.setOnClickListener {
            if (cartItems.isEmpty()) {
                showError("Your cart is empty")
                return@setOnClickListener
            }
            startPayment()
        }
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("Please log in to view cart")
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        cartItems.clear()

        firestore.collection("cartProduct")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} cart items")
                if (documents.isEmpty) {
                    Log.d(TAG, "Cart is empty")
                    updateUI()
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                var loadedItems = 0
                val totalItems = documents.size()

                documents.forEach { document ->
                    try {
                        Log.d(TAG, "Processing document: ${document.id}")
                        Log.d(TAG, "Document data: ${document.data}")
                        
                        val cartItem = CartItem(
                            id = document.id,
                            userId = document.getString("userId") ?: "",
                            productId = document.getString("productId") ?: "",
                            quantity = document.getLong("quantity")?.toInt() ?: 1,
                            price = document.getLong("price")?.toDouble() ?: 0.0,
                            productName = document.getString("productName") ?: "",
                            productImage = document.getString("productImage") ?: "",
                            productImageUrl = document.getString("productImageUrl") ?: "",
                            productCategory = document.getString("productCategory") ?: "Other",
                            addedAt = document.getLong("addedAt") ?: System.currentTimeMillis(),
                            status = document.getString("status") ?: "pending"
                        )
                        
                        Log.d(TAG, "Created CartItem: $cartItem")

                        // Get available quantity from products collection
                        firestore.collection("saplingProducts")
                            .document(cartItem.productId)
                            .get()
                            .addOnSuccessListener { productDoc ->
                                if (productDoc.exists()) {
                                    Log.d(TAG, "Found product document: ${productDoc.id}")
                                    val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0
                                    val updatedCartItem = cartItem.copy(availableQuantity = availableQuantity)
                                    
                                    val index = cartItems.indexOfFirst { it.id == cartItem.id }
                                    if (index != -1) {
                                        cartItems[index] = updatedCartItem
                                    } else {
                                        cartItems.add(updatedCartItem)
                                    }
                                } else {
                                    Log.d(TAG, "Product not found, using default quantity")
                                    cartItems.add(cartItem)
                                }

                                loadedItems++
                                Log.d(TAG, "Loaded $loadedItems of $totalItems items")
                                if (loadedItems == totalItems) {
                                    // Sort items by addedAt timestamp
                                    val sortedItems = cartItems.sortedByDescending { it.addedAt }
                                    cartItems.clear()
                                    cartItems.addAll(sortedItems)
                                    
                                    // All items loaded, update UI
                                    Log.d(TAG, "All items loaded, updating UI with ${cartItems.size} items")
                                    cartAdapter.submitList(cartItems.toList())
                                    updateUI()
                                    progressBar.visibility = View.GONE
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error loading product details: ${e.message}")
                                // Still add the cart item even if we can't get the product details
                                cartItems.add(cartItem)
                                
                                loadedItems++
                                if (loadedItems == totalItems) {
                                    cartAdapter.submitList(cartItems.toList())
                                    updateUI()
                                    progressBar.visibility = View.GONE
                                }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to CartItem: ${e.message}")
                        Log.e(TAG, "Document data: ${document.data}")
                        e.printStackTrace()
                        loadedItems++
                        if (loadedItems == totalItems) {
                            cartAdapter.submitList(cartItems.toList())
                            updateUI()
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading cart items: ${e.message}")
                e.printStackTrace()
                progressBar.visibility = View.GONE
                showError("Error loading cart items")
                updateUI()
            }
    }

    private fun updateCartItemQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity < 1) {
            showError("Quantity cannot be less than 1")
            return
        }

        // Check available quantity from products collection first
        firestore.collection("saplingProducts")
            .document(cartItem.productId)
            .get()
            .addOnSuccessListener { productDoc ->
                if (!productDoc.exists()) {
                    showError("Product not found")
                    return@addOnSuccessListener
                }
                
                val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0
                
                if (newQuantity > availableQuantity) {
                    showError("Cannot exceed available quantity ($availableQuantity)")
                    return@addOnSuccessListener
                }

                progressBar.visibility = View.VISIBLE
                
                firestore.collection("cartProduct")
                    .document(cartItem.id)
                    .update("quantity", newQuantity)
                    .addOnSuccessListener {
                        val index = cartItems.indexOfFirst { it.id == cartItem.id }
                        if (index != -1) {
                            val updatedCartItem = cartItems[index].copy(
                                quantity = newQuantity,
                                availableQuantity = availableQuantity
                            )
                            cartItems[index] = updatedCartItem
                            cartAdapter.submitList(cartItems.toList())
                            updateTotalAmount()
                        }
                        progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating quantity: ${e.message}")
                        progressBar.visibility = View.GONE
                        showError("Error updating quantity")
                        cartAdapter.submitList(cartItems.toList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking available quantity: ${e.message}")
                showError("Error checking available quantity")
            }
    }

    private fun removeCartItem(cartItem: CartItem) {
        progressBar.visibility = View.VISIBLE
        
        firestore.collection("cartProduct")
            .document(cartItem.id)
            .delete()
            .addOnSuccessListener {
                cartItems.removeAll { it.id == cartItem.id }
                updateUI()
                Toast.makeText(this, "Item removed from cart", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing item: ${e.message}")
                progressBar.visibility = View.GONE
                showError("Error removing item")
            }
    }

    private fun startPayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_dsJ0S4SOsOEhJD")
        
        try {
            val options = JSONObject().apply {
                put("name", "Sapling Sales")
                put("description", "Order Payment")
                put("currency", "INR")
                put("amount", (totalAmount * 100).toInt()) // Converting to paise
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email ?: "")
                    put("contact", "+91 86680 49150")
                })
                
                // Enable specific payment methods
                put("method", JSONObject().apply {
                    put("netbanking", true)
                    put("card", true)
                    put("upi", true)
                    put("wallet", true)
                })

                // Configure preferred payment methods
                val prefill = JSONObject().apply {
                    put("method", "upi")
                    put("vpa", "") // Leave empty for user input
                }

                // Set up payment preferences
                put("preferences", JSONObject().apply {
                    put("show_default_payment_methods", true)
                    put("show_saved_instruments", true)
                    put("saved_instruments_first", true)
                })

                // Theme configuration
                put("theme", JSONObject().apply {
                    put("color", "#4CAF50")
                    put("backdrop_color", "#ffffff")
                    put("hide_topbar", false)
                })

                put("modal", JSONObject().apply {
                    put("confirm_close", true)
                    put("animation", true)
                })

                put("send_sms_hash", true)
                put("allow_rotation", true)
                put("remember_customer", true)
            }

            checkout.open(this@CartActivity, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting payment: ${e.message}")
            e.printStackTrace()
            showError("Error initiating payment: ${e.message}")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String) {
        try {
            Log.d(TAG, "Payment successful: $razorpayPaymentId")
            showSuccessDialog {
                createOrder(razorpayPaymentId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPaymentSuccess: ${e.message}")
            e.printStackTrace()
            showError("Error processing successful payment")
        }
    }

    override fun onPaymentError(code: Int, description: String) {
        try {
            Log.e(TAG, "Payment failed: Code: $code, Description: $description")
            val message = when (code) {
                Checkout.PAYMENT_CANCELED -> "Payment cancelled by user"
                Checkout.NETWORK_ERROR -> "Network error occurred"
                Checkout.INVALID_OPTIONS -> "Invalid payment options"
                else -> "Payment failed: $description"
            }
            showFailureDialog(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPaymentError: ${e.message}")
            e.printStackTrace()
            showError("Error handling payment failure")
        }
    }

    private fun showSuccessDialog(onSuccess: () -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.payment_success_dialog)
        dialog.setCancelable(false)

        // Set dialog width to 90% of screen width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onSuccess.invoke()
        }

        dialog.show()
    }

    private fun showFailureDialog(errorMessage: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.payment_failure_dialog)
        dialog.setCancelable(false)

        // Set dialog width to 90% of screen width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<TextView>(R.id.tvErrorMessage).text = errorMessage
        dialog.findViewById<Button>(R.id.btnTryAgain).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createOrder(paymentId: String) {
        progressBar.visibility = View.VISIBLE
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("User not authenticated")
            progressBar.visibility = View.GONE
            return
        }

        try {
            val orderId = UUID.randomUUID().toString()
            
            // First fetch user information
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { userDoc ->
                    if (!userDoc.exists()) {
                        showError("User information not found")
                        progressBar.visibility = View.GONE
                        return@addOnSuccessListener
                    }

                    val userName = userDoc.getString("name") ?: ""
                    val userEmail = userDoc.getString("email") ?: ""
                    val userPhone = userDoc.getString("phone") ?: ""
                    val userAddress = userDoc.getString("address") ?: ""
                    
                    // Now proceed with order creation transaction
                    firestore.runTransaction { transaction ->
                        var canProceed = true
                        val productUpdates = mutableMapOf<String, Int>()

                        // First check product availability
                        cartItems.forEach { cartItem ->
                            val productRef = firestore.collection("saplingProducts").document(cartItem.productId)
                            val productDoc = transaction.get(productRef)
                            
                            if (!productDoc.exists()) {
                                canProceed = false
                                return@forEach
                            }
                            
                            val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0
                            if (availableQuantity < cartItem.quantity) {
                                canProceed = false
                                return@forEach
                            }
                            
                            productUpdates[cartItem.productId] = availableQuantity - cartItem.quantity
                        }

                        if (canProceed) {
                            // Create order document with user information
                            val orderRef = firestore.collection("saplingOrders").document(orderId)
                            val orderData = hashMapOf(
                                "userId" to userId,
                                "orderId" to orderId,
                                "paymentId" to paymentId,
                                "totalAmount" to totalAmount,
                                "status" to "confirmed",
                                "createdAt" to System.currentTimeMillis(),
                                // Include user information
                                "customerName" to userName,
                                "customerEmail" to userEmail,
                                "customerPhone" to userPhone,
                                "shippingAddress" to userAddress,
                                "items" to cartItems.map { cartItem ->
                                    // Get product details to ensure we have the latest image data
                                    val productRef = firestore.collection("saplingProducts").document(cartItem.productId)
                                    val productDoc = transaction.get(productRef)
                                    
                                    hashMapOf(
                                        "productId" to cartItem.productId,
                                        "quantity" to cartItem.quantity,
                                        "price" to cartItem.price,
                                        "productName" to cartItem.productName,
                                        "productImage" to (productDoc.getString("imageBase64") ?: cartItem.productImage),
                                        "productImageUrl" to (productDoc.get("imageUrls")?.let { urls ->
                                            when (urls) {
                                                is List<*> -> urls.firstOrNull()?.toString()
                                                else -> null
                                            }
                                        } ?: cartItem.productImageUrl),
                                        "productCategory" to cartItem.productCategory
                                    )
                                }
                            )
                            transaction.set(orderRef, orderData)

                            // Update product quantities
                            productUpdates.forEach { (productId, newQuantity) ->
                                val productRef = firestore.collection("saplingProducts").document(productId)
                                transaction.update(productRef, "quantity", newQuantity)
                            }

                            // Delete cart items
                            cartItems.forEach { cartItem ->
                                val cartRef = firestore.collection("cartProduct").document(cartItem.id)
                                transaction.delete(cartRef)
                            }
                        }
                        
                        canProceed
                    }.addOnSuccessListener {
                        Log.d(TAG, "Order created successfully")
                        Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show()
                        cartItems.clear()
                        cartAdapter.submitList(emptyList())
                        updateUI()
                        progressBar.visibility = View.GONE
                        finish()
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Transaction failed: ${e.message}")
                        showError("Failed to process order: ${e.message}")
                        progressBar.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user data: ${e.message}")
                    showError("Error fetching user information")
                    progressBar.visibility = View.GONE
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createOrder: ${e.message}")
            e.printStackTrace()
            showError("Error creating order: ${e.message}")
            progressBar.visibility = View.GONE
        }
    }

    private fun updateUI() {
        if (cartItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            checkoutButton.isEnabled = false
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            checkoutButton.isEnabled = true
            updateTotalAmount()
        }
    }

    private fun updateTotalAmount() {
        totalAmount = cartItems.sumOf { it.price * it.quantity }
        val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            .format(totalAmount)
        totalAmountText.text = "Total: $formattedAmount"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 