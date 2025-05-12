package com.example.saplingsales.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R

class ImagePagerAdapter : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {
    private var images: List<Bitmap> = emptyList()

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivProductImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.imageView.setImageBitmap(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun submitList(newImages: List<Bitmap>) {
        images = newImages
        notifyDataSetChanged()
    }
} 