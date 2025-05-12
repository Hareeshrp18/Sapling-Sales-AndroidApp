package com.example.saplingsales

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.saplingsales.activities.CartActivity
import com.example.saplingsales.activities.UserScreenActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UserProfileActivity : AppCompatActivity() {
    private var toolbar: Toolbar? = null
    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvPhone: TextView? = null
    private var tvAddress: TextView? = null
    private var btnEdit: MaterialButton? = null
    private var btnLogout: MaterialButton? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var profileImageView: ImageView? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    
    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
        setContentView(R.layout.activity_user_profile)

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            
            initializeViews()
            setupToolbar()
            setupBottomNavigation()
            loadUserProfile()
            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing profile: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            tvName = findViewById(R.id.tvName)
            tvEmail = findViewById(R.id.tvEmail)
            tvPhone = findViewById(R.id.tvPhone)
            tvAddress = findViewById(R.id.tvAddress)
            btnEdit = findViewById(R.id.btnEdit)
            btnLogout = findViewById(R.id.btnLogout)
            bottomNavigationView = findViewById(R.id.bottomNavigationView)
            profileImageView = findViewById(R.id.profileImageView)

            // Verify all views are initialized
            if (toolbar == null || tvName == null || tvEmail == null || 
                tvPhone == null || tvAddress == null || btnEdit == null || 
                btnLogout == null || bottomNavigationView == null ||
                profileImageView == null) {
                throw IllegalStateException("Failed to initialize one or more views")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }

    private fun setupToolbar() {
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView?.let { nav ->
                nav.selectedItemId = R.id.nav_profile
                nav.setOnItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_home -> {
                            startActivity(Intent(this, UserScreenActivity::class.java))
                            finish()
                            true
                        }
                        R.id.nav_cart -> {
                            startActivity(Intent(this, CartActivity::class.java))
                            finish()
                            true
                        }
                        R.id.nav_profile -> true
                        else -> false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, redirecting to login")
            startActivity(Intent(this, UserLoginActivity::class.java))
            finish()
            return
        }

        Log.d(TAG, "Loading profile for user: ${currentUser.uid}")
        db.collection("users").document(currentUser.uid)
            .get()
                .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d(TAG, "User data found: ${document.data}")
                    tvName?.text = document.getString("name") ?: "No name"
                    tvEmail?.text = document.getString("email") ?: currentUser.email
                    tvPhone?.text = document.getString("phone") ?: "No phone"
                    tvAddress?.text = document.getString("address") ?: "No address"
                    
                    // Load profile image from Base64
                    document.getString("ImageBase64")?.let { base64String ->
                        Log.d(TAG, "Loading profile image from Base64")
                        try {
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.let { bitmap ->
                                // Create a circular bitmap
                                val circularBitmap = createCircularBitmap(bitmap)
                                profileImageView?.setImageBitmap(circularBitmap)
                                Log.d(TAG, "Image set to ImageView")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing Base64 image: ${e.message}", e)
                            profileImageView?.setImageResource(R.drawable.default_profile)
                        }
                    } ?: run {
                        Log.d(TAG, "No profile image found, using default")
                        profileImageView?.setImageResource(R.drawable.default_profile)
                    }
                } else {
                    Log.w(TAG, "User document does not exist")
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading profile: ${e.message}", e)
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val minDim = Math.min(bitmap.width, bitmap.height)
        val squareBitmap = if (bitmap.width != bitmap.height) {
            val x = (bitmap.width - minDim) / 2
            val y = (bitmap.height - minDim) / 2
            Bitmap.createBitmap(bitmap, x, y, minDim, minDim)
        } else {
            bitmap
        }

        val output = Bitmap.createBitmap(squareBitmap.width, squareBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
        }

        val rect = android.graphics.RectF(0f, 0f, squareBitmap.width.toFloat(), squareBitmap.height.toFloat())
        canvas.drawOval(rect, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squareBitmap, 0f, 0f, paint)

        return output
    }

    private fun setupClickListeners() {
        btnEdit?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnLogout?.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, UserLoginActivity::class.java))
            finish()
        }
    }

    private fun checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        try {
                            val addresses = geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1
                            )
                            if (addresses != null && addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val fullAddress = buildString {
                                    append(address.getAddressLine(0) ?: "")
                                    if (address.locality != null) {
                                        append(", ${address.locality}")
                                    }
                                    if (address.adminArea != null) {
                                        append(", ${address.adminArea}")
                                    }
                                    if (address.postalCode != null) {
                                        append(" ${address.postalCode}")
                                    }
                                    if (address.countryName != null) {
                                        append(", ${address.countryName}")
                                    }
                                }
                                tvAddress?.text = fullAddress
                                
                                // Update address in Firestore
                                val currentUser = auth.currentUser
                                if (currentUser != null) {
                                    db.collection("users").document(currentUser.uid)
                                        .update("address", fullAddress)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Address updated successfully", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error updating address: ${e.message}", e)
                                            Toast.makeText(this, "Error updating address: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                Toast.makeText(this, "No address found for this location", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Error getting address: ${e.message}", e)
                            Toast.makeText(this, "Error getting address: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Could not get location. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting location: ${e.message}", e)
                    Toast.makeText(this, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getLocation: ${e.message}", e)
            Toast.makeText(this, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation()
        } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
