package com.example.saplingsales

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.activities.AdminViewProductActivity
import com.example.saplingsales.adapters.EditProductImagesAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class EditProductActivity : AppCompatActivity() {
    private lateinit var recyclerViewImages: RecyclerView
    private lateinit var buttonSelectImages: Button
    private lateinit var textViewImageCount: TextView
    private lateinit var editTextProductName: TextInputEditText
    private lateinit var editTextProductPrice: TextInputEditText
    private lateinit var editTextQuantity: TextInputEditText
    private lateinit var editTextProductDescription: TextInputEditText
    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var spinnerStatus: AutoCompleteTextView
    private lateinit var categoryLayout: TextInputLayout
    private lateinit var statusLayout: TextInputLayout
    private lateinit var buttonUpdate: Button
    private lateinit var buttonCancel: Button
    private lateinit var imagesAdapter: EditProductImagesAdapter
    private lateinit var db: FirebaseFirestore
    private var productId: String? = null

    private val categories = listOf("Fruits", "Vegetables", "Herbs", "Flowers", "Tree Seed", "Other")
    private val statuses = listOf("Available", "Out of Stock", "Discontinued")

    private val getContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let { selectedUris ->
            val remainingSlots = 12 - imagesAdapter.getImages().size
            val urisToProcess = selectedUris.take(remainingSlots)

            urisToProcess.forEach { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    imagesAdapter.addImage(bitmap)
                    updateImageCount()
                } catch (e: Exception) {
                    Log.e("EditProductActivity", "Error loading image: ${e.message}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        db = FirebaseFirestore.getInstance()
        initializeViews()
        setupRecyclerView()
        setupSpinners()
        setupClickListeners()

        productId = intent.getStringExtra("productId")
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Product ID not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProductDetails()
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

    private fun initializeViews() {
        recyclerViewImages = findViewById(R.id.recyclerViewImages)
        buttonSelectImages = findViewById(R.id.buttonSelectImages)
        textViewImageCount = findViewById(R.id.textViewImageCount)
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextProductPrice = findViewById(R.id.editTextProductPrice)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        editTextProductDescription = findViewById(R.id.editTextProductDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        categoryLayout = findViewById(R.id.categoryLayout)
        statusLayout = findViewById(R.id.statusLayout)
        buttonUpdate = findViewById(R.id.buttonUpdate)
        buttonCancel = findViewById(R.id.buttonCancel)
    }

    private fun setupRecyclerView() {
        imagesAdapter = EditProductImagesAdapter()
        recyclerViewImages.apply {
            layoutManager = LinearLayoutManager(this@EditProductActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = imagesAdapter
        }

        imagesAdapter.setOnDeleteClickListener { position ->
            imagesAdapter.removeImage(position)
            updateImageCount()
        }
    }

    private fun setupSpinners() {
        // Set up category spinner
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        spinnerCategory.setAdapter(categoryAdapter)
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            categoryLayout.error = null
        }

        // Set up status spinner
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            statuses
        )
        spinnerStatus.setAdapter(statusAdapter)
        spinnerStatus.setOnItemClickListener { _, _, position, _ ->
            statusLayout.error = null
        }

        // Enable dropdown on click
        spinnerCategory.setOnClickListener {
            spinnerCategory.showDropDown()
        }
        spinnerStatus.setOnClickListener {
            spinnerStatus.showDropDown()
        }
    }

    private fun setupClickListeners() {
        buttonSelectImages.setOnClickListener {
            if (imagesAdapter.getImages().size >= 12) {
                Toast.makeText(this, "Maximum 12 images allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getContent.launch("image/*")
        }

        buttonUpdate.setOnClickListener { updateProduct() }
        buttonCancel.setOnClickListener { finish() }
    }

    private fun updateImageCount() {
        val count = imagesAdapter.getImages().size
        textViewImageCount.text = "Selected Images: $count/12"
        buttonSelectImages.isEnabled = count < 12
    }

    private fun loadProductDetails() {
        Log.d("EditProductActivity", "Loading product details for ID: $productId")
        
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Product ID not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("saplingProducts").document(productId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        Log.d("EditProductActivity", "Document data: ${document.data}")

                        // Load basic details
                        val name = document.getString("name")
                        val price = document.getDouble("price")
                        val quantity = document.getLong("quantity")
                        val description = document.getString("description")
                        val category = document.getString("category")
                        val status = document.getString("status")
                        val images = document.get("images") as? List<String>

                        // Set text fields
                        editTextProductName.setText(name)
                        editTextProductPrice.setText(price?.toString())
                        editTextQuantity.setText(quantity?.toString())
                        editTextProductDescription.setText(description)
                        
                        // Set spinners
                        if (!category.isNullOrEmpty()) {
                            spinnerCategory.setText(category, false)
                        }
                        if (!status.isNullOrEmpty()) {
                            spinnerStatus.setText(status, false)
                        }

                        // Load images
                        images?.let { imageList ->
                            for (base64Image in imageList) {
                                try {
                                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    imagesAdapter.addImage(bitmap)
                                } catch (e: Exception) {
                                    Log.e("EditProductActivity", "Error loading image: ${e.message}")
                                }
                            }
                            updateImageCount()
                        }

                    } catch (e: Exception) {
                        Log.e("EditProductActivity", "Error parsing document: ${e.message}")
                        Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProductActivity", "Error loading product: ${e.message}")
                Toast.makeText(this, "Error loading product: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
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
            Log.e("EditProductActivity", "Error compressing image: ${e.message}")
            throw e
        }
    }

    private fun updateProduct() {
        if (!validateInput()) return

        val images = imagesAdapter.getImages()
        if (images.size < 4) {
            Toast.makeText(this, "Please select at least 4 images", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Show progress
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.VISIBLE
            buttonUpdate.isEnabled = false

            // Process images in background
            Thread {
                try {
                    val imageBase64List = mutableListOf<String>()
                    for (bitmap in images) {
                        try {
                            val base64String = compressAndEncodeImage(bitmap)
                            imageBase64List.add(base64String)
                        } catch (e: Exception) {
                            Log.e("EditProductActivity", "Error processing image: ${e.message}")
                        }
                    }

                    runOnUiThread {
                        try {
                            // Convert numeric values safely
                            val priceText = editTextProductPrice.text.toString().trim()
                            val quantityText = editTextQuantity.text.toString().trim()
                            
                            // Validate numeric values
                            val price = try {
                                priceText.toDouble()
                            } catch (e: NumberFormatException) {
                                progressBar.visibility = View.GONE
                                buttonUpdate.isEnabled = true
                                Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show()
                                return@runOnUiThread
                            }

                            val quantity = try {
                                quantityText.toInt()
                            } catch (e: NumberFormatException) {
                                progressBar.visibility = View.GONE
                                buttonUpdate.isEnabled = true
                                Toast.makeText(this, "Invalid quantity format", Toast.LENGTH_SHORT).show()
                                return@runOnUiThread
                            }

                            val productData = hashMapOf(
                                "name" to editTextProductName.text.toString().trim(),
                                "description" to editTextProductDescription.text.toString().trim(),
                                "price" to price,
                                "quantity" to quantity,
                                "category" to spinnerCategory.text.toString().trim(),
                                "status" to spinnerStatus.text.toString().trim(),
                                "updatedAt" to com.google.firebase.Timestamp.now(),
                                "imageBase64" to (imageBase64List.firstOrNull() ?: ""),
                                "images" to imageBase64List,
                                "searchKeywords" to generateSearchKeywords(editTextProductName.text.toString().trim())
                            )

                            // Update document using update instead of set
        db.collection("saplingProducts").document(productId!!)
                                .update(productData as Map<String, Any>)
            .addOnSuccessListener {
                                    progressBar.visibility = View.GONE
                                    buttonUpdate.isEnabled = true
                                    Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, AdminViewProductActivity::class.java)
                                    startActivity(intent)
                                    finish()
                }
            .addOnFailureListener { e ->
                                    progressBar.visibility = View.GONE
                                    buttonUpdate.isEnabled = true
                Log.e("EditProductActivity", "Error updating product: ${e.message}")
                                    Toast.makeText(this, "Error updating product: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } catch (e: Exception) {
                            progressBar.visibility = View.GONE
                            buttonUpdate.isEnabled = true
                            Log.e("EditProductActivity", "Error preparing update data: ${e.message}")
                            Toast.makeText(this, "Error preparing update data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        buttonUpdate.isEnabled = true
                        Log.e("EditProductActivity", "Error processing images: ${e.message}")
                        Toast.makeText(this, "Error processing images: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } catch (e: Exception) {
            Log.e("EditProductActivity", "Error in update process: ${e.message}")
            Toast.makeText(this, "Error in update process: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateSearchKeywords(name: String): List<String> {
        val keywords = mutableListOf<String>()
        val words = name.lowercase().split(" ")
        
        // Add full name
        keywords.add(name.lowercase())
        
        // Add individual words
        keywords.addAll(words)
        
        // Add partial matches (substrings)
        words.forEach { word ->
            for (i in 1..word.length) {
                keywords.add(word.substring(0, i))
            }
        }
        
        return keywords.distinct()
    }

    private fun validateInput(): Boolean {
        if (editTextProductName.text.isNullOrBlank()) {
            editTextProductName.error = "Product name is required"
            return false
        }
        if (editTextProductPrice.text.isNullOrBlank()) {
            editTextProductPrice.error = "Price is required"
            return false
        }
        if (editTextQuantity.text.isNullOrBlank()) {
            editTextQuantity.error = "Quantity is required"
            return false
        }
        if (editTextProductDescription.text.isNullOrBlank()) {
            editTextProductDescription.error = "Description is required"
            return false
        }
        if (spinnerCategory.text.isNullOrBlank()) {
            categoryLayout.error = "Category is required"
            return false
        }
        if (spinnerStatus.text.isNullOrBlank()) {
            statusLayout.error = "Status is required"
            return false
        }
        return true
    }
}
