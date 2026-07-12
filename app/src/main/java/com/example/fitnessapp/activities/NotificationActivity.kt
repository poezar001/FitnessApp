package com.example.fitnessapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitnessapp.R
import com.example.fitnessapp.adapters.NotificationAdapter
import com.example.fitnessapp.databinding.ActivityNotificationBinding
import com.example.fitnessapp.models.NotificationItem
import com.example.fitnessapp.repository.MainRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var mainRepository: MainRepository
    private lateinit var notificationAdapter: NotificationAdapter
    private var notifications = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainRepository = MainRepository(this)

        setupToolbar()
        setupRecyclerView()
        loadNotificationsFromServer()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onItemClick = { notification ->
                if (!notification.isRead) {
                    markNotificationAsRead(notification.id)
                }
            },
            onDeleteClick = { notification ->
                showDeleteConfirmationDialog(notification)
            }
        )
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = notificationAdapter
    }

    private fun showDeleteConfirmationDialog(notification: NotificationItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                deleteNotification(notification.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadNotificationsFromServer() {
        val userId = mainRepository.getUserId()
        if (userId == -1) {
            showEmptyState()
            return
        }

        val url = "http://10.0.2.2/fitness_app/get_notifications.php?user_id=$userId"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        val notificationsArray = response.getJSONArray("notifications")
                        notifications.clear()

                        for (i in 0 until notificationsArray.length()) {
                            val obj = notificationsArray.getJSONObject(i)
                            val notification = NotificationItem(
                                id = obj.getInt("id"),
                                title = obj.getString("title"),
                                message = obj.getString("message"),
                                timestamp = parseDate(obj.getString("created_at")),
                                isRead = obj.getInt("is_read") == 1,
                                type = obj.getString("type")
                            )
                            notifications.add(notification)
                        }

                        updateUI()
                        // FIX: Do not auto-mark all as read instantly here, let the user click them
                        // or add a explicit "Mark all as read" button in the toolbar.
                    } else {
                        showEmptyState()
                    }
                } catch (e: Exception) {
                    showEmptyState()
                }
            },
            { showEmptyState() }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun parseDate(dateString: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun updateUI() {
        if (notifications.isNotEmpty()) {
            notificationAdapter.updateData(notifications)
            binding.emptyStateText.visibility = View.GONE
            binding.notificationsRecycler.visibility = View.VISIBLE

            // Show only the actual unread count on this screen label if desired
            val unreadCount = notifications.count { !it.isRead }
            binding.tvNotificationCount.text = "$unreadCount unread notifications"
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        binding.emptyStateText.visibility = View.VISIBLE
        binding.notificationsRecycler.visibility = View.GONE
        binding.tvNotificationCount.text = "No notifications"
    }

    private fun markNotificationAsRead(notificationId: Int) {
        val url = "http://10.0.2.2/fitness_app/mark_notification_read.php"
        val jsonBody = JSONObject().apply {
            put("notification_id", notificationId)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val index = notifications.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    notifications[index] = notifications[index].copy(isRead = true)
                    notificationAdapter.updateData(ArrayList(notifications)) // Pass fresh reference
                    updateUI()
                    sendBroadcast(Intent("REFRESH_NOTIFICATION_COUNT"))
                }
            },
            { Toast.makeText(this, "Failed to mark as read", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun deleteNotification(notificationId: Int) {
        val url = "http://10.0.2.2/fitness_app/delete_notification.php"
        val jsonBody = JSONObject().apply {
            put("notification_id", notificationId)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show()
                        notifications.removeAll { it.id == notificationId }
                        notificationAdapter.updateData(ArrayList(notifications))
                        updateUI()
                        sendBroadcast(Intent("REFRESH_NOTIFICATION_COUNT"))
                    } else {
                        Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error -> Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}