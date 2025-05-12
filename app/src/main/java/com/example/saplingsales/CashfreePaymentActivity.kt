package com.example.saplingsales

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.ui.api.upi.intent.CFUPIIntentCheckoutPayment
import org.json.JSONObject

class CashfreePaymentActivity : AppCompatActivity(), CFCheckoutResponseCallback {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashfree_payment)

        // Extract order details from Intent
        val orderID = intent.getStringExtra("orderId") ?: ""
        val orderAmount = intent.getDoubleExtra("orderAmount", 0.0)
        val customerEmail = intent.getStringExtra("customerEmail") ?: ""
        val customerPhone = intent.getStringExtra("customerPhone") ?: ""
        val customerName = intent.getStringExtra("customerName") ?: ""

        if (orderID.isEmpty() || orderAmount <= 0.0) {
            Toast.makeText(this, "Invalid order details!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // For testing, we'll use a static session ID
        // In production, you should fetch this from your backend
        val sessionID = "session_" + System.currentTimeMillis()

        startPayment(sessionID, orderID, orderAmount, customerEmail, customerPhone, customerName)
    }

    private fun startPayment(
        sessionID: String,
        orderID: String,
        amount: Double,
        customerEmail: String,
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
        
        // Update order status in Firestore
        updateOrderStatus(orderID, "Completed")
        finish()
    }

    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderID: String) {
        Log.e("CashfreePayment", "Payment failed for Order ID: $orderID, Error: ${cfErrorResponse.message}")
        Toast.makeText(this, "Payment Failed: ${cfErrorResponse.message}", Toast.LENGTH_SHORT).show()
        
        // Update order status in Firestore
        updateOrderStatus(orderID, "Failed")
        finish()
    }

    private fun updateOrderStatus(orderID: String, status: String) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("saplingOrders").document(orderID)
            .update("status", status)
            .addOnSuccessListener {
                Log.d("CashfreePayment", "Order status updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("CashfreePayment", "Failed to update order status: ${e.message}")
            }
    }
} 