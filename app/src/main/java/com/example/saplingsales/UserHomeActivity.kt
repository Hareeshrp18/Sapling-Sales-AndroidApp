package com.example.saplingsales

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.saplingsales.activities.CartActivity
import com.example.saplingsales.activities.UserOrdersActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class UserHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var btnBack: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var tvWelcome: TextView
    private lateinit var tvUserId: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvUserId = findViewById(R.id.tvUserId)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            tvWelcome.text = "Welcome back, ${user.displayName ?: "User"}"
            loadUserProfile(user.uid)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        setupClickListeners()
        setupBottomNavigation()
        setupNavigationDrawer()
    }

    private fun setupClickListeners() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_orders -> {
                    startActivity(Intent(this, UserOrdersActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserProfile(userId: String) {
        val userRef = firestore.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Log.d("UserHomeActivity", "User data found: ${document.data}")
                tvUserId.text = "User ID: $userId"
            } else {
                Log.e("UserHomeActivity", "User document does not exist")
                Toast.makeText(this, "Error: User data not found", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Log.e("UserHomeActivity", "Error retrieving user data: ${e.message}")
            Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, UserLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
