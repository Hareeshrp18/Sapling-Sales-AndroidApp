package com.example.saplingsales.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import java.net.URL
import kotlinx.coroutines.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import java.io.InputStream

class AdminImageAdapter(
    private val images: MutableList<String>,
    private val onRemoveClick: (Int) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<AdminImageAdapter.ImageViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.productImage)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveImage)
        val btnAdd: ImageButton = view.findViewById(R.id.btnAddImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        try {
            val imageData = images[position]
            
            // Show add button only for the last item if it's empty
            if (position == images.size - 1 && imageData.isEmpty()) {
                holder.btnAdd.visibility = View.VISIBLE
                holder.imageView.setImageResource(R.drawable.placeholder_image)
                holder.btnRemove.visibility = View.GONE
                holder.btnAdd.setOnClickListener { onAddClick() }
                return
            }

            holder.btnAdd.visibility = View.GONE
            holder.btnRemove.visibility = View.VISIBLE
            holder.btnRemove.setOnClickListener { onRemoveClick(position) }

            // Set placeholder while loading
            holder.imageView.setImageResource(R.drawable.placeholder_image)

            // Try to load as Base64
            if (imageData.startsWith("data:image") || imageData.length > 200) {
                try {
                    val base64Image = if (imageData.contains(",")) {
                        imageData.split(",")[1]
                    } else {
                        imageData
                    }
                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        holder.imageView.setImageBitmap(bitmap)
                    } else {
                        holder.imageView.setImageResource(R.drawable.placeholder_image)
                    }
                } catch (e: Exception) {
                    Log.e("AdminImageAdapter", "Error loading base64 image: ${e.message}")
                    loadImageFromUrl(imageData, holder.imageView)
                }
            } else {
                // Try loading as URL
                loadImageFromUrl(imageData, holder.imageView)
            }
        } catch (e: Exception) {
            Log.e("AdminImageAdapter", "Error binding view holder: ${e.message}")
            holder.imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()

                    val input: InputStream = connection.getInputStream()
                    BitmapFactory.decodeStream(input)
                }
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } catch (e: Exception) {
                Log.e("AdminImageAdapter", "Error loading image from URL: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    override fun getItemCount() = images.size

    fun updateImages(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.cancel()
    }
} 