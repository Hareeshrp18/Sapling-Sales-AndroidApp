package com.example.saplingsales.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R

class ImageSliderAdapter : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {
    private var images: List<String> = listOf()

    fun setImages(newImages: List<String>) {
        images = newImages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        try {
            val imageString = images[position]
            if (imageString == "placeholder") {
                holder.imageView.setImageResource(R.drawable.placeholder_image)
                return
            }

            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("ImageSliderAdapter", "Error loading image: ${e.message}")
            holder.imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivProductImage)
    }
} 