package com.example.saplingsales.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.saplingsales.EditProductActivity
import com.example.saplingsales.R
import com.example.saplingsales.adapters.ProductAdapter
import com.example.saplingsales.adapters.ProductImageAdapter
import com.example.saplingsales.models.Product
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import android.speech.RecognizerIntent
import android.app.Activity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.ArrayAdapter
import java.util.Locale
import androidx.core.content.ContextCompat

class AdminViewProductActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var adapter: ProductAdapter
    private lateinit var db: FirebaseFirestore
    private var snapshotListener: ListenerRegistration? = null
    private var lastVisible: DocumentSnapshot? = null
    private val allProducts = mutableListOf<Product>()
    private var isLoading = false
    private var hasMoreProducts = true
    private lateinit var etSearch: EditText
    private lateinit var btnMic: ImageButton
    private lateinit var btnFilter: ImageButton
    private var selectedCategory: String? = null
    private var availableCategories: List<String> = listOf()
    private var selectedSortIndex = 0
    private val VOICE_SEARCH_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_view_product)

        try {
            // Initialize Firestore
            db = FirebaseFirestore.getInstance()

            // Initialize views
            toolbar = findViewById(R.id.toolbar)
            recyclerView = findViewById(R.id.recyclerView)
            progressBar = findViewById(R.id.progressBar)
            etSearch = findViewById(R.id.etSearch)
            btnMic = findViewById(R.id.btnMic)
            btnFilter = findViewById(R.id.btnFilter)

            // Setup toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "View Products"

            // Programmatically set navigation icon color
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.forestGreen))

            // Setup RecyclerView
            setupRecyclerView()

            // Setup search and sort
            setupSearchAndSort()

            // Load initial products
            loadProducts()
            fetchCategories()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            showError("Failed to initialize the view")
        }
    }

    private fun setupRecyclerView() {
        try {
            adapter = ProductAdapter(
                emptyList(),
                onProductClick = { product ->
                    showProductDetailsDialog(product)
                },
                onEditClick = { product ->
                    val intent = Intent(this, EditProductActivity::class.java).apply {
                        putExtra("productId", product.id)
                    }
                    startActivity(intent)
                },
                onDeleteClick = { product ->
                    showDeleteConfirmationDialog(product)
                }
            )

            adapter.setContext(this)

            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@AdminViewProductActivity)
                adapter = this@AdminViewProductActivity.adapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        if (!isLoading && hasMoreProducts) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                            ) {
                                loadMoreProducts()
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}")
            showError("Failed to set up the product list")
        }
    }

    private fun loadProducts() {
        try {
            isLoading = true
            showLoading(true)

            db.collection("saplingProducts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        allProducts.clear()
                        for (document in documents) {
                            try {
                                val product = document.toObject(Product::class.java).apply {
                                    id = document.id
                                }
                                allProducts.add(product)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing product: ${e.message}")
                            }
                        }

                        if (documents.size() < PAGE_SIZE) {
                            hasMoreProducts = false
                        }

                        lastVisible = documents.lastOrNull()

                        try {
                            adapter.updateProducts(allProducts.toList())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating adapter: ${e.message}")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading products: ${e.message}")
                        showError("Failed to load products")
                    } finally {
                        showLoading(false)
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error in loadProducts: ${e.message}")
                    showError("Failed to load products")
                    showLoading(false)
                    isLoading = false
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadProducts: ${e.message}")
            showError("Failed to load products")
            showLoading(false)
            isLoading = false
        }
    }

    private fun loadMoreProducts() {
        if (isLoading || !hasMoreProducts || lastVisible == null) return

        try {
            isLoading = true
            showLoading(true)

            db.collection("saplingProducts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastVisible!!)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        for (document in documents) {
                            try {
                                val product = document.toObject(Product::class.java).apply {
                                    id = document.id
                                }
                                allProducts.add(product)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing additional product: ${e.message}")
                            }
                        }

                        if (documents.size() < PAGE_SIZE) {
                            hasMoreProducts = false
                        }

                        lastVisible = documents.lastOrNull()
                        adapter.updateProducts(allProducts.toList())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading more products: ${e.message}")
                        showError("Failed to load more products")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading more products: ${e.message}")
                    showError("Failed to load more products")
                }
                .addOnCompleteListener {
                    showLoading(false)
                    isLoading = false
                }

            Log.d(TAG, "Updated UI with ${allProducts.size} products")
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadMoreProducts: ${e.message}")
            showError("Failed to load more products")
            showLoading(false)
            isLoading = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_admin_view_product, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating menu: ${e.message}")
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            return when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                R.id.action_refresh -> {
                    loadProducts()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling menu item: ${e.message}")
            return false
        }
    }

    private fun setupProductListener() {
        try {
            snapshotListener = db.collection("saplingProducts")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val products = snapshot.documents.mapNotNull { document ->
                                try {
                                    document.toObject(Product::class.java)?.apply {
                                        id = document.id
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                                    null
                                }
                            }
                            adapter.updateProducts(products)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing snapshot: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listener: ${e.message}")
        }
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteProduct(product)
                }
                .setNegativeButton("No", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alert: ${e.message}")
            showError("Failed to show delete confirmation")
        }
    }

    private fun showProductDetailsDialog(product: Product) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product_details, null)
            
            // Initialize views
            val viewPagerImages = dialogView.findViewById<ViewPager2>(R.id.viewPagerImages)
            val tvProductName = dialogView.findViewById<TextView>(R.id.tvProductName)
            val tvProductDescription = dialogView.findViewById<TextView>(R.id.tvProductDescription)
            val tvProductPrice = dialogView.findViewById<TextView>(R.id.tvProductPrice)
            val tvProductQuantity = dialogView.findViewById<TextView>(R.id.tvProductQuantity)
            val tvProductCategory = dialogView.findViewById<TextView>(R.id.tvProductCategory)
            val tvProductStatus = dialogView.findViewById<TextView>(R.id.tvProductStatus)

            // Set product details
            tvProductName.text = product.name
            tvProductDescription.text = product.description
            tvProductPrice.text = "Price: ₹${product.price}"
            tvProductQuantity.text = "Quantity: ${product.quantity}"
            tvProductCategory.text = "Category: ${product.category}"
            tvProductStatus.text = "Status: ${product.status}"

            // Setup ViewPager2 for images
            if (product.images.isNotEmpty()) {
                val imageAdapter = ProductImageAdapter(product.images)
                viewPagerImages.adapter = imageAdapter
            } else {
                viewPagerImages.visibility = View.GONE
            }

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing product details: ${e.message}")
            showError("Failed to show product details")
        }
    }

    override fun onDestroy() {
        try {
            snapshotListener?.remove()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun deleteProduct(product: Product) {
        db.collection("saplingProducts").document(product.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearchAndSort() {
        // Voice search
        btnMic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak product name...")
            try {
                startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(this, "Your device does not support speech input", Toast.LENGTH_SHORT).show()
            }
        }
        // Text search
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                filterAndSortProducts()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // Filter icon
        btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        if (availableCategories.isEmpty()) {
            fetchCategories()
            Toast.makeText(this, "Loading categories, please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        val sortOptions = arrayOf("Price: Low to High", "Price: High to Low")
        var tempSelectedCategory = selectedCategory ?: "All"
        var tempSortIndex = selectedSortIndex
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter_sort, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val sortSpinner = dialogView.findViewById<Spinner>(R.id.spinnerSort)
        // Category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableCategories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        categorySpinner.setSelection(availableCategories.indexOf(tempSelectedCategory))
        // Sort spinner
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = sortAdapter
        sortSpinner.setSelection(tempSortIndex)
        AlertDialog.Builder(this)
            .setTitle("Filter & Sort")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                selectedCategory = categorySpinner.selectedItem as String
                selectedSortIndex = sortSpinner.selectedItemPosition
                filterAndSortProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterAndSortProducts() {
        val query = etSearch.text.toString().trim().lowercase(Locale.getDefault())
        var filtered = allProducts.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
        if (selectedCategory != null && selectedCategory != "All") {
            filtered = filtered.filter { it.category == selectedCategory }
        }
        when (selectedSortIndex) {
            0 -> filtered = filtered.sortedBy { it.price }
            1 -> filtered = filtered.sortedByDescending { it.price }
        }
        adapter.updateProducts(filtered)
    }

    private fun fetchCategories() {
        db.collection("saplingProducts")
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.mapNotNull { it.getString("category") }
                    .distinct()
                    .sorted()
                val mutableCategories = categories.toMutableList()
                if (!mutableCategories.contains("Tree Seed")) {
                    mutableCategories.add("Tree Seed")
                }
                availableCategories = listOf("All") + mutableCategories.sorted()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                etSearch.setText(result[0])
            }
        }
    }

    companion object {
        private const val TAG = "AdminViewProductActivity"
        private const val PAGE_SIZE = 10L
    }
} 