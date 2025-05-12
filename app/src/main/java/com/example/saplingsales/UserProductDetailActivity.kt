package com.example.saplingsales

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.saplingsales.R
import com.example.saplingsales.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.BitmapFactory
import android.util.Base64
import android.content.Intent
import android.view.Menu
import android.view.MenuItem

class UserProductDetailActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productDescription: TextView
    private lateinit var productPrice: TextView
    private lateinit var productQuantity: TextView
    private lateinit var addToCartButton: Button
    private lateinit var progressBar: View
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserProductDetailAct"

    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_user_product_detail)
            initializeViews()
            setupToolbar()
            loadProduct()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing screen: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        productImage = findViewById(R.id.ivProductImage)
        productName = findViewById(R.id.tvProductName)
        productDescription = findViewById(R.id.tvProductDescription)
        productPrice = findViewById(R.id.tvProductPrice)
        productQuantity = findViewById(R.id.tvQuantity)
        addToCartButton = findViewById(R.id.btnAddToCart)
        progressBar = findViewById(R.id.progressBar)

        addToCartButton.setOnClickListener {
            product?.let { addToCart(it) }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun loadProduct() {
        val productId = intent.getStringExtra("productId")
        if (productId == null) {
            Toast.makeText(this, "Invalid product", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        db.collection("saplingProducts").document(productId)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    product = document.toObject(Product::class.java)?.copy(id = document.id)
                    displayProduct()
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading product: ${e.message}")
                Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayProduct() {
        product?.let { product ->
            productName.text = product.name
            productDescription.text = product.description
            productPrice.text = "₹${product.price}"
            productQuantity.text = "Available: ${product.quantity}"

            // Load product image
            if (!product.imageBase64.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(product.imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    productImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image: ${e.message}")
                    productImage.setImageResource(R.drawable.placeholder_image)
                }
            } else {
                productImage.setImageResource(R.drawable.placeholder_image)
            }

            // Enable/disable add to cart button based on quantity
            addToCartButton.isEnabled = product.quantity > 0
        }
    }

    private fun addToCart(product: Product) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to add items to cart", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        addToCartButton.isEnabled = false

        // First check if product already exists in cart
        db.collection("cartProduct")
            .whereEqualTo("userId", userId)
            .whereEqualTo("productId", product.id)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Product already exists in cart
                    Toast.makeText(this, "Product already in cart", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    addToCartButton.isEnabled = true
                    return@addOnSuccessListener
                }

                // Create new cart item
                val cartItem = hashMapOf(
                    "userId" to userId,
                    "productId" to product.id,
                    "productName" to product.name,
                    "productImage" to (product.imageBase64 ?: ""),
                    "productCategory" to product.category,
                    "price" to product.price,
                    "quantity" to product.quantity,
                    "addedAt" to System.currentTimeMillis(),
                    "status" to "pending"
                )

                // Add to cartProduct collection
                db.collection("cartProduct")
                    .add(cartItem)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        addToCartButton.isEnabled = true
                        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error adding to cart: ${e.message}")
                        progressBar.visibility = View.GONE
                        addToCartButton.isEnabled = true
                        Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking cart: ${e.message}")
                progressBar.visibility = View.GONE
                addToCartButton.isEnabled = true
                Toast.makeText(this, "Error checking cart", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_share) {
            shareProductLink()
            return true
        }
        return super.onOptionsItemSelected(item)
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
