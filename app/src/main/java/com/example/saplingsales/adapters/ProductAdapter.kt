package com.example.saplingsales.adapters

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var context: Context? = null
    private val firestore = FirebaseFirestore.getInstance()

    fun setContext(context: Context) {
        this.context = context
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.ivProductImage)
        val productName: TextView = view.findViewById(R.id.tvProductName)
        val productDescription: TextView = view.findViewById(R.id.tvProductDescription)
        val productPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val productQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val productStatus: TextView = view.findViewById(R.id.tvStatus)
        val editButton: ImageView = view.findViewById(R.id.btnEdit)
        val deleteButton: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        
        holder.productName.text = product.name
        holder.productDescription.text = product.description
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        holder.productPrice.text = formatter.format(product.price)
        holder.productQuantity.text = "Quantity: ${product.quantity}"
        holder.productStatus.text = product.status
        holder.productStatus.visibility = if (product.status.isNotEmpty()) View.VISIBLE else View.GONE

        // Set a default image or placeholder
        holder.productImage.setImageResource(R.drawable.placeholder_image)

        // Load product image in background
        coroutineScope.launch {
            try {
                val bitmap = when {
                    !product.imageBase64.isNullOrEmpty() -> withContext(Dispatchers.IO) {
                        val imageBytes = android.util.Base64.decode(product.imageBase64, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    }
                    product.imageUrls.isNotEmpty() -> withContext(Dispatchers.IO) {
                        val url = URL(product.imageUrls[0])
                        val connection = url.openConnection()
                        connection.doInput = true
                        connection.connect()
                        val input: InputStream = connection.getInputStream()
                        BitmapFactory.decodeStream(input)
                    }
                    else -> null
                }
                
                bitmap?.let {
                    withContext(Dispatchers.Main) {
                        holder.productImage.setImageBitmap(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        holder.itemView.setOnClickListener { onProductClick(product) }
        holder.editButton.setOnClickListener { onEditClick(product) }
        holder.deleteButton.setOnClickListener { 
            context?.let { ctx ->
                AlertDialog.Builder(ctx)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes") { _, _ ->
                        onDeleteClick(product)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
} 