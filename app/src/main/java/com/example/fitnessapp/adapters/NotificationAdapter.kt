package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemNotificationBinding
import com.example.fitnessapp.models.NotificationItem
import com.example.fitnessapp.utils.DateUtils

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem, Int) -> Unit // Added callback for deletion
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notifications = mutableListOf<NotificationItem>() // Changed to MutableList to support removal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position], position)
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<NotificationItem>) {
        notifications = newNotifications.toMutableList()
        notifyDataSetChanged()
    }

    // Call this from your activity/fragment when the network deletion succeeds
    fun removeItem(position: Int) {
        if (position in notifications.indices) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notifications.size)
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem, position: Int) {
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTime.text = DateUtils.formatTimeAgo(notification.timestamp)

            if (notification.isRead) {
                binding.viewUnreadIndicator.visibility = android.view.View.GONE
                binding.root.alpha = 0.7f
            } else {
                binding.viewUnreadIndicator.visibility = android.view.View.VISIBLE
                binding.root.alpha = 1.0f
            }

            binding.root.setOnClickListener {
                onItemClick(notification)
            }

            binding.ivDelete.setOnClickListener {
                onDeleteClick(notification, position)
            }
        }
    }
}