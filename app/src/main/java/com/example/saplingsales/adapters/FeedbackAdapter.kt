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
import java.util.Locale
import com.google.firebase.Timestamp
import java.util.Date

class FeedbackAdapter : ListAdapter<Feedback, RecyclerView.ViewHolder>(FeedbackDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ADMIN = 2
    }

    override fun getItemViewType(position: Int): Int {
        val feedback = getItem(position)
        return if (feedback.sender == "admin") VIEW_TYPE_ADMIN else VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_feedback, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_ADMIN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_feedback_admin, parent, false)
                AdminMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val feedback = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(feedback)
            is AdminMessageViewHolder -> holder.bind(feedback)
        }
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvMessage)
        private val timestampText: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(feedback: Feedback) {
            try {
                messageText.text = feedback.message
                // Convert Timestamp to Date before formatting
                val date = Date(feedback.timestamp.seconds * 1000)
                timestampText.text = dateFormat.format(date)
            } catch (e: Exception) {
                timestampText.text = "Invalid date"
            }
        }
    }

    inner class AdminMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvMessage)
        private val timestampText: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(feedback: Feedback) {
            try {
                messageText.text = feedback.message
                // Convert Timestamp to Date before formatting
                val date = Date(feedback.timestamp.seconds * 1000)
                timestampText.text = dateFormat.format(date)
            } catch (e: Exception) {
                timestampText.text = "Invalid date"
            }
        }
    }

    private class FeedbackDiffCallback : DiffUtil.ItemCallback<Feedback>() {
        override fun areItemsTheSame(oldItem: Feedback, newItem: Feedback): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Feedback, newItem: Feedback): Boolean {
            return oldItem == newItem
        }
    }
} 