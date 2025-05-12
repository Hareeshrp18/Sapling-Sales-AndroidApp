package com.example.saplingsales.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R

class EditProductImagesAdapter : RecyclerView.Adapter<EditProductImagesAdapter.ImageViewHolder>() {
    private val images = mutableListOf<Bitmap>()
    private var onDeleteClickListener: ((Int) -> Unit)? = null

    fun setOnDeleteClickListener(listener: (Int) -> Unit) {
        onDeleteClickListener = listener
    }

    fun addImage(bitmap: Bitmap) {
        images.add(bitmap)
        notifyItemInserted(images.size - 1)
    }

    fun removeImage(position: Int) {
        if (position in 0 until images.size) {
            images.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, images.size)
        }
    }

    fun getImages(): List<Bitmap> = images

    fun clearImages() {
        images.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_product_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val bitmap = images[position]
        holder.bind(bitmap)
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener?.invoke(position)
        }
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)

        fun bind(bitmap: Bitmap) {
            imageView.setImageBitmap(bitmap)
        }
    }
} 