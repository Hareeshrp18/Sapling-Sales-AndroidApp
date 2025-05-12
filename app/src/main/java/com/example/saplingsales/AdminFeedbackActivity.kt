package com.example.saplingsales

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.AdminFeedbackUserAdapter
import com.google.firebase.firestore.FirebaseFirestore

// Data class for user feedback info
data class FeedbackUser(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String,
    val latestFeedback: String
)

class AdminFeedbackActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoUsers: TextView
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminFeedbackUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_feedback)

        recyclerView = findViewById(R.id.rvUsers)
        progressBar = findViewById(R.id.progressBar)
        tvNoUsers = findViewById(R.id.tvNoUsers)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminFeedbackUserAdapter { user ->
            val intent = Intent(this, AdminFeedbackChatActivity::class.java)
            intent.putExtra("userId", user.userId)
            intent.putExtra("userName", user.userName)
            intent.putExtra("userEmail", user.userEmail)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadUsersWithFeedback()
    }

    private fun loadUsersWithFeedback() {
        progressBar.visibility = View.VISIBLE
        tvNoUsers.visibility = View.GONE
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                val users = snapshot.documents.mapNotNull { doc ->
                    val feedbackArray = doc.get("feedback") as? List<Map<String, Any>>
                    if (feedbackArray != null && feedbackArray.isNotEmpty()) {
                        val userId = doc.getString("userId") ?: doc.id
                        val userName = doc.getString("name") ?: "User"
                        val userEmail = doc.getString("email") ?: ""
                        val userPhone = doc.getString("phone") ?: ""
                        val latestFeedback = feedbackArray.lastOrNull()?.get("message") as? String ?: ""
                        FeedbackUser(userId, userName, userEmail, userPhone, latestFeedback)
                    } else {
                        null
                    }
                }
                adapter.submitList(users)
                tvNoUsers.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvNoUsers.visibility = View.VISIBLE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 