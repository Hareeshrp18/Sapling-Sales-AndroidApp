package com.example.saplingsales.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.FeedbackUser

class AdminFeedbackUserAdapter(
    private val onUserClick: (FeedbackUser) -> Unit
) : ListAdapter<FeedbackUser, AdminFeedbackUserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position), onUserClick)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvUserPhone: TextView = itemView.findViewById(R.id.tvUserPhone)
        private val tvLatestFeedback: TextView = itemView.findViewById(R.id.tvLatestFeedback)

        fun bind(user: FeedbackUser, onUserClick: (FeedbackUser) -> Unit) {
            tvUserName.text = user.userName
            tvUserEmail.text = user.userEmail
            tvUserPhone.text = user.userPhone
            tvLatestFeedback.text = user.latestFeedback
            itemView.setOnClickListener { onUserClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<FeedbackUser>() {
        override fun areItemsTheSame(oldItem: FeedbackUser, newItem: FeedbackUser): Boolean {
            return oldItem.userId == newItem.userId
        }
        override fun areContentsTheSame(oldItem: FeedbackUser, newItem: FeedbackUser): Boolean {
            return oldItem == newItem
        }
    }
} 