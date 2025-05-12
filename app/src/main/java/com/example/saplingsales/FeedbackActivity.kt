package com.example.saplingsales

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.FeedbackAdapter
import com.example.saplingsales.models.Feedback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.ArrayList

class FeedbackActivity : AppCompatActivity() {
    private lateinit var adapter: FeedbackAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var noFeedbackText: TextView
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        // Initialize views first
        initializeViews()

        // Check if user is logged in
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in to access feedback", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupFeedbackSubmission()
        loadFeedback()
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.rvFeedback)
            editText = findViewById(R.id.etFeedback)
            sendButton = findViewById(R.id.btnSend)
            noFeedbackText = findViewById(R.id.tvNoFeedback)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { onBackPressed() }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up toolbar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        try {
            adapter = FeedbackAdapter()
            recyclerView.layoutManager = LinearLayoutManager(this).apply {
                stackFromEnd = true
                reverseLayout = false
            }
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up feedback list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFeedbackSubmission() {
        sendButton.setOnClickListener {
            try {
                val message = editText.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendFeedback(message)
                } else {
                    Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error sending feedback", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendFeedback(message: String) {
        val currentUser = auth.currentUser ?: return

        sendButton.isEnabled = false

        try {
            // Create feedback data
            val feedbackData = mapOf(
                "userId" to currentUser.uid,
                "message" to message,
                "timestamp" to Timestamp.now(),
                "type" to "feedback",
                "email" to (currentUser.email ?: ""),
                "username" to (currentUser.displayName ?: "")
            )

            // Get reference to user document
            val userDocRef = db.collection("users").document(currentUser.uid)

            // First check if user document exists
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        // Create user document if it doesn't exist
                        val userData = mapOf(
                            "userId" to currentUser.uid,
                            "email" to (currentUser.email ?: ""),
                            "feedback" to ArrayList<Map<String, Any>>()
                        )
                        userDocRef.set(userData)
                    }

                    // Get existing feedback or create new list
                    val existingFeedback = try {
                        (document.get("feedback") as? ArrayList<Map<String, Any>>) ?: ArrayList()
                    } catch (e: Exception) {
                        ArrayList<Map<String, Any>>()
                    }

                    // Add new feedback
                    existingFeedback.add(feedbackData)

                    // Update the document
                    userDocRef.update("feedback", existingFeedback)
                        .addOnSuccessListener {
                            editText.text.clear()
                            Toast.makeText(this, "Feedback sent successfully", Toast.LENGTH_SHORT).show()
                            loadFeedback()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error sending feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener {
                            sendButton.isEnabled = true
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error accessing user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    sendButton.isEnabled = true
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing feedback", Toast.LENGTH_SHORT).show()
            sendButton.isEnabled = true
        }
    }

    private fun loadFeedback() {
        val currentUser = auth.currentUser ?: return

        try {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        showEmptyState()
                        return@addOnSuccessListener
                    }

                    val feedbackList = ArrayList<Feedback>()
                    
                    try {
                        val existingFeedback = document.get("feedback") as? ArrayList<Map<String, Any>> ?: ArrayList()
                        
                        for ((index, feedbackMap) in existingFeedback.withIndex()) {
                            try {
                                val feedback = Feedback(
                                    id = index.toString(),
                                    userId = feedbackMap["userId"] as? String ?: "",
                                    message = feedbackMap["message"] as? String ?: "",
                                    timestamp = feedbackMap["timestamp"] as? Timestamp ?: Timestamp.now()
                                )
                                feedbackList.add(feedback)
                            } catch (e: Exception) {
                                continue // Skip invalid feedback entries
                            }
                        }

                        if (feedbackList.isEmpty()) {
                            showEmptyState()
                        } else {
                            hideEmptyState()
                            adapter.submitList(feedbackList)
                            recyclerView.scrollToPosition(feedbackList.size - 1)
                        }
                    } catch (e: Exception) {
                        showEmptyState()
                        return@addOnSuccessListener
                    }
                }
                .addOnFailureListener {
                    showEmptyState()
                }
        } catch (e: Exception) {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        noFeedbackText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        noFeedbackText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
} 