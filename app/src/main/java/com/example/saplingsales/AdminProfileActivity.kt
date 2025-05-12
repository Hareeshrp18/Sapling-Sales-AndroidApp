package com.example.saplingsales

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminEmail: TextView
    private lateinit var btnLogout: Button
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile)

        // Initialize Views
        tvAdminName = findViewById(R.id.tvAdminName)
        tvAdminEmail = findViewById(R.id.tvAdminEmail)
        btnLogout = findViewById(R.id.btnLogout)

        // Fetch Admin Details
        fetchAdminDetails()

        // Logout Button Click Listener
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
        }
    }

    private fun fetchAdminDetails() {
        val adminId = auth.currentUser?.uid
        if (adminId != null) {
            db.collection("saplingAdmin").document(adminId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val adminName = document.getString("adminname")
                        val adminEmail = document.getString("email")

                        tvAdminName.text = "Admin Name: $adminName"
                        tvAdminEmail.text = "Email: $adminEmail"
                    } else {
                        Toast.makeText(this, "Admin data not found!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
