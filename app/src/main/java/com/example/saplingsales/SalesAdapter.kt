package com.example.saplingsales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Base64
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream

class SalesAdapter(private val salesList: List<Sale>) :
    RecyclerView.Adapter<SalesAdapter.SaleViewHolder>() {

    class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val productName: TextView = itemView.findViewById(R.id.tvProductName)
        val quantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val totalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        val userName: TextView = itemView.findViewById(R.id.tvUserName)
        val userMobile: TextView = itemView.findViewById(R.id.tvUserMobile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val sale = salesList[position]

        holder.productName.text = sale.productName
        holder.quantity.text = "Qty: ${sale.quantity}"
        holder.totalPrice.text = "Total: $${sale.totalPrice}"
        holder.userName.text = "User: ${sale.userName}"
        holder.userMobile.text = "Mobile: ${sale.userMobile}"

        // Decode Base64 image (if applicable)
        if (sale.productImage.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(sale.productImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(decodedBytes))
                holder.productImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = salesList.size
}
