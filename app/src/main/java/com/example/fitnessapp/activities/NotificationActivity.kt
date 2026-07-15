package com.example.fitnessapp.activities

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
import com.example.fitnessapp.utils.NetworkUtils
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
        notificationAdapter = NotificationAdapter { notification ->
            // Mark as read when clicked
            if (!notification.isRead) {
                markNotificationAsRead(notification.id)
            }
        }
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = notificationAdapter
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
                    } else {
                        showEmptyState()
                    }
                } catch (e: Exception) {
                    showEmptyState()
                }
            },
            { error ->
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
            Date()
        }
    }

    private fun updateUI() {
        if (notifications.isNotEmpty()) {
            notificationAdapter.updateData(notifications)
            binding.emptyStateText.visibility = View.GONE
            binding.notificationsRecycler.visibility = View.VISIBLE
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
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
                // Refresh list
                loadNotificationsFromServer()
            },
            { error ->
                Toast.makeText(this, "Failed to mark as read", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Safely re-runs the existing code block to fetch the latest notifications from the server
        loadNotificationsFromServer()
    }
}