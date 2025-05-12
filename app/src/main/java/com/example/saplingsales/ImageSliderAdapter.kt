package com.example.saplingsales

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R

class ImageSliderAdapter : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {
    private var images = listOf<String>()

    fun setImages(newImages: List<String>) {
        try {
            images = newImages
            notifyDataSetChanged()
            Log.d("ImageSliderAdapter", "Updated with ${newImages.size} images")
        } catch (e: Exception) {
            Log.e("ImageSliderAdapter", "Error updating images: ${e.message}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_slider, parent, false)
            return ImageViewHolder(view)
        } catch (e: Exception) {
            Log.e("ImageSliderAdapter", "Error creating ViewHolder: ${e.message}")
            throw RuntimeException("Failed to create view holder: ${e.message}")
        }
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        try {
            val imageData = images.getOrNull(position)
            if (imageData != null) {
                holder.bind(imageData)
            }
        } catch (e: Exception) {
            Log.e("ImageSliderAdapter", "Error binding ViewHolder: ${e.message}")
        }
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivProduct)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(imageData: String) {
            try {
                progressBar.visibility = View.VISIBLE
                
                if (imageData == "placeholder") {
                    imageView.setImageResource(R.drawable.placeholder_image)
                    progressBar.visibility = View.GONE
                    Log.d("ImageSliderAdapter", "Set placeholder image")
                    return
                }

                // Clean the Base64 string
                val cleanBase64 = imageData.trim()
                    .replace("data:image/jpeg;base64,", "")
                    .replace("data:image/png;base64,", "")
                    .replace("data:image/jpg;base64,", "")
                    .replace("\n", "")
                    .replace(" ", "")

                if (cleanBase64.isEmpty()) {
                    Log.e("ImageSliderAdapter", "Empty Base64 string after cleaning")
                    imageView.setImageResource(R.drawable.placeholder_image)
                    progressBar.visibility = View.GONE
                    return
                }

                try {
                    val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        Log.d("ImageSliderAdapter", "Successfully loaded image, bitmap size: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("ImageSliderAdapter", "Failed to decode bitmap")
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                } catch (e: Exception) {
                    Log.e("ImageSliderAdapter", "Error decoding Base64: ${e.message}")
                    e.printStackTrace()
                    imageView.setImageResource(R.drawable.placeholder_image)
                } finally {
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("ImageSliderAdapter", "Error binding image: ${e.message}")
                e.printStackTrace()
                imageView.setImageResource(R.drawable.placeholder_image)
                progressBar.visibility = View.GONE
            }
        }
    }
} 