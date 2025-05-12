package com.example.saplingsales

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var profileImageView: ImageView
    private lateinit var btnChangePhoto: MaterialButton
    private lateinit var tilName: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilAddress: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnGetLocation: MaterialButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    
    private val TAG = "EditProfileActivity"

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                profileImageView.setImageURI(uri)
                Log.d(TAG, "Image selected: $uri")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initializeViews()
        setupToolbar()
        loadCurrentData()
        setupClickListeners()
    }

    private fun initializeViews() {
        try {
            // Initialize Toolbar
            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            
            // Initialize Image related views
            profileImageView = findViewById(R.id.profileImageView)
            btnChangePhoto = findViewById(R.id.btnChangePhoto)
            
            // Initialize TextInputLayouts
            tilName = findViewById(R.id.tilName)
            tilPhone = findViewById(R.id.tilPhone)
            tilAddress = findViewById(R.id.tilAddress)
            
            // Initialize EditTexts
            etName = findViewById(R.id.etName)
            etPhone = findViewById(R.id.etPhone)
            etAddress = findViewById(R.id.etAddress)
            
            // Initialize Buttons
            btnSaveChanges = findViewById(R.id.btnSaveChanges)
            btnGetLocation = findViewById(R.id.btnGetLocation)

            // Verify all views are initialized
            if (toolbar == null || profileImageView == null || btnChangePhoto == null ||
                tilName == null || tilPhone == null || tilAddress == null ||
                etName == null || etPhone == null || etAddress == null ||
                btnSaveChanges == null || btnGetLocation == null) {
                throw IllegalStateException("Failed to initialize one or more views")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = "Edit Profile"
            }
            toolbar.setNavigationOnClickListener { 
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar: ${e.message}", e)
        }
    }

    private fun loadCurrentData() {
        val userId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Loading data for user: $userId")
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etName.setText(document.getString("name"))
                    etPhone.setText(document.getString("phone"))
                    etAddress.setText(document.getString("address"))
                    
                    // Load profile image if exists
                    document.getString("ImageBase64")?.let { base64String ->
                        Log.d(TAG, "Loading profile image from Base64")
                        try {
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.let { bitmap ->
                                profileImageView.setImageBitmap(bitmap)
                                Log.d(TAG, "Image set to ImageView")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing Base64 image: ${e.message}", e)
                            profileImageView.setImageResource(R.drawable.default_profile)
                        }
                    } ?: run {
                        Log.d(TAG, "No profile image found, using default")
                        profileImageView.setImageResource(R.drawable.default_profile)
                    }
                } else {
                    Log.d(TAG, "No user document found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading profile: ${e.message}", e)
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }

        btnSaveChanges.setOnClickListener {
            saveProfile()
        }

        btnGetLocation.setOnClickListener {
            checkLocationPermissionAndGetLocation()
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
                                etAddress.setText(fullAddress)
                                Toast.makeText(this, "Location found", Toast.LENGTH_SHORT).show()
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

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Saving profile for user: $userId")
        
        val userData = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "address" to address
        )

        // If there's a new image selected, convert it to Base64
        if (selectedImageUri != null) {
            Log.d(TAG, "Converting new profile image to Base64")
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                val base64String = convertBitmapToBase64(bitmap)
                userData["ImageBase64"] = base64String
                Log.d(TAG, "Image converted to Base64 successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Error converting image to Base64: ${e.message}", e)
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        updateUserProfile(userId, userData)
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun updateUserProfile(userId: String, userData: MutableMap<String, Any>) {
        Log.d(TAG, "Updating user profile with data: $userData")
        
        db.collection("users").document(userId)
            .update(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Profile updated successfully")
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating profile: ${e.message}", e)
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
