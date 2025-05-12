package com.example.saplingsales.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.adapters.UserProductAdapter
import com.example.saplingsales.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import com.example.saplingsales.UserLoginActivity
import android.util.Log
import com.google.android.material.button.MaterialButton

class FavoritesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: UserProductAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rvFavorites)
        emptyView = findViewById(R.id.emptyView)
        toolbar = findViewById(R.id.toolbar)

        // Setup explore button click
        findViewById<MaterialButton>(R.id.btnExplore).setOnClickListener {
            finish() // Go back to the main screen
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Favorites"
        }
    }

    private fun setupRecyclerView() {
        adapter = UserProductAdapter(
            onProductClick = { product ->
                val intent = Intent(this, UserViewProductActivity::class.java).apply {
                    putExtra("productId", product.id)
                }
                startActivity(intent)
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
    }

    private fun loadFavorites() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showEmptyView("Please login to view favorites")
            return
        }

        // Show loading state
        showLoading(true)
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        firestore.collection("favouriteProduct")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FavoritesActivity", "Error loading favorites: ${error.message}")
                    showLoading(false)
                    showEmptyView("Error loading favorites. Please try again.")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    showLoading(false)
                    showEmptyView("No favorites yet")
                    return@addSnapshotListener
                }

                val favoriteProducts = mutableListOf<Product>()
                var processedDocs = 0
                val totalDocs = snapshot.size()

                for (doc in snapshot.documents) {
                    try {
                        val productId = doc.getString("productId")
                        if (productId == null) {
                            processedDocs++
                            checkAndShowProducts(processedDocs, totalDocs, favoriteProducts)
                            continue
                        }

                        // Create a basic product from favorite data
                        val basicProduct = Product(
                            id = productId,
                            name = doc.getString("productName") ?: "",
                            price = doc.getDouble("productPrice") ?: 0.0,
                            description = doc.getString("productDescription") ?: "",
                            category = doc.getString("productCategory") ?: "",
                            imageBase64 = doc.getString("productImage") ?: "",
                            imageUrls = doc.getString("productImageUrl")?.let { listOf(it) } ?: emptyList(),
                            images = emptyList(),
                            quantity = 1,
                            status = "Available"
                        )
                        favoriteProducts.add(basicProduct)
                        
                        // Get the full product details from saplingProducts collection
                        firestore.collection("saplingProducts")
                            .document(productId)
                            .get()
                            .addOnSuccessListener { productDoc ->
                                try {
                                    if (productDoc.exists()) {
                                        val product = productDoc.toObject(Product::class.java)
                                        if (product != null) {
                                            // Update the product in our list with full details
                                            val index = favoriteProducts.indexOfFirst { it.id == productId }
                                            if (index != -1) {
                                                favoriteProducts[index] = product.copy(
                                                    id = productId,
                                                    name = product.name ?: basicProduct.name,
                                                    price = product.price ?: basicProduct.price,
                                                    description = product.description ?: basicProduct.description,
                                                    category = product.category ?: basicProduct.category,
                                                    imageBase64 = product.imageBase64 ?: basicProduct.imageBase64,
                                                    imageUrls = product.imageUrls.ifEmpty { basicProduct.imageUrls }
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("FavoritesActivity", "Error processing product: ${e.message}")
                                } finally {
                                    processedDocs++
                                    checkAndShowProducts(processedDocs, totalDocs, favoriteProducts)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("FavoritesActivity", "Error loading product $productId: ${e.message}")
                                processedDocs++
                                checkAndShowProducts(processedDocs, totalDocs, favoriteProducts)
                            }
                    } catch (e: Exception) {
                        Log.e("FavoritesActivity", "Error processing favorite document: ${e.message}")
                        processedDocs++
                        checkAndShowProducts(processedDocs, totalDocs, favoriteProducts)
                    }
                }
            }
    }

    private fun checkAndShowProducts(processed: Int, total: Int, products: List<Product>) {
        if (processed >= total) {
            showLoading(false)
            if (products.isEmpty()) {
                showEmptyView("No favorites available")
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                adapter.submitList(products.sortedByDescending { it.price })
            }
        }
    }

    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.progressBar)?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(message: String) {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        findViewById<android.widget.TextView>(R.id.tvEmptyMessage).text = message
    }

    private fun addToCart(product: Product) {
        firestore.collection("saplingProducts")
            .document(product.id)
            .get()
            .addOnSuccessListener { productDoc ->
                if (!productDoc.exists()) {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val currentQuantity = productDoc.getLong("quantity")?.toInt() ?: 0
                
                if (currentQuantity <= 0) {
                    Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Check if product already in cart
                firestore.collection("cartProduct")
                    .whereEqualTo("userId", auth.currentUser?.uid)
                    .whereEqualTo("productId", product.id)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val cartDoc = documents.documents[0]
                            val currentCartQuantity = cartDoc.getLong("quantity")?.toInt() ?: 0
                            
                            if (currentCartQuantity < currentQuantity) {
                                cartDoc.reference.update(
                                    mapOf(
                                        "quantity" to (currentCartQuantity + 1),
                                        "availableQuantity" to currentQuantity
                                    )
                                ).addOnSuccessListener {
                                    Toast.makeText(this, "Cart updated", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "Maximum available quantity reached", Toast.LENGTH_SHORT).show()
                            }
                            return@addOnSuccessListener
                        }

                        // Add new cart item
                        val cartItem = hashMapOf(
                            "userId" to auth.currentUser?.uid,
                            "productId" to product.id,
                            "productName" to product.name,
                            "productImage" to (product.images.firstOrNull() ?: ""),
                            "productImageUrl" to "",
                            "productCategory" to product.category,
                            "price" to product.price,
                            "quantity" to 1,
                            "availableQuantity" to currentQuantity,
                            "addedAt" to System.currentTimeMillis(),
                            "status" to "pending"
                        )

                        firestore.collection("cartProduct")
                            .add(cartItem)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Reload favorites when activity resumes
        loadFavorites()
    }
} 