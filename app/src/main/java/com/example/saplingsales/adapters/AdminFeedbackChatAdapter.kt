package com.example.saplingsales.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saplingsales.R
import com.example.saplingsales.models.Feedback
import java.text.SimpleDateFormat
import java.util.*

class AdminFeedbackChatAdapter : ListAdapter<Feedback, RecyclerView.ViewHolder>(FeedbackDiffCallback()) {
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).type == "admin_reply") VIEW_TYPE_ADMIN else VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ADMIN) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_admin, parent, false)
            AdminViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val feedback = getItem(position)
        if (holder is AdminViewHolder) {
            holder.bind(feedback)
        } else if (holder is UserViewHolder) {
            holder.bind(feedback)
        }
    }

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        fun bind(feedback: Feedback) {
            tvMessage.text = feedback.message
            tvTime.text = formatTime(feedback.timestamp)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        fun bind(feedback: Feedback) {
            tvMessage.text = feedback.message
            tvTime.text = formatTime(feedback.timestamp)
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ADMIN = 1
        fun formatTime(timestamp: com.google.firebase.Timestamp): String {
            val date = timestamp.toDate()
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(date)
        }
    }

    class FeedbackDiffCallback : DiffUtil.ItemCallback<Feedback>() {
        override fun areItemsTheSame(oldItem: Feedback, newItem: Feedback): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Feedback, newItem: Feedback): Boolean {
            return oldItem == newItem
        }
    }
} 