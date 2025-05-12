package com.example.saplingsales

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminSignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etAdminId: EditText
    private lateinit var etAdminName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var btnGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        etAdminId = findViewById(R.id.etAdminId)
        etAdminName = findViewById(R.id.etAdminName)
        etEmail = findViewById(R.id.etAdminEmail)
        etPassword = findViewById(R.id.etAdminPassword)
        etConfirmPassword = findViewById(R.id.etAdminConfirmPassword)
        btnSignup = findViewById(R.id.btnAdminSignup)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)

        btnSignup.setOnClickListener {
            val adminId = etAdminId.text.toString().trim()
            val adminName = etAdminName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (adminId.isEmpty() || adminName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if any admin exists
            db.collection("saplingAdmin").get().addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    Toast.makeText(this, "An admin already exists. Signup not allowed.", Toast.LENGTH_LONG).show()
                } else {
                    registerAdmin(adminId, adminName, email, password)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error checking admin existence: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
        }
    }

    private fun registerAdmin(adminId: String, adminName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val adminData = hashMapOf(
                            "adminId" to adminId,
                            "adminname" to adminName,
                            "email" to email
                            // ⚠️ Do NOT store passwords in Firestore for security reasons!
                        )

                        db.collection("saplingAdmin").document(userId)
                            .set(adminData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Admin Signup Successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, AdminLoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save admin: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
