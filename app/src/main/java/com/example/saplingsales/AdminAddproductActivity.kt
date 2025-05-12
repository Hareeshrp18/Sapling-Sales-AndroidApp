package com.example.saplingsales

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.adapters.AdminImageAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.IOException

class AdminAddproductActivity : AppCompatActivity() {

    private lateinit var editTextProductName: TextInputEditText
    private lateinit var editTextProductDescription: TextInputEditText
    private lateinit var editTextProductPrice: TextInputEditText
    private lateinit var editTextQuantity: TextInputEditText
    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var spinnerStatus: AutoCompleteTextView
    private lateinit var recyclerViewImages: RecyclerView
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonUpload: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar

    private var selectedImageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: AdminImageAdapter
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storagePermissionCode = 101

    private var selectedCategory: String = "Other"
    private var selectedStatus: String = "Available"

    private val PICK_IMAGE_REQUEST = 1

    private val imageBase64List = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        try {
            initializeViews()
            setupToolbar()
            setupSpinners()
            setupImageRecyclerView()
            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextProductDescription = findViewById(R.id.editTextProductDescription)
        editTextProductPrice = findViewById(R.id.editTextProductPrice)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        recyclerViewImages = findViewById(R.id.recyclerViewImages)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        buttonUpload = findViewById(R.id.buttonUpload)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Product"
    }

    private fun setupSpinners() {
        // Category Spinner
        val categories = arrayOf("Fruits", "Vegetables", "Flowers", "Tree Seed", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(categoryAdapter)
        spinnerCategory.setText(categories[0], false)
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }

        // Status Spinner
        val statuses = arrayOf("Available", "Out of Stock")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        spinnerStatus.setAdapter(statusAdapter)
        spinnerStatus.setText(statuses[0], false)
        spinnerStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = statuses[position]
        }
    }

    private fun setupImageRecyclerView() {
        recyclerViewImages.layoutManager = GridLayoutManager(this, 2)
        imageAdapter = AdminImageAdapter(
            images = imageBase64List,
            onRemoveClick = { position -> 
                imageBase64List.removeAt(position)
                imageAdapter.notifyItemRemoved(position)
            },
            onAddClick = { 
                openImagePicker()
            }
        )
        recyclerViewImages.adapter = imageAdapter
    }

    private fun setupClickListeners() {
        buttonSelectImage.setOnClickListener {
            checkAndRequestPermissions()
        }

        buttonUpload.setOnClickListener {
            if (validateInputs()) {
                uploadProduct()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                openImagePicker()
            } else {
                requestPermission(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                openImagePicker()
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), storagePermissionCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening image picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
                if (data?.clipData != null) {
                    // Multiple images selected
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = data.clipData!!.getItemAt(i).uri
                        handleSelectedImage(imageUri)
                    }
                } else if (data?.data != null) {
                    // Single image selected
                    handleSelectedImage(data.data!!)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing selected images: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val base64Image = convertImageToBase64(uri)
            if (base64Image != null) {
                imageBase64List.add(base64Image)
                imageAdapter.notifyItemInserted(imageBase64List.size - 1)
            }
        } catch (e: Exception) {
            Log.e("AdminAddProduct", "Error handling selected image", e)
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        val name = editTextProductName.text.toString().trim()
        val description = editTextProductDescription.text.toString().trim()
        val price = editTextProductPrice.text.toString().trim()
        val quantity = editTextQuantity.text.toString().trim()

        if (name.isEmpty()) {
            editTextProductName.error = "Product name is required"
            return false
        }
        if (description.isEmpty()) {
            editTextProductDescription.error = "Description is required"
            return false
        }
        if (price.isEmpty()) {
            editTextProductPrice.error = "Price is required"
            return false
        }
        if (quantity.isEmpty()) {
            editTextQuantity.error = "Quantity is required"
            return false
        }
        if (imageBase64List.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun compressAndEncodeImage(bitmap: Bitmap): String {
        try {
            // Scale down the image if it's too large
            val maxDimension = 1024
            var scaledBitmap = bitmap
            
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val scale = minOf(
                    maxDimension.toFloat() / bitmap.width,
                    maxDimension.toFloat() / bitmap.height
                )
                
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                if (bitmap != scaledBitmap) {
                    bitmap.recycle()
                }
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("AdminAddProduct", "Error compressing image: ${e.message}")
            throw e
        }
    }

    private fun convertImageToBase64(uri: Uri): String? {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            return compressAndEncodeImage(bitmap)
        } catch (e: IOException) {
            Log.e("AdminAddProduct", "Error reading image: ${e.message}")
            return null
        }
    }

    private fun uploadProduct() {
        try {
            if (!validateInputs()) {
                return
            }

            progressBar.visibility = View.VISIBLE
            buttonUpload.isEnabled = false

            // Create product data
            val productData = mapOf(
                "name" to editTextProductName.text.toString().trim(),
                "description" to editTextProductDescription.text.toString().trim(),
                "price" to editTextProductPrice.text.toString().trim().toDouble(),
                "quantity" to editTextQuantity.text.toString().trim().toInt(),
                "category" to selectedCategory,
                "status" to selectedStatus,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "imageBase64" to (imageBase64List.firstOrNull() ?: ""),
                "images" to imageBase64List,
                "searchKeywords" to generateSearchKeywords(editTextProductName.text.toString().trim())
            )

            firestore.collection("saplingProducts")
                .add(productData)
                .addOnSuccessListener { documentReference ->
                    progressBar.visibility = View.GONE
                    buttonUpload.isEnabled = true
                    Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    buttonUpload.isEnabled = true
                    Toast.makeText(this, "Error adding product: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AdminAddProduct", "Error adding product", e)
                }

        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            buttonUpload.isEnabled = true
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AdminAddProduct", "Error in uploadProduct", e)
        }
    }

    private fun generateSearchKeywords(productName: String): List<String> {
        val keywords = mutableListOf<String>()
        val name = productName.toLowerCase()
        
        // Add full name
        keywords.add(name)
        
        // Add each word individually
        keywords.addAll(name.split(" "))
        
        // Add partial matches (for search-as-you-type)
        for (i in 1..name.length) {
            keywords.add(name.substring(0, i))
        }
        
        return keywords.distinct()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}

