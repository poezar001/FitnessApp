package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemNotificationBinding
import com.example.fitnessapp.models.NotificationItem
import com.example.fitnessapp.utils.DateUtils

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit
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
        this.notifications = newNotifications
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem) {
            binding.apply {
                tvTitle.text = notification.title
                tvMessage.text = notification.message
                tvTime.text = DateUtils.formatTimeAgo(notification.timestamp)

                if (notification.isRead) {
                    viewUnreadIndicator.visibility = android.view.View.GONE
                    root.alpha = 0.6f
                } else {
                    viewUnreadIndicator.visibility = android.view.View.VISIBLE
                    root.alpha = 1.0f
                }

                root.setOnClickListener {
                    onItemClick(notification)
                }

                ivDelete.setOnClickListener {
                    onDeleteClick(notification)
                }
            }
        }
    }
}