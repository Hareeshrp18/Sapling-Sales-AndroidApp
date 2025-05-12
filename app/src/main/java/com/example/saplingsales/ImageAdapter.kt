package com.example.saplingsales

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R

class ImageAdapter(private var images: List<String>, private val onImageClick: (String) -> Unit) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivProductImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageData = images[position]
        try {
            // Try to decode base64 image
            val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap)
            } else {
                holder.imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            holder.imageView.setImageResource(R.drawable.placeholder_image)
        }
        holder.imageView.setOnClickListener { onImageClick(imageData) }
    }

    override fun getItemCount(): Int = images.size

    fun submitList(newImages: List<String>) {
        images = newImages
        notifyDataSetChanged()
    }

    fun getImages(): List<String> = images
}
