package com.example.saplingsales.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.adapters.UserProductAdapter
import com.example.saplingsales.models.Product
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.RangeSlider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.saplingsales.UserProfileActivity
import com.example.saplingsales.UserLoginActivity
import com.example.saplingsales.activities.CartActivity
import com.google.firebase.firestore.Query
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.saplingsales.InfoActivity
import com.google.android.material.navigation.NavigationView
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.Matrix
import com.example.saplingsales.FeedbackActivity
import com.example.saplingsales.RoleSelectionActivity
import com.example.saplingsales.models.Order
import com.example.saplingsales.models.OrderItem
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Button
import android.app.AlertDialog
import android.widget.RadioButton
import android.widget.RadioGroup

class UserScreenActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var searchEditText: EditText
    private lateinit var micButton: ImageButton
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var priceRangeSlider: RangeSlider
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var adapter: UserProductAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserScreenActivity"
    private var products = mutableListOf<Product>()
    private var filteredProducts = mutableListOf<Product>()
    private var selectedCategory: String? = null
    private var searchQuery = ""
    private var selectedPriceRange: Int = 0 // 0: All, 1: 0-50, 2: 51-100, 3: 101-200, 4: 201-300, 5: 301-400, 6: 401-500, 7: 501-600, 8: 601-700, 9: 701-800, 10: 801-900, 11: 901-1000, 12: 1000+

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )
    private val PERMISSION_REQUEST_CODE = 123
    private val VOICE_SEARCH_REQUEST_CODE = 456

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var ivMenu: ImageView
    private lateinit var userProfileImage: CircleImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_screen)

        // Initialize location client first
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize views
        initViews()
        setupNavigation()
        setupRecyclerView()
        setupBottomNavigation()
        setupSearch()
        setupCategoryFilter()
        
        // Load products first
        loadProducts()
        
        // Check permissions after everything is set up
        checkAndRequestPermissions()
    }

    private fun initViews() {
        try {
            recyclerView = findViewById(R.id.rvProducts)
            emptyView = findViewById(R.id.emptyView)
            progressBar = findViewById(R.id.progressBar)
            searchEditText = findViewById(R.id.etSearch)
            micButton = findViewById(R.id.btnMic)
            chipGroupCategories = findViewById(R.id.chipGroupCategories)
            bottomNavigationView = findViewById(R.id.bottomNavigationView)
            drawerLayout = findViewById(R.id.drawerLayout)
            navigationView = findViewById(R.id.navigationView)
            toolbar = findViewById(R.id.toolbar)
            ivMenu = findViewById(R.id.ivMenu)
            val btnFilter = findViewById<ImageButton>(R.id.btnFilter)

            val headerView = navigationView.getHeaderView(0)
            userProfileImage = headerView.findViewById(R.id.ivUserProfile)
            userNameTextView = headerView.findViewById(R.id.tvUserName)
            userEmailTextView = headerView.findViewById(R.id.tvUserEmail)

            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            btnFilter.setOnClickListener { showPriceFilterDialog() }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}")
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email from Firebase Auth
            userEmailTextView.text = currentUser.email

            // Load user profile from Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        try {
                            // Set user name
                            val userName = document.getString("name")
                            if (!userName.isNullOrEmpty()) {
                                userNameTextView.text = userName
                            }

                            // Set user email if different from auth email
                            val userEmail = document.getString("email")
                            if (!userEmail.isNullOrEmpty()) {
                                userEmailTextView.text = userEmail
                            }

                            // Load profile image - using the correct key "ImageBase64"
                            val profileImageBase64 = document.getString("ImageBase64")
                            if (!profileImageBase64.isNullOrEmpty()) {
                                try {
                                    val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    if (bitmap != null) {
                                        // Set the bitmap directly to CircleImageView
                                        userProfileImage.setImageBitmap(bitmap)
                                        Log.d(TAG, "Successfully loaded and set profile image")
                                    } else {
                                        Log.e(TAG, "Failed to decode bitmap from base64 string")
                                        userProfileImage.setImageResource(R.drawable.baseline_person_24)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing profile image: ${e.message}")
                                    e.printStackTrace()
                                    userProfileImage.setImageResource(R.drawable.baseline_person_24)
                                }
                            } else {
                                Log.d(TAG, "No profile image found in document")
                                userProfileImage.setImageResource(R.drawable.baseline_person_24)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing user data: ${e.message}")
                            e.printStackTrace()
                            Toast.makeText(this, "Error loading user profile", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d(TAG, "No user document found")
                        userNameTextView.text = "User"
                        userProfileImage.setImageResource(R.drawable.baseline_person_24)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user info: ${e.message}")
                    e.printStackTrace()
                    Toast.makeText(this, "Error loading user profile", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No user logged in
            userNameTextView.text = "Guest"
            userEmailTextView.text = ""
            userProfileImage.setImageResource(R.drawable.baseline_person_24)
        }
    }

    private fun setupNavigation() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        navigationView.setNavigationItemSelectedListener(this)
        
        ivMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // All permissions are granted, get location
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            userLocation = it
                            Log.d(TAG, "Location obtained: ${it.latitude}, ${it.longitude}")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting location: ${e.message}")
                        if (e is SecurityException) {
                            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
                            checkAndRequestPermissions()
                        }
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException when requesting location: ${e.message}")
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.isNotEmpty() && 
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                getUserLocation()
                // If we were trying to start voice search, start it now
                if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
                    startVoiceSearch()
                }
            } else {
                // Check which permissions were denied
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                        when (permission) {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                                Toast.makeText(this, "Location features will be limited", Toast.LENGTH_SHORT).show()
                            }
                            Manifest.permission.CAMERA -> {
                                Toast.makeText(this, "Camera features will be limited", Toast.LENGTH_SHORT).show()
                            }
                            Manifest.permission.RECORD_AUDIO -> {
                                Toast.makeText(this, "Voice search requires microphone permission", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                filterProducts()
            }
        })

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        micButton.setOnClickListener {
            startVoiceSearch()
        }
    }

    private fun performSearch() {
        searchQuery = searchEditText.text.toString()
        filterProducts()
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    private fun startVoiceSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search products")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice search: ${e.message}")
            Toast.makeText(this, "Voice search not available. Please check if Google app is installed.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == VOICE_SEARCH_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = results?.get(0)
                    
                    if (!spokenText.isNullOrEmpty()) {
                        searchEditText.setText(spokenText)
                        Toast.makeText(this, "Searching for: $spokenText", Toast.LENGTH_SHORT).show()
                    }
                }
                RecognizerIntent.RESULT_AUDIO_ERROR -> {
                    Toast.makeText(this, "Audio recording error", Toast.LENGTH_SHORT).show()
                }
                RecognizerIntent.RESULT_CLIENT_ERROR -> {
                    Toast.makeText(this, "Client error", Toast.LENGTH_SHORT).show()
                }
                RecognizerIntent.RESULT_NETWORK_ERROR -> {
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                }
                RecognizerIntent.RESULT_NO_MATCH -> {
                    Toast.makeText(this, "No speech recognized", Toast.LENGTH_SHORT).show()
                }
                RecognizerIntent.RESULT_SERVER_ERROR -> {
                    Toast.makeText(this, "Server error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCategoryFilter() {
        try {
            // Add "All" category chip
            addCategoryChip("All")
            
            // Fetch unique categories from products
            firestore.collection("saplingProducts")
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        val categories = documents
                            .mapNotNull { it.getString("category") }
                            .distinct()
                            .sorted()
                        
                        categories.forEach { category ->
                            addCategoryChip(category)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing categories: ${e.message}")
                    }
                    }
                    .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading categories: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up category filter: ${e.message}")
        }
    }

    private fun addCategoryChip(category: String) {
        try {
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategory = if (category == "All") null else category
                        filterProducts()
                    }
                }
            }
            chipGroupCategories.addView(chip)
            
            // Set "All" as default selected
            if (category == "All") {
                chip.isChecked = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding category chip: ${e.message}")
        }
    }

    private fun showPriceFilterDialog() {
        val priceRanges = arrayOf(
            "All",
            "0-50",
            "51-100",
            "101-200",
            "201-300",
            "301-400",
            "401-500",
            "501-600",
            "601-700",
            "701-800",
            "801-900",
            "901-1000",
            "1000+"
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Price Range")
        builder.setSingleChoiceItems(priceRanges, selectedPriceRange) { dialog, which ->
            selectedPriceRange = which
        }
        builder.setPositiveButton("Apply") { dialog, _ ->
            filterProducts()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun filterProducts() {
        try {
            filteredProducts.clear()
            filteredProducts.addAll(products.filter { product ->
                val matchesSearch = product.name.contains(searchQuery, ignoreCase = true)
                val matchesCategory = selectedCategory == null || product.category == selectedCategory
                val matchesPrice = when (selectedPriceRange) {
                    1 -> product.price in 0.0..50.0
                    2 -> product.price in 51.0..100.0
                    3 -> product.price in 101.0..200.0
                    4 -> product.price in 201.0..300.0
                    5 -> product.price in 301.0..400.0
                    6 -> product.price in 401.0..500.0
                    7 -> product.price in 501.0..600.0
                    8 -> product.price in 601.0..700.0
                    9 -> product.price in 701.0..800.0
                    10 -> product.price in 801.0..900.0
                    11 -> product.price in 901.0..1000.0
                    12 -> product.price > 1000.0
                    else -> true
                }
                matchesSearch && matchesCategory && matchesPrice
            })
            adapter.submitList(filteredProducts.toList())
            if (filteredProducts.isEmpty()) {
                showEmptyView()
            } else {
                showProducts()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering products: ${e.message}")
            showError("Error updating products")
        }
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.selectedItemId = R.id.nav_home
            
            bottomNavigationView.setOnItemSelectedListener { item ->
                try {
                    when (item.itemId) {
                        R.id.nav_home -> true
                        R.id.nav_favorites -> {
                            if (auth.currentUser == null) {
                                startActivity(Intent(this, UserLoginActivity::class.java))
                            } else {
                                val intent = Intent(this, FavoritesActivity::class.java)
                                startActivity(intent)
                            }
                            true
                        }
                        R.id.nav_cart -> {
                            if (auth.currentUser == null) {
                                startActivity(Intent(this, UserLoginActivity::class.java))
                            } else {
                                startActivity(Intent(this, CartActivity::class.java))
                            }
                            true
                        }
                        R.id.nav_profile -> {
                            if (auth.currentUser == null) {
                                startActivity(Intent(this, UserLoginActivity::class.java))
                            } else {
                                startActivity(Intent(this, UserProfileActivity::class.java))
                            }
                            true
                        }
                        R.id.nav_feedback -> {
                            startActivity(Intent(this, FeedbackActivity::class.java))
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in bottom navigation: ${e.message}", e)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            adapter = UserProductAdapter(
                onProductClick = { product ->
                    try {
                        val intent = Intent(this, UserViewProductActivity::class.java).apply {
                            putExtra("productId", product.id)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to product details: ${e.message}")
                        Toast.makeText(this, "Error opening product details", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddToCartClick = { product ->
                    if (auth.currentUser == null) {
                        Toast.makeText(this, "Please log in to add items to cart", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, UserLoginActivity::class.java))
                        return@UserProductAdapter
                    }
                    addToCart(product)
                }
            )
            
            recyclerView.layoutManager = GridLayoutManager(this, 2)
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}")
            throw e
        }
    }

    private fun loadProducts() {
        try {
            showLoading()
            
        firestore.collection("saplingProducts")
            .get()
            .addOnSuccessListener { documents ->
                    try {
                products.clear()
                for (document in documents) {
                            try {
                                val product = document.toObject(Product::class.java).copy(id = document.id)
                                products.add(product)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing product: ${e.message}")
                            }
                        }
                        filterProducts()
                        hideLoading()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing products: ${e.message}")
                        showError("Error loading products")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading products: ${e.message}")
                    showError("Error loading products")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadProducts: ${e.message}")
            showError("Error loading products")
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showEmptyView() {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }

    private fun showProducts() {
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
    }

    private fun showError(message: String) {
        hideLoading()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        showEmptyView()
    }

    private fun addToCart(product: Product) {
        try {
            firestore.collection("saplingProducts")
                .document(product.id)
                .get()
                .addOnSuccessListener { productDoc ->
                    try {
                        if (!productDoc.exists()) {
                            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val currentQuantity = productDoc.getLong("quantity")?.toInt() ?: 0
                        
                        if (currentQuantity <= 0) {
                            Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        checkAndUpdateCart(product, currentQuantity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing product document: ${e.message}")
                        Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking product: ${e.message}")
                    Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in addToCart: ${e.message}")
            Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndUpdateCart(product: Product, availableQuantity: Int) {
        try {
            firestore.collection("cartProduct")
                .whereEqualTo("userId", auth.currentUser?.uid)
                .whereEqualTo("productId", product.id)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        if (!documents.isEmpty) {
                            val cartDoc = documents.documents[0]
                            val currentCartQuantity = cartDoc.getLong("quantity")?.toInt() ?: 0
                            
                            if (currentCartQuantity < availableQuantity) {
                                updateCartQuantity(cartDoc.reference, currentCartQuantity, availableQuantity)
                            } else {
                                Toast.makeText(this, "Maximum available quantity reached", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            createNewCartItem(product, availableQuantity)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing cart document: ${e.message}")
                        Toast.makeText(this, "Error updating cart", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking cart: ${e.message}")
                    Toast.makeText(this, "Error checking cart", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndUpdateCart: ${e.message}")
            Toast.makeText(this, "Error updating cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCartQuantity(docRef: com.google.firebase.firestore.DocumentReference, currentQuantity: Int, availableQuantity: Int) {
        docRef.update(
            mapOf(
                "quantity" to (currentQuantity + 1),
                "availableQuantity" to availableQuantity
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Cart updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error updating cart quantity: ${e.message}")
            Toast.makeText(this, "Error updating cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewCartItem(product: Product, availableQuantity: Int) {
        try {
            val cartItem = hashMapOf(
                "userId" to auth.currentUser?.uid,
                "productId" to product.id,
                "productName" to product.name,
                "productImage" to (product.images.firstOrNull() ?: ""),
                "productImageUrl" to "",
                "productCategory" to product.category,
                "price" to product.price,
                "quantity" to 1,
                "availableQuantity" to availableQuantity,
                "addedAt" to System.currentTimeMillis(),
                "status" to "pending"
            )

            firestore.collection("cartProduct")
                .add(cartItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                    Log.e(TAG, "Error creating cart item: ${e.message}")
                    Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createNewCartItem: ${e.message}")
            Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Reload user info when activity resumes
        loadUserInfo()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already in home, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, UserProfileActivity::class.java))
            }
            R.id.nav_orders -> {
                startActivity(Intent(this, UserOrdersActivity::class.java))
            }
            R.id.nav_info -> {
                startActivity(Intent(this, InfoActivity::class.java))
            }
            R.id.nav_feedback -> {
                startActivity(Intent(this, FeedbackActivity::class.java))
            }
            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, UserLoginActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
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