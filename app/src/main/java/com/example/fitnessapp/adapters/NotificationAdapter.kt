package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemNotificationBinding
import com.example.fitnessapp.models.NotificationItem
import com.example.fitnessapp.utils.DateUtils

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notifications = listOf<NotificationItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<NotificationItem>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem) {
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
        }
    }
}