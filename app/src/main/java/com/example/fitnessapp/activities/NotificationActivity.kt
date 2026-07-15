package com.example.fitnessapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitnessapp.R
import com.example.fitnessapp.adapters.NotificationAdapter
import com.example.fitnessapp.databinding.ActivityNotificationBinding
import com.example.fitnessapp.models.NotificationItem
import com.example.fitnessapp.repository.MainRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
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
        supportActionBar?.title = "Notifications"

        lifecycleScope.launch {
            try {
                val count = mainRepository.getUnreadNotificationCount()
                if (count > 0) {
                    supportActionBar?.title = "Notifications ($count)"
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            if (!notification.isRead) {
                markNotificationAsRead(notification.id)
            }
        }
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = notificationAdapter
    }

    private fun loadNotificationsFromServer() {
        val userId = mainRepository.getUserId()
        android.util.Log.d("NotificationActivity", "🔍 User ID: $userId")

        if (userId == -1) {
            android.util.Log.e("NotificationActivity", "❌ User ID not found")
            showEmptyState()
            return
        }

        val url = "http://10.0.2.2/fitness_app/get_notifications.php?user_id=$userId"
        android.util.Log.d("NotificationActivity", "📊 Loading from: $url")

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    android.util.Log.d("NotificationActivity", "📊 Raw Response: $response")

                    val success = response.getBoolean("success")
                    android.util.Log.d("NotificationActivity", "📊 Success: $success")

                    if (success) {
                        // Check if notifications array exists
                        if (!response.has("notifications")) {
                            android.util.Log.e("NotificationActivity", "❌ No 'notifications' field in response")
                            showEmptyState()
                            return@JsonObjectRequest
                        }

                        val notificationsArray = response.getJSONArray("notifications")
                        android.util.Log.d("NotificationActivity", "📊 Found ${notificationsArray.length()} notifications")

                        notifications.clear()

                        for (i in 0 until notificationsArray.length()) {
                            try {
                                val obj = notificationsArray.getJSONObject(i)
                                android.util.Log.d("NotificationActivity", "📊 Notification $i: $obj")

                                val notification = NotificationItem(
                                    id = obj.getInt("id"),
                                    title = obj.getString("title"),
                                    message = obj.getString("message"),
                                    timestamp = parseDate(obj.getString("created_at")),
                                    isRead = obj.getInt("is_read") == 1,
                                    type = obj.getString("type")
                                )
                                notifications.add(notification)
                                android.util.Log.d("NotificationActivity", "✅ Added: ${notification.title}")
                            } catch (e: Exception) {
                                android.util.Log.e("NotificationActivity", "❌ Error parsing notification $i: ${e.message}")
                                e.printStackTrace()
                            }
                        }

                        android.util.Log.d("NotificationActivity", "📊 Total notifications: ${notifications.size}")
                        updateUI()
                    } else {
                        val message = response.optString("message", "Unknown error")
                        android.util.Log.e("NotificationActivity", "❌ API returned false: $message")
                        showEmptyState()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationActivity", "❌ JSON Parsing error: ${e.message}")
                    e.printStackTrace()
                    showEmptyState()
                }
            },
            { error ->
                android.util.Log.e("NotificationActivity", "❌ Network error: ${error.message}")
                error.printStackTrace()
                showEmptyState()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun parseDate(dateString: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            android.util.Log.e("NotificationActivity", "❌ Date parse error: ${e.message}")
            Date()
        }
    }

    private fun updateUI() {
        if (notifications.isNotEmpty()) {
            android.util.Log.d("NotificationActivity", "📊 Updating UI with ${notifications.size} notifications")
            android.util.Log.d("NotificationActivity", "📊 First notification: ${notifications[0].title}")

            // Update adapter
            notificationAdapter.updateData(notifications)

            // ============ FIX: Hide the "0 notifications" text ============
            binding.tvNotificationCount.visibility = View.GONE

            // Show RecyclerView, hide empty state
            binding.emptyStateText.visibility = View.GONE
            binding.notificationsRecycler.visibility = View.VISIBLE

            // Force refresh
            binding.notificationsRecycler.post {
                notificationAdapter.notifyDataSetChanged()
                android.util.Log.d("NotificationActivity", "📊 Adapter item count after post: ${notificationAdapter.itemCount}")
                android.util.Log.d("NotificationActivity", "📊 RecyclerView visibility: ${binding.notificationsRecycler.visibility}")
            }

            android.util.Log.d("NotificationActivity", "📊 Adapter item count: ${notificationAdapter.itemCount}")
        } else {
            android.util.Log.d("NotificationActivity", "📊 No notifications to show")
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        // Show the count as "0 notifications" and hide RecyclerView
        binding.tvNotificationCount.visibility = View.VISIBLE
        binding.tvNotificationCount.text = "0 notifications"
        binding.emptyStateText.visibility = View.VISIBLE
        binding.notificationsRecycler.visibility = View.GONE
    }

    private fun markNotificationAsRead(notificationId: Int) {
        val url = "http://10.0.2.2/fitness_app/mark_notification_read.php"
        val jsonBody = JSONObject().apply {
            put("notification_id", notificationId)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                android.util.Log.d("NotificationActivity", "✅ Marked as read: $response")
                loadNotificationsFromServer()
                sendBroadcast(Intent("REFRESH_NOTIFICATION_BADGE"))
            },
            { error ->
                android.util.Log.e("NotificationActivity", "❌ Failed to mark as read: ${error.message}")
                Toast.makeText(this, "Failed to mark as read", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        sendBroadcast(Intent("REFRESH_NOTIFICATION_BADGE"))
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadNotificationsFromServer()
        sendBroadcast(Intent("REFRESH_NOTIFICATION_BADGE"))
        android.util.Log.d("NotificationActivity", "📤 Broadcast sent to refresh badge")
    }

    override fun onDestroy() {
        super.onDestroy()
        sendBroadcast(Intent("REFRESH_NOTIFICATION_BADGE"))
        android.util.Log.d("NotificationActivity", "📤 Broadcast sent to refresh badge on destroy")
    }
}