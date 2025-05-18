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
import android.Manifest
import android.telephony.SmsManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.MediaController
import android.widget.VideoView

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
                Log.d(TAG, "Found "+documents.size()+" cart items")
                if (documents.isEmpty) {
                    Log.d(TAG, "Cart is empty")
                    // Show the empty cart layout
                    setContentView(R.layout.layout_empty_cart)

                    // Setup the empty cart video
                    val videoView = findViewById<VideoView>(R.id.videoView)
                    val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.empty_cart}")
                    videoView.setVideoURI(videoUri)

//                    val mediaController = MediaController(this)
//                    mediaController.setAnchorView(videoView)
//                    videoView.setMediaController(mediaController)

                    videoView.setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true // optional: loop the video
                    }

                    videoView.start()

                    val btnContinueShopping = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinueShopping)
                    btnContinueShopping.setOnClickListener {
                        val intent = Intent(this, com.example.saplingsales.activities.UserScreenActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    // Handle back icon in toolbar
                    val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarEmptyCart)
                    toolbar.setNavigationOnClickListener {
                        onBackPressed()
                    }
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

                    // Create a batch operation for better handling of multiple products
                    val batch = firestore.batch()
                    
                    // First check all product availability
                    val productChecks = cartItems.map { cartItem ->
                        firestore.collection("saplingProducts").document(cartItem.productId).get()
                    }

                    // Wait for all product checks to complete
                    val allChecks = mutableListOf<Boolean>()
                    var checkCount = 0

                    productChecks.forEach { task ->
                        task.addOnSuccessListener { productDoc ->
                            val cartItem = cartItems[checkCount]
                            if (!productDoc.exists()) {
                                allChecks.add(false)
                            } else {
                                val currentQuantity = productDoc.getLong("quantity") ?: 0
                                if (currentQuantity < cartItem.quantity) {
                                    allChecks.add(false)
                                } else {
                                    // Update product quantity
                                    val productRef = firestore.collection("saplingProducts").document(cartItem.productId)
                                    batch.update(productRef, "quantity", currentQuantity - cartItem.quantity)
                                    allChecks.add(true)
                                }
                            }
                            checkCount++

                            // When all checks are complete
                            if (checkCount == cartItems.size) {
                                if (allChecks.all { it }) {
                                    // All products are available, proceed with order creation
                                    val orderRef = firestore.collection("saplingOrders").document(orderId)
                                    val orderData = hashMapOf(
                                        "userId" to userId,
                                        "orderId" to orderId,
                                        "paymentId" to paymentId,
                                        "totalAmount" to totalAmount,
                                        "status" to "confirmed",
                                        "createdAt" to System.currentTimeMillis(),
                                        "customerName" to userName,
                                        "customerEmail" to userEmail,
                                        "customerPhone" to userPhone,
                                        "shippingAddress" to userAddress,
                                        "items" to cartItems.map { item ->
                                            hashMapOf(
                                                "productId" to item.productId,
                                                "productName" to item.productName,
                                                "quantity" to item.quantity,
                                                "price" to item.price,
                                                "productImage" to item.productImage,
                                                "productImageUrl" to item.productImageUrl,
                                                "productCategory" to item.productCategory
                                            )
                                        }
                                    )

                                    batch.set(orderRef, orderData)

                                    // Delete cart items
                                    cartItems.forEach { cartItem ->
                                        val cartRef = firestore.collection("cartProduct").document(cartItem.id)
                                        batch.delete(cartRef)
                                    }

                                    // Commit the batch
                                    batch.commit()
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Order created successfully")
                                            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show()
                                            
                                            // Send SMS notification
                                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                                                == PackageManager.PERMISSION_GRANTED) {
                                                try {
                                                    val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                        this.getSystemService(SmsManager::class.java)
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        SmsManager.getDefault()
                                                    }

                                                    val formattedPhone = userPhone.trim().let {
                                                        if (it.startsWith("+91")) it else "+91$it"
                                                    }.replace(" ", "")

                                                    val productDetails = cartItems.joinToString("\n") { item ->
                                                        "${item.productName} (Qty: ${item.quantity}) - ₹${item.price * item.quantity}"
                                                    }

                                                    val message = """Dear $userName,
                                                        |
                                                        |Thank you for your order #$orderId!
                                                        |
                                                        |Order Details:
                                                        |$productDetails
                                                        |
                                                        |Total Amount: ₹$totalAmount
                                                        |
                                                        |Thank you for shopping with Sapling Sales!""".trimMargin()

                                                    // Split message if it's too long
                                                    val messageParts = smsManager.divideMessage(message)
                                                    smsManager.sendMultipartTextMessage(
                                                        formattedPhone,
                                                        null,
                                                        messageParts,
                                                        null,
                                                        null
                                                    )
                                                    Log.d(TAG, "SMS sent successfully to $formattedPhone")
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Failed to send SMS: ${e.message}")
                                                }
                                            } else {
                                                Log.e(TAG, "SMS permission not granted")
                                            }

                                            cartItems.clear()
                                            cartAdapter.submitList(emptyList())
                                            updateUI()
                                            progressBar.visibility = View.GONE
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Batch operation failed: ${e.message}")
                                            showError("Failed to process order: ${e.message}")
                                            progressBar.visibility = View.GONE
                                        }
                                } else {
                                    // Some products are not available
                                    showError("Some products are not available in the requested quantity")
                                    progressBar.visibility = View.GONE
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking product availability: ${e.message}")
                            showError("Error checking product availability")
                            progressBar.visibility = View.GONE
                        }
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