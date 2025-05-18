package com.example.saplingsales

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.ui.api.upi.intent.CFUPIIntentCheckoutPayment
import com.google.firebase.firestore.FirebaseFirestore
import android.telephony.SmsManager
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import android.content.Intent
import android.net.Uri

class CashfreePaymentActivity : AppCompatActivity(), CFCheckoutResponseCallback {
    private val SMS_PERMISSION_REQUEST_CODE = 123
    private val firestore = FirebaseFirestore.getInstance()
    private var orderID: String = ""
    private var customerPhone: String = ""
    private var customerName: String = ""
    private var orderAmount: Double = 0.0
    private val isSmsPermissionGranted = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashfree_payment)

        // Extract order details from Intent
        orderID = intent.getStringExtra("orderId") ?: ""
        orderAmount = intent.getDoubleExtra("orderAmount", 0.0)
        val customerEmail = intent.getStringExtra("customerEmail") ?: ""
        customerPhone = intent.getStringExtra("customerPhone") ?: ""
        customerName = intent.getStringExtra("customerName") ?: ""

        if (orderID.isEmpty() || orderAmount <= 0.0) {
            Toast.makeText(this, "Invalid order details!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check SMS permission before starting payment
        checkSmsPermission()
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        } else {
            isSmsPermissionGranted.set(true)
            startPaymentProcess()
        }
    }

    private fun startPaymentProcess() {
        // For testing, we'll use a static session ID
        // In production, you should fetch this from your backend
        val sessionID = "session_" + System.currentTimeMillis()
        startPayment(sessionID, orderID, orderAmount, customerPhone, customerName)
    }

    private fun startPayment(
        sessionID: String,
        orderID: String,
        amount: Double,
        customerPhone: String,
        customerName: String
    ) {
        try {
            // Create session
            val session = CFSession.CFSessionBuilder()
                .setEnvironment(CFSession.Environment.SANDBOX)
                .setPaymentSessionID(sessionID)
                .setOrderId(orderID)
                .build()

            // Create payment object
            val checkoutPayment = CFUPIIntentCheckoutPayment.CFUPIIntentPaymentBuilder()
                .setSession(session)
                .build()

            // Set callback
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)

            // Start payment
            CFPaymentGatewayService.getInstance().doPayment(this, checkoutPayment)
        } catch (e: CFException) {
            Log.e("CashfreePayment", "Payment initialization failed: ${e.message}")
            Toast.makeText(this, "Payment initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onPaymentVerify(orderID: String) {
        Log.d("CashfreePayment", "Payment verified for Order ID: $orderID")
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show()
        
        // Update order status in Firestore and then send SMS
        updateOrderStatusAndSendSMS(orderID, true)
    }

    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderID: String) {
        Log.e("CashfreePayment", "Payment failed for Order ID: $orderID, Error: ${cfErrorResponse.message}")
        Toast.makeText(this, "Payment Failed: ${cfErrorResponse.message}", Toast.LENGTH_SHORT).show()
        
        // Update order status in Firestore and then send SMS
        updateOrderStatusAndSendSMS(orderID, false, cfErrorResponse.message)
    }

    private fun updateOrderStatusAndSendSMS(orderID: String, isSuccess: Boolean, errorMessage: String = "") {
        firestore.collection("saplingOrders").document(orderID)
            .update("status", if (isSuccess) "Completed" else "Failed")
            .addOnSuccessListener {
                Log.d("CashfreePayment", "Order status updated successfully")
                if (isSmsPermissionGranted.get()) {
                    sendPaymentStatusSMS(orderID, isSuccess, errorMessage)
                } else {
                    Log.e("CashfreePayment", "SMS permission not granted")
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("CashfreePayment", "Failed to update order status: ${e.message}")
                finish()
            }
    }

    private fun sendPaymentStatusSMS(orderID: String, isSuccess: Boolean, errorMessage: String = "") {
        if (!isSmsPermissionGranted.get()) {
            Log.e("CashfreePayment", "SMS permission not granted")
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Format phone number (remove spaces and ensure it starts with +91)
            val formattedPhone = customerPhone.trim().let {
                if (it.startsWith("+91")) it else "+91$it"
            }.replace(" ", "")

            Log.d("CashfreePayment", "Attempting to send SMS to: $formattedPhone")

            // Get order details from Firestore to include product information
            firestore.collection("saplingOrders").document(orderID)
                .get()
                .addOnSuccessListener { document ->
                    val items = document.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val productDetails = items.joinToString("\n") { item ->
                        val name = item["productName"] as? String ?: "Unknown Product"
                        val quantity = item["quantity"] as? Int ?: 0
                        val price = item["price"] as? Double ?: 0.0
                        "$name (Qty: $quantity) - ₹${price * quantity}"
                    }

            // Create the message
            val message = if (isSuccess) {
                        """Dear $customerName,
                            |
                            |Thank you for your order #$orderID!
                            |
                            |Order Details:
                            |$productDetails
                            |
                            |Total Amount: ₹$orderAmount
                            |
                            |Thank you for shopping with Sapling Sales!""".trimMargin()
            } else {
                "Dear $customerName, your payment of ₹$orderAmount for order #$orderID has failed. Reason: $errorMessage. Please try again."
            }

                    Log.d("CashfreePayment", "Prepared message: $message")

            // Try sending SMS using SmsManager first
            try {
                        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            this.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }

                        // Split message if it's too long
                        val messageParts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(
                    formattedPhone,
                    null,
                            messageParts,
                    null,
                    null
                )
                        
                Log.d("CashfreePayment", "SMS sent successfully using SmsManager to $formattedPhone")
                Toast.makeText(this, "Payment status SMS sent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CashfreePayment", "Failed to send SMS using SmsManager: ${e.message}")
                
                // Fallback: Try sending SMS using Intent
                try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$formattedPhone")
                                putExtra("sms_body", message)
                            }
                            if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    Log.d("CashfreePayment", "SMS intent launched for $formattedPhone")
                            } else {
                                Log.e("CashfreePayment", "No SMS app found")
                                Toast.makeText(this, "No SMS app found", Toast.LENGTH_SHORT).show()
                            }
                } catch (e2: Exception) {
                    Log.e("CashfreePayment", "Failed to send SMS using Intent: ${e2.message}")
                    Toast.makeText(this, "Failed to send payment status SMS", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CashfreePayment", "Failed to fetch order details: ${e.message}")
                    // Send basic message if order details can't be fetched
                    val basicMessage = if (isSuccess) {
                        "Dear $customerName, your payment of ₹$orderAmount for order #$orderID has been successful. Thank you for shopping with Sapling Sales!"
                    } else {
                        "Dear $customerName, your payment of ₹$orderAmount for order #$orderID has failed. Reason: $errorMessage. Please try again."
                    }
                    
                    try {
                        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            this.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }

                        // Split message if it's too long
                        val messageParts = smsManager.divideMessage(basicMessage)
                        smsManager.sendMultipartTextMessage(
                            formattedPhone,
                            null,
                            messageParts,
                            null,
                            null
                        )
                        Log.d("CashfreePayment", "Basic SMS sent successfully")
                    } catch (e: Exception) {
                        Log.e("CashfreePayment", "Failed to send basic SMS: ${e.message}")
                        Toast.makeText(this, "Failed to send SMS notification", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("CashfreePayment", "Error in sendPaymentStatusSMS: ${e.message}")
            Toast.makeText(this, "Error sending payment status SMS", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isSmsPermissionGranted.set(true)
                Log.d("CashfreePayment", "SMS permission granted")
                startPaymentProcess()
            } else {
                Log.e("CashfreePayment", "SMS permission denied")
                Toast.makeText(this, "SMS permission is required for payment notifications", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
} 