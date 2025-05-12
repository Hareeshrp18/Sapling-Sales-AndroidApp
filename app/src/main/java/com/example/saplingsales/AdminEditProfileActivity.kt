package com.example.saplingsales

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import android.util.Log

class AdminEditProfileActivity : AppCompatActivity() {
    private lateinit var ivAdminProfile: ImageView
    private lateinit var btnChangePhoto: MaterialButton
    private lateinit var etAdminName: TextInputEditText
    private lateinit var etStoreName: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: View
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var currentImageBase64: String? = null
    
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivAdminProfile.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit_profile)
        
        try {
            initializeViews()
            setupToolbar()
            loadAdminDetails()
            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initializeViews() {
        ivAdminProfile = findViewById(R.id.ivAdminProfile)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        etAdminName = findViewById(R.id.etAdminName)
        etStoreName = findViewById(R.id.etStoreName)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"
    }

    private fun setupClickListeners() {
        btnChangePhoto.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnSave.setOnClickListener {
            saveAdminDetails()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun openImagePicker() {
        try {
            pickImage.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening image picker: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadAdminDetails() {
        auth.currentUser?.uid?.let { uid ->
            db.collection("saplingAdmin").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        etAdminName.setText(document.getString("adminname"))
                        etStoreName.setText(document.getString("storename"))
                        
                        // Load profile image if exists
                        val imageBase64 = document.getString("imageBase64")
                        if (!imageBase64.isNullOrEmpty()) {
                            currentImageBase64 = imageBase64
                            try {
                                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivAdminProfile.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveAdminDetails() {
        val adminName = etAdminName.text.toString().trim()
        val storeName = etStoreName.text.toString().trim()

        if (adminName.isEmpty() || storeName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        try {
            auth.currentUser?.uid?.let { uid ->
                // Create a map for the update data
                val updateData = hashMapOf<String, Any>()
                
                // Add the basic fields
                updateData["adminname"] = adminName
                updateData["storename"] = storeName

                // Handle image update
                selectedImageUri?.let { uri ->
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                        val imageBytes = baos.toByteArray()
                        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                        updateData["imageBase64"] = base64Image
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    // Keep existing image if no new image selected
                    currentImageBase64?.let { updateData["imageBase64"] = it }
                }

                // Update the document using set with merge
                db.collection("saplingAdmin").document(uid)
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        Log.e("AdminEditProfile", "Error updating profile: ${e.message}")
                        Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            btnSave.isEnabled = true
            Log.e("AdminEditProfile", "Error in save process: ${e.message}")
            Toast.makeText(this, "Error in save process: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Please allow access to photos to change profile picture.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
} 