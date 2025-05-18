package com.example.saplingsales

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Base64
import android.widget.ImageView
import com.example.saplingsales.activities.AdminViewProductActivity
import android.util.Log
import java.util.*
import android.app.DatePickerDialog
import java.text.SimpleDateFormat

class AdminDashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var fabAddProduct: ExtendedFloatingActionButton
    private lateinit var menuButton: MaterialButton
    private lateinit var tvWelcomeAdmin: TextView
    private lateinit var tvNavHeaderName: TextView
    private lateinit var tvStoreName: TextView
    private lateinit var ivAdminProfile: ImageView
    private lateinit var navHeaderLayout: View
    private lateinit var tvTotalCollection: TextView
    private lateinit var tvSalesComparison: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnCompare: View
    private lateinit var tvCustomComparisonResult: TextView
    private lateinit var tvTotalProductSales: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        try {
            initializeViews()
            setupToolbar()
            setupNavigationDrawer()
            loadAdminDetails()
            setupDashboardStats()
            setupCustomComparison()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)
        fabAddProduct = findViewById(R.id.fabAddProduct)
        menuButton = findViewById(R.id.btnMenu)
        tvWelcomeAdmin = findViewById(R.id.tvWelcomeAdmin)
        tvTotalCollection = findViewById(R.id.tvTotalCollection)
        tvSalesComparison = findViewById(R.id.tvSalesComparison)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        btnCompare = findViewById(R.id.btnCompare)
        tvCustomComparisonResult = findViewById(R.id.tvCustomComparisonResult)
        tvTotalProductSales = findViewById(R.id.tvTotalProductSales)

        // Get nav header views
        val headerView = navigationView.getHeaderView(0)
        navHeaderLayout = headerView
        tvNavHeaderName = headerView.findViewById(R.id.tvAdminName)
        tvStoreName = headerView.findViewById(R.id.tvStoreName)
        ivAdminProfile = headerView.findViewById(R.id.ivAdminProfile)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this)

        // Setup menu button click with animation
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Setup FAB click listener
        fabAddProduct.setOnClickListener {
            try {
                Log.d("AdminDashboard", "Starting AdminAddproductActivity")
                val intent = Intent(this, AdminAddproductActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error launching AdminAddproductActivity", e)
                Toast.makeText(this, "Error launching Add Product: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // Setup nav header click
        navHeaderLayout.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AdminEditProfileActivity::class.java))
        }
    }

    private fun loadAdminDetails() {
        try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                db.collection("saplingAdmin")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val adminName = document.getString("adminname") ?: user.displayName ?: "Admin"
                            val storeName = document.getString("storename") ?: "Store"
                            updateAdminInfo(adminName, storeName)

                            // Load profile image if exists
                            val imageBase64 = document.getString("imageBase64")
                            if (!imageBase64.isNullOrEmpty()) {
                                try {
                                    val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    ivAdminProfile.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            updateAdminInfo(user.displayName ?: "Admin", "Store")
                            Toast.makeText(this, "Admin details not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error loading admin details: ${e.message}", Toast.LENGTH_SHORT).show()
                        updateAdminInfo("Admin", "Store")
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading admin details: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun updateAdminInfo(adminName: String, storeName: String) {
        try {
            tvWelcomeAdmin.text = "Welcome,   $adminName  \n    $storeName"
            tvNavHeaderName.text = adminName
            tvStoreName.text = storeName
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun setupDashboardStats() {
        db.collection("saplingOrders").get().addOnSuccessListener { snapshot ->
            val now = Calendar.getInstance()
            val thisMonth = now.get(Calendar.MONTH)
            val thisYear = now.get(Calendar.YEAR)

            // Calculate start/end of this month
            val startOfThisMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val startOfNextMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1)
            }.timeInMillis

            // Calculate start/end of last month
            val startOfLastMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -1)
            }.timeInMillis
            val endOfLastMonth = startOfThisMonth

            var sumThisMonth = 0.0
            var sumLastMonth = 0.0
            var total = 0.0
            val totalOrders = snapshot.documents.size

            snapshot.documents.forEach { doc ->
                try {
                    val amount = doc.getDouble("totalAmount") ?: 0.0
                    total += amount

                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                    if (createdAt != null) {
                        if (createdAt >= startOfThisMonth && createdAt < startOfNextMonth) {
                            sumThisMonth += amount
                        } else if (createdAt >= startOfLastMonth && createdAt < endOfLastMonth) {
                            sumLastMonth += amount
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminDashboard", "Error processing order: ${e.message}")
                }
            }

            tvTotalCollection.text = "Total Collection ₹%.2f".format(total)
            tvTotalProductSales.text = "Total Products Sold: $totalOrders"
            val percentChange = if (sumLastMonth > 0) ((sumThisMonth - sumLastMonth) / sumLastMonth) * 100 else 0.0
            val comparisonText = "This Month: ₹%.2f\nLast Month: ₹%.2f\nChange: %.1f%%".format(sumThisMonth, sumLastMonth, percentChange)
            tvSalesComparison.text = comparisonText
            tvSalesComparison.setTextColor(if (percentChange >= 0) getColor(R.color.admin_button_color) else getColor(android.R.color.holo_red_dark))
        }.addOnFailureListener { e ->
            Log.e("AdminDashboard", "Error loading orders: ${e.message}")
            Toast.makeText(this, "Error loading dashboard stats", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomComparison() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvStartDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                startDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                tvStartDate.text = dateFormat.format(startDate!!.time)
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        tvEndDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                endDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                tvEndDate.text = dateFormat.format(endDate!!.time)
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        btnCompare.setOnClickListener {
            if (startDate == null || endDate == null) {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (startDate!! > endDate!!) {
                Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            compareSalesBetweenDates(startDate!!.timeInMillis, endDate!!.timeInMillis)
        }
    }

    private fun compareSalesBetweenDates(startMillis: Long, endMillis: Long) {
        val periodLength = endMillis - startMillis + 1
        val prevEnd = startMillis - 1
        val prevStart = prevEnd - periodLength + 1

        db.collection("saplingOrders")
            .whereGreaterThanOrEqualTo("createdAt", startMillis)
            .whereLessThanOrEqualTo("createdAt", endMillis)
            .get()
            .addOnSuccessListener { currentSnapshot ->
                val currentTotal = currentSnapshot.documents.sumOf { it.getDouble("totalAmount") ?: 0.0 }
                db.collection("saplingOrders")
                    .whereGreaterThanOrEqualTo("createdAt", prevStart)
                    .whereLessThanOrEqualTo("createdAt", prevEnd)
                    .get()
                    .addOnSuccessListener { prevSnapshot ->
                        val prevTotal = prevSnapshot.documents.sumOf { it.getDouble("totalAmount") ?: 0.0 }
                        val percentChange = if (prevTotal > 0) ((currentTotal - prevTotal) / prevTotal) * 100 else 0.0
                        val isProfit = percentChange >= 0
                        tvCustomComparisonResult.text =
                            "Sales from ${formatDate(startMillis)} to ${formatDate(endMillis)}: ₹%.2f\n".format(currentTotal) +
                            "Previous  ₹%.2f\n".format(prevTotal) +
                            "Change    %.1f%%".format(percentChange)
                        tvCustomComparisonResult.setTextColor(
                            if (isProfit) getColor(R.color.forestGreen) else getColor(android.R.color.holo_red_dark)
                        )
                    }
                    .addOnFailureListener {
                        tvCustomComparisonResult.text = "Error fetching previous period sales data."
                        tvCustomComparisonResult.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
            }
            .addOnFailureListener {
                tvCustomComparisonResult.text = "Error fetching sales data."
                tvCustomComparisonResult.setTextColor(getColor(android.R.color.holo_red_dark))
            }
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_manage_users -> {
                    startActivity(Intent(this, ManageUserActivity::class.java))
                }
                R.id.nav_manage_products -> {
                    startActivity(Intent(this, AdminViewProductActivity::class.java))
                }
                R.id.nav_view_orders -> {
                    startActivity(Intent(this, AdminOrdersActivity::class.java))
                }
                R.id.nav_feedback -> {
                    startActivity(Intent(this, AdminFeedbackActivity::class.java))
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    val intent = Intent(this, AdminLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
