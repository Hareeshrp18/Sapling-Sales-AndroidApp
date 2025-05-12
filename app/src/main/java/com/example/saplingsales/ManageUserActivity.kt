package com.example.saplingsales

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.UserAdapter
import com.example.saplingsales.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageUserActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var allUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Manage Users"

        // Initialize views
        recyclerView = findViewById(R.id.rvUsers)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter()
        recyclerView.adapter = userAdapter

        // Setup search button click listener
        btnSearch.setOnClickListener {
            val searchQuery = etSearch.text.toString().trim()
            if (searchQuery.isNotEmpty()) {
                searchUsers(searchQuery)
            } else {
                Toast.makeText(this, "Please enter search text", Toast.LENGTH_SHORT).show()
            }
        }

        checkAdminAndLoadUsers()
    }

    private fun checkAdminAndLoadUsers() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if user is admin
        db.collection("saplingAdmin").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    loadUsers()
                } else {
                    Toast.makeText(this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                allUsers.clear()
                for (document in documents) {
                    val user = User(
                        id = document.id,
                        name = document.getString("username") ?: "",
                        email = document.getString("email") ?: "",
                        phone = document.getString("phone") ?: "",
                        status = document.getString("status") ?: "active"
                    )
                    allUsers.add(user)
                }
                if (allUsers.isEmpty()) {
                    Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show()
                }
                userAdapter.submitList(allUsers)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchUsers(query: String) {
        val searchResults = allUsers.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true) ||
            user.phone.contains(query, ignoreCase = true)
        }

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No matching users found", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Found ${searchResults.size} matching users", Toast.LENGTH_SHORT).show()
        }

        userAdapter.submitList(searchResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 