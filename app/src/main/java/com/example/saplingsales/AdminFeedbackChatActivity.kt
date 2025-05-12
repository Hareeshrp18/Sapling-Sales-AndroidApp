package com.example.saplingsales

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.AdminFeedbackChatAdapter
import com.example.saplingsales.models.Feedback
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast

class AdminFeedbackChatActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminFeedbackChatAdapter
    private lateinit var etReply: EditText
    private lateinit var btnSendReply: ImageButton
    private lateinit var toolbar: Toolbar
    private val db = FirebaseFirestore.getInstance()
    private var userId: String = ""
    private var userName: String = ""
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_feedback_chat)

        userId = intent.getStringExtra("userId") ?: ""
        userName = intent.getStringExtra("userName") ?: "User"
        userEmail = intent.getStringExtra("userEmail") ?: ""

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = userName
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recyclerView = findViewById(R.id.rvMessages)
        etReply = findViewById(R.id.etReply)
        btnSendReply = findViewById(R.id.btnSendReply)

        adapter = AdminFeedbackChatAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter

        btnSendReply.setOnClickListener { sendReply() }

        loadMessages()
    }

    private fun loadMessages() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val feedbackList = mutableListOf<Feedback>()
                val feedbackArray = doc.get("feedback") as? List<Map<String, Any>> ?: emptyList()
                for ((index, map) in feedbackArray.withIndex()) {
                    val type = map["type"] as? String ?: "feedback"
                    val message = map["message"] as? String ?: ""
                    val timestamp = map["timestamp"] as? Timestamp ?: Timestamp.now()
                    val sender = if (type == "admin_reply") "admin" else "user"
                    feedbackList.add(
                        Feedback(
                            id = index.toString(),
                            userId = userId,
                            message = message,
                            timestamp = timestamp,
                            type = type,
                            sender = sender
                        )
                    )
                }
                adapter.submitList(feedbackList)
                recyclerView.scrollToPosition(feedbackList.size - 1)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendReply() {
        val reply = etReply.text.toString().trim()
        if (reply.isEmpty()) return
        btnSendReply.isEnabled = false
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val feedbackArray = (doc.get("feedback") as? ArrayList<Map<String, Any>>) ?: ArrayList()
                val replyData = mapOf(
                    "userId" to userId,
                    "message" to reply,
                    "timestamp" to Timestamp.now(),
                    "type" to "admin_reply"
                )
                feedbackArray.add(replyData)
                db.collection("users").document(userId)
                    .update("feedback", feedbackArray)
                    .addOnSuccessListener {
                        etReply.text.clear()
                        loadMessages()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error sending reply", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener { btnSendReply.isEnabled = true }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error sending reply", Toast.LENGTH_SHORT).show()
                btnSendReply.isEnabled = true
            }
    }
} 