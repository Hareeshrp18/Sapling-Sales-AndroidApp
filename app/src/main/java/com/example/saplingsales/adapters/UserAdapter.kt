package com.example.saplingsales.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.User

class UserAdapter : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvUserPhone)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvUserStatus)

        fun bind(user: User) {
            tvName.text = user.name
            tvEmail.text = user.email
            tvPhone.text = user.phone
            tvStatus.text = user.status

            // Set up email click listener
            tvEmail.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${user.email}")
                    putExtra(Intent.EXTRA_SUBJECT, "Contact from Sapling Sales")
                }
                itemView.context.startActivity(Intent.createChooser(intent, "Send Email"))
            }

            // Set up phone click listener
            tvPhone.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${user.phone}")
                }
                itemView.context.startActivity(intent)
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
} 