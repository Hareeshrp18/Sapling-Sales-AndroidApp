package com.example.saplingsales.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.ImageAdapter
import com.example.saplingsales.adapters.UserProductAdapter
import com.example.saplingsales.models.Product
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import com.example.saplingsales.ImageDataHolder

class UserViewProductActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var imageSlider: RecyclerView
    private lateinit var imageCounterBadge: TextView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var productCategory: TextView
    private lateinit var productQuantity: TextView
    private lateinit var btnAddToCart: MaterialButton
    private lateinit var similarProductsRecyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var productStatus: TextView
    private lateinit var favoriteButton: ImageView
    private var product: Product? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var imageAdapter: ImageAdapter? = null
    private lateinit var similarProductsAdapter: UserProductAdapter
    private var isFavorite = false
    private val TAG = "UserViewProductActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_user_view_product)
            initializeViews()
            setupViews()
            loadProduct()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing screen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            imageSlider = findViewById(R.id.imageSlider)
            imageCounterBadge = findViewById(R.id.imageCounterBadge)
            productName = findViewById(R.id.tvProductName)
            productPrice = findViewById(R.id.tvProductPrice)
            productDescription = findViewById(R.id.tvProductDescription)
            productCategory = findViewById(R.id.tvProductCategory)
            productStatus = findViewById(R.id.tvStatus)
            btnAddToCart = findViewById(R.id.btnAddToCart)
            similarProductsRecyclerView = findViewById(R.id.rvSimilarProducts)
            progressBar = findViewById(R.id.progressBar)
            favoriteButton = findViewById(R.id.btnFavorite)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding views: ${e.message}", e)
            throw e
        }
    }

    private fun setupViews() {
        try {
            // Setup toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // Setup image slider
            imageSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            PagerSnapHelper().attachToRecyclerView(imageSlider)
            imageAdapter = com.example.saplingsales.ImageAdapter(emptyList()) { imageData ->
                val images = (imageAdapter?.let { (it as com.example.saplingsales.ImageAdapter).getImages() } ?: emptyList())
                ImageDataHolder.images = images
                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putExtra("position", images.indexOf(imageData))
                startActivity(intent)
            }
            imageSlider.adapter = imageAdapter

            // Setup image counter
            imageSlider.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        updateImageCounter(position)
                    }
                }
            })

            // Setup similar products RecyclerView
            similarProductsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            similarProductsAdapter = UserProductAdapter(
                onProductClick = { clickedProduct ->
                    val intent = Intent(this, UserViewProductActivity::class.java)
                    intent.putExtra("productId", clickedProduct.id)
                    startActivity(intent)
                },
                onAddToCartClick = { clickedProduct ->
                    addToCart(clickedProduct)
                }
            )
            similarProductsRecyclerView.adapter = similarProductsAdapter

            // Setup click listeners
            btnAddToCart.setOnClickListener {
                product?.let { p -> addToCart(p) }
            }
            
            // Setup favorite button click listener
            favoriteButton.setOnClickListener {
                if (auth.currentUser == null) {
                    Toast.makeText(this, "Please log in to add favorites", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, com.example.saplingsales.UserLoginActivity::class.java))
                    return@setOnClickListener
                }
                toggleFavorite()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}", e)
            throw e
        }
    }

    private fun loadProduct() {
        val productId = intent.getStringExtra("productId")
        if (productId.isNullOrEmpty()) {
            Log.e(TAG, "Product ID is null or empty")
            Toast.makeText(this, "Invalid product ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Loading product with ID: $productId")
        progressBar.visibility = View.VISIBLE

        firestore.collection("saplingProducts")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    try {
                        Log.d(TAG, "Document exists, converting to Product object")
                        val loadedProduct = document.toObject(Product::class.java)
                        if (loadedProduct != null) {
                            // Copy document ID and assign to product
                            product = loadedProduct.copy(id = document.id)
                            
                            // Set default values for nullable fields if needed
                            product = product?.copy(
                                description = loadedProduct.description ?: "No description available",
                                status = loadedProduct.status ?: "Available",
                                category = loadedProduct.category ?: "Uncategorized",
                                quantity = loadedProduct.quantity ?: 0
                            )
                            
                            Log.d(TAG, "Product loaded successfully: ${product?.name}")
                            displayProduct()
                            checkFavoriteStatus()
                            loadSimilarProducts()
                        } else {
                            Log.e(TAG, "Failed to convert document to Product object")
                            Toast.makeText(this, "Invalid product data", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product: ${e.message}", e)
                        Toast.makeText(this, "Error loading product data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Log.e(TAG, "Document does not exist for ID: $productId")
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading product: ${e.message}", e)
                Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayProduct() {
        try {
            product?.let { p ->
                // Set basic product information
                productName.text = p.name ?: "Unnamed Product"
                productDescription.text = p.description ?: "No description available"
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                productPrice.text = formatter.format(p.price ?: 0.0)
                productStatus.text = p.status ?: "Available"
                productStatus.visibility = if (!p.status.isNullOrEmpty()) View.VISIBLE else View.GONE
                productCategory.text = p.category ?: "Uncategorized"

                // Handle product images
                val images = mutableListOf<String>()
                
                // Add base64 image if available
                if (!p.imageBase64.isNullOrEmpty()) {
                    Log.d(TAG, "Adding base64 image")
                    images.add(p.imageBase64)
                }
                
                // Add image URLs if available
                if (!p.imageUrls.isNullOrEmpty()) {
                    Log.d(TAG, "Adding ${p.imageUrls.size} image URLs")
                    images.addAll(p.imageUrls)
                }
                
                // Add images from the images field if available
                if (!p.images.isNullOrEmpty()) {
                    Log.d(TAG, "Adding ${p.images.size} images from images field")
                    images.addAll(p.images)
                }
                
                Log.d(TAG, "Total images to display: ${images.size}")
                
                if (images.isNotEmpty()) {
                    imageAdapter?.submitList(images)
                    imageCounterBadge.visibility = if (images.size > 1) View.VISIBLE else View.GONE
                    imageCounterBadge.text = "1/${images.size}"
                } else {
                    // Set default image
                    Log.d(TAG, "No images found, using default image")
                    imageAdapter?.submitList(listOf("default"))
                    imageCounterBadge.visibility = View.GONE
                }

                // Enable/disable add to cart button based on quantity
                btnAddToCart.isEnabled = (p.quantity ?: 0) > 0
                btnAddToCart.alpha = if ((p.quantity ?: 0) > 0) 1.0f else 0.5f
                
                // Make sure similar products section is visible
                similarProductsRecyclerView.visibility = View.VISIBLE
            } ?: run {
                Log.e(TAG, "Product is null in displayProduct")
                Toast.makeText(this, "Error displaying product", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in displayProduct: ${e.message}", e)
            Toast.makeText(this, "Error displaying product", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun addToCart(product: Product) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to add items to cart", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, com.example.saplingsales.UserLoginActivity::class.java))
            return
        }

        // Check if product quantity is available
        if ((product.quantity ?: 0) <= 0) {
            Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress while processing
        progressBar.visibility = View.VISIBLE
        btnAddToCart.isEnabled = false

        try {
            // First check if the product is already in cart
            firestore.collection("cartProduct")
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", product.id)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Product already in cart, update quantity
                        val document = documents.documents[0]
                        val currentQuantity = document.getLong("quantity") ?: 0
                        
                        // Check if adding one more would exceed available quantity
                        if (currentQuantity + 1 > (product.quantity ?: 0)) {
                            progressBar.visibility = View.GONE
                            btnAddToCart.isEnabled = true
                            Toast.makeText(this, "Cannot add more items. Maximum quantity reached", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Update quantity in cart
                        document.reference.update("quantity", currentQuantity + 1)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                btnAddToCart.isEnabled = true
                                Toast.makeText(this, "Cart updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnAddToCart.isEnabled = true
                                Log.e(TAG, "Error updating cart: ${e.message}", e)
                                Toast.makeText(this, "Error updating cart", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Product not in cart, add new item
                        val cartItem = hashMapOf(
                            "userId" to userId,
                            "productId" to product.id,
                            "quantity" to 1,
                            "price" to (product.price ?: 0.0),
                            "productName" to (product.name ?: ""),
                            "productImage" to (product.imageBase64 ?: ""),
                            "productImageUrl" to (product.imageUrls?.firstOrNull() ?: ""),
                            "productCategory" to (product.category ?: ""),
                            "addedAt" to System.currentTimeMillis(),
                            "status" to "pending"
                        )

                        // Add to cartProduct collection
                        firestore.collection("cartProduct")
                            .add(cartItem)
                            .addOnSuccessListener { documentReference ->
                                // Add to saplingOrders collection
                                val orderItem = hashMapOf(
                                    "userId" to userId,
                                    "productId" to product.id,
                                    "quantity" to 1,
                                    "price" to (product.price ?: 0.0),
                                    "productName" to (product.name ?: ""),
                                    "productImage" to (product.imageBase64 ?: ""),
                                    "productImageUrl" to (product.imageUrls?.firstOrNull() ?: ""),
                                    "productCategory" to (product.category ?: ""),
                                    "status" to "pending",
                                    "orderDate" to System.currentTimeMillis()
                                )

                                firestore.collection("saplingOrders")
                                    .add(orderItem)
                                    .addOnSuccessListener {
                                        progressBar.visibility = View.GONE
                                        btnAddToCart.isEnabled = true
                                        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        progressBar.visibility = View.GONE
                                        btnAddToCart.isEnabled = true
                                        Log.e(TAG, "Error adding to orders: ${e.message}", e)
                                        Toast.makeText(this, "Error adding to orders", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnAddToCart.isEnabled = true
                                Log.e(TAG, "Error adding to cart: ${e.message}", e)
                                Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnAddToCart.isEnabled = true
                    Log.e(TAG, "Error checking cart: ${e.message}", e)
                    Toast.makeText(this, "Error checking cart", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            btnAddToCart.isEnabled = true
            Log.e(TAG, "Exception in addToCart: ${e.message}", e)
            Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkFavoriteStatus() {
        val userId = auth.currentUser?.uid
        val productId = product?.id
        
        if (userId == null || productId == null) {
            Log.d(TAG, "User ID or Product ID is null in checkFavoriteStatus")
            updateFavoriteButton()
            return
        }

        try {
            progressBar.visibility = View.VISIBLE
            // Create the document ID using userId and productId
            val favoriteId = "${userId}_${productId}"
            
            firestore.collection("favouriteProduct")
                .document(favoriteId)
                .get()
                .addOnSuccessListener { document ->
                    progressBar.visibility = View.GONE
                    isFavorite = document.exists()
                    updateFavoriteButton()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Error checking favorite status: ${e.message}", e)
                    updateFavoriteButton()
                }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e(TAG, "Exception in checkFavoriteStatus: ${e.message}", e)
            updateFavoriteButton()
        }
    }

    private fun updateFavoriteButton() {
        try {
            favoriteButton.setImageResource(
                if (isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )
            favoriteButton.isEnabled = true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating favorite button: ${e.message}", e)
        }
    }

    private fun toggleFavorite() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Please log in to add favorites", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, com.example.saplingsales.UserLoginActivity::class.java))
                return
            }

            val currentProduct = product
            if (currentProduct == null) {
                Log.e(TAG, "Product is null in toggleFavorite")
                Toast.makeText(this, "Error: Product not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Show progress while processing
            progressBar.visibility = View.VISIBLE
            favoriteButton.isEnabled = false

            // Create the document ID using userId and productId
            val favoriteId = "${userId}_${currentProduct.id}"
            val favoriteRef = firestore.collection("favouriteProduct").document(favoriteId)

            if (isFavorite) {
                // Remove from favorites
                favoriteRef.delete()
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        isFavorite = false
                        updateFavoriteButton()
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        Log.e(TAG, "Error removing from favorites: ${e.message}", e)
                        Toast.makeText(this, "Error removing from favorites", Toast.LENGTH_SHORT).show()
                        updateFavoriteButton()
                    }
            } else {
                // Add to favorites
                val favorite = hashMapOf(
                    "id" to favoriteId,
                    "userId" to userId,
                    "productId" to currentProduct.id,
                    "productName" to (currentProduct.name ?: ""),
                    "productPrice" to (currentProduct.price ?: 0.0),
                    "productDescription" to (currentProduct.description ?: ""),
                    "productCategory" to (currentProduct.category ?: ""),
                    "addedAt" to System.currentTimeMillis()
                )

                // Add image data based on availability
                when {
                    !currentProduct.imageBase64.isNullOrEmpty() -> 
                        favorite["productImage"] = currentProduct.imageBase64
                    currentProduct.imageUrls.isNotEmpty() -> 
                        favorite["productImageUrl"] = currentProduct.imageUrls[0]
                    currentProduct.images.isNotEmpty() -> 
                        favorite["productImage"] = currentProduct.images[0]
                }

                favoriteRef.set(favorite)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        isFavorite = true
                        updateFavoriteButton()
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        Log.e(TAG, "Error adding to favorites: ${e.message}", e)
                        Toast.makeText(this, "Error adding to favorites", Toast.LENGTH_SHORT).show()
                        updateFavoriteButton()
                    }
            }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e(TAG, "Exception in toggleFavorite: ${e.message}", e)
            Toast.makeText(this, "Error updating favorites", Toast.LENGTH_SHORT).show()
            updateFavoriteButton()
        }
    }

    private fun updateImageCounter(position: Int) {
        val totalImages = imageAdapter?.itemCount ?: 0
        if (totalImages > 1) {
            imageCounterBadge.visibility = View.VISIBLE
            imageCounterBadge.text = "${position + 1}/$totalImages"
        } else {
            imageCounterBadge.visibility = View.GONE
        }
    }

    private fun loadSimilarProducts() {
        // Load similar products based on category
        val category = product?.category
        if (!category.isNullOrEmpty()) {
            Log.d(TAG, "Loading similar products for category: $category")
            
            // Show loading indicator for similar products
            similarProductsRecyclerView.visibility = View.VISIBLE
            
            try {
                // First try to get products from the same category
                firestore.collection("saplingProducts")
                    .whereEqualTo("category", category)
                    .limit(20) // Get more products to ensure we have enough after filtering
                    .get()
                    .addOnSuccessListener { documents ->
                        val similarProducts = mutableListOf<Product>()
                        
                        // Filter out the current product
                        val currentProductId = product?.id
                        for (document in documents) {
                            document.toObject(Product::class.java)?.let { product ->
                                if (document.id != currentProductId) { // Compare document IDs instead of product IDs
                                    similarProducts.add(product.copy(id = document.id))
                                }
                            }
                        }
                        
                        Log.d(TAG, "Loaded ${similarProducts.size} similar products")
                        
                        if (similarProducts.isEmpty()) {
                            // If no similar products found, try to load any products
                            loadAnyProducts()
                        } else {
                            // Limit to 10 products
                            val limitedProducts = if (similarProducts.size > 10) {
                                similarProducts.take(10)
                            } else {
                                similarProducts
                            }
                            similarProductsAdapter.submitList(limitedProducts)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error loading similar products: ${e.message}", e)
                        // Try to load any products as fallback
                        loadAnyProducts()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadSimilarProducts: ${e.message}", e)
                // Try to load any products as fallback
                loadAnyProducts()
            }
        } else {
            Log.d(TAG, "Product category is empty, trying to load any products")
            loadAnyProducts()
        }
    }
    
    private fun loadAnyProducts() {
        try {
            firestore.collection("saplingProducts")
                .limit(20) // Get more products to ensure we have enough after filtering
                .get()
                .addOnSuccessListener { documents ->
                    val products = mutableListOf<Product>()
                    
                    // Filter out the current product
                    val currentProductId = product?.id
                    for (document in documents) {
                        document.toObject(Product::class.java)?.let { product ->
                            if (document.id != currentProductId) { // Compare document IDs instead of product IDs
                                products.add(product.copy(id = document.id))
                            }
                        }
                    }
                    
                    Log.d(TAG, "Loaded ${products.size} fallback products")
                    
                    if (products.isEmpty()) {
                        // If still no products, show a message
                        Toast.makeText(this, "No similar products available", Toast.LENGTH_SHORT).show()
                    } else {
                        // Limit to 10 products
                        val limitedProducts = if (products.size > 10) {
                            products.take(10)
                        } else {
                            products
                        }
                        similarProductsAdapter.submitList(limitedProducts)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading fallback products: ${e.message}", e)
                    // Don't show error toast to user, just log the error
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in loadAnyProducts: ${e.message}", e)
            // Don't show error toast to user, just log the error
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        if (item.itemId == R.id.action_share) {
            shareProductLink()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite status when activity resumes
        checkFavoriteStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.launch {
            // Cancel any ongoing coroutines
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return true
    }

    private fun shareProductLink() {
        val productId = product?.id ?: return
        val productName = product?.name ?: "Sapling Product"
        val shareText = "Check out this product: $productName\nhttps://saplingshop.com/product/$productId"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Product via"))
    }
} 