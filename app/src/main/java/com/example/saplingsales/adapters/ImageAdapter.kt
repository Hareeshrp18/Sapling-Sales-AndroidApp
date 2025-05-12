package com.example.saplingsales.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class ImageAdapter : ListAdapter<String, ImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "ImageAdapter"

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivProductImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageData = getItem(position)
        
        // Set placeholder while loading
        holder.imageView.setImageResource(R.drawable.placeholder_image)
        
        if (imageData == "default") {
            return
        }

        Log.d(TAG, "Loading image at position $position: ${imageData.take(50)}...")

        coroutineScope.launch {
            try {
                val bitmap = when {
                    // Try to load as Base64
                    imageData.startsWith("data:image") || imageData.length > 200 -> {
                        withContext(Dispatchers.IO) {
                            try {
                                val base64Image = if (imageData.contains(",")) {
                                    imageData.split(",")[1]
                                } else {
                                    imageData
                                }
                                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding base64 image: ${e.message}", e)
                                null
                            }
                        }
                    }
                    // Try to load as URL
                    imageData.startsWith("http") -> {
                        withContext(Dispatchers.IO) {
                            try {
                                val url = URL(imageData)
                                val connection = url.openConnection()
                                connection.doInput = true
                                connection.connectTimeout = 10000
                                connection.readTimeout = 10000
                                connection.connect()
                                val input: InputStream = connection.getInputStream()
                                BitmapFactory.decodeStream(input)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading image from URL: ${e.message}", e)
                                null
                            }
                        }
                    }
                    else -> {
                        Log.e(TAG, "Unrecognized image format: ${imageData.take(50)}...")
                        null
                    }
                }
                
                bitmap?.let {
                    withContext(Dispatchers.Main) {
                        holder.imageView.setImageBitmap(it)
                        Log.d(TAG, "Successfully loaded image at position $position")
                    }
                } ?: run {
                    Log.e(TAG, "Failed to load image at position $position")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onBindViewHolder: ${e.message}", e)
            }
        }
    }

    private class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
} 