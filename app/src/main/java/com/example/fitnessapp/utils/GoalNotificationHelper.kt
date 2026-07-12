package com.example.fitnessapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.MainDashboardActivity
import org.json.JSONObject

class GoalNotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "goal_channel"
        private const val CHANNEL_NAME = "Goal Tracking"
        private const val NOTIFICATION_ID = 2001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for goal progress and achievements"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendGoalProgressNotification(
        goalType: String,
        progress: Int,
        remaining: Double,
        unit: String,
        caloriesBurned: Int = 0,
        userId: Int = 0,
        activityType: String = ""
    ) {
        val intent = Intent(context, MainDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityEmoji = getActivityEmoji(activityType)

        // Build message
        val message = when {
            progress >= 100 -> "🎉 Amazing! You reached your $goalType goal!"
            caloriesBurned > 0 && progress >= 75 -> "$activityEmoji You burned $caloriesBurned kcal! Only $remaining $unit to go!"
            caloriesBurned > 0 && progress >= 50 -> "$activityEmoji Great job! $caloriesBurned kcal burned, $remaining $unit to go!"
            progress >= 75 -> "💪 Almost there! Only $remaining $unit to go!"
            progress >= 50 -> "🌟 Halfway there! $remaining $unit to go!"
            else -> "💪 Keep pushing! You're $progress% to your $goalType goal ($remaining $unit left)!"
        }

        val title = when {
            progress >= 100 -> "🎉 Goal Achieved!"
            progress >= 75 -> "💪 Almost There!"
            progress >= 50 -> "🌟 Great Progress!"
            progress >= 25 -> "🚀 Good Start!"
            else -> "🎯 Goal Progress"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_fitness_plus)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID + progress, notification)

        if (userId > 0) {
            saveNotificationToDatabase(userId, title, message, "goal_progress")
        }
    }

    fun sendGoalAchievedNotification(
        goalType: String,
        targetValue: Double,
        unit: String,
        userId: Int = 0,
        activityType: String = ""
    ) {
        val intent = Intent(context, MainDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityEmoji = getActivityEmoji(activityType)
        val title = "🎉 Goal Achieved!"
        val message = "$activityEmoji Congratulations! You reached your $goalType goal of $targetValue $unit! 🏆"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_fitness_plus)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID + 999, notification)

        if (userId > 0) {
            saveNotificationToDatabase(userId, title, message, "goal_achieved")
        }
    }

    fun sendMilestoneNotification(
        goalType: String,
        milestone: Int,
        userId: Int = 0
    ) {
        val intent = Intent(context, MainDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🌟 Milestone Reached!"
        val message = "You've completed $milestone% of your $goalType goal! Keep going!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_fitness_plus)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID + milestone, notification)

        if (userId > 0) {
            saveNotificationToDatabase(userId, title, message, "milestone")
        }
    }

    // NEW: Custom notification for no goals
    fun sendCustomNotification(
        title: String,
        message: String,
        userId: Int = 0
    ) {
        val intent = Intent(context, MainDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_fitness_plus)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID + 1000, notification)

        if (userId > 0) {
            saveNotificationToDatabase(userId, title, message, "workout_complete")
        }
    }

    private fun getActivityEmoji(activityType: String): String {
        return when (activityType) {
            "Running" -> "🏃"
            "Cycling" -> "🚲"
            "Walking" -> "🚶"
            "Weightlifting" -> "💪"
            "Yoga" -> "🧘"
            "Meditation" -> "🧠"
            "Strength" -> "🏋️"
            "Pilates" -> "🧘‍♀️"
            "Kickboxing" -> "🥊"
            "Treadmill" -> "🏃‍♂️"
            else -> "💪"
        }
    }

    private fun saveNotificationToDatabase(userId: Int, title: String, message: String, type: String) {
        try {
            val url = "http://10.0.2.2/fitness_app/save_notification.php"
            val jsonBody = JSONObject().apply {
                put("user_id", userId)
                put("title", title)
                put("message", message)
                put("type", type)
            }

            val request = object : JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                { response ->
                    android.util.Log.d("NotificationHelper", "Notification saved: $response")
                },
                { error ->
                    android.util.Log.e("NotificationHelper", "Failed to save: ${error.message}")
                }
            ) {
                override fun getBodyContentType(): String = "application/json"
            }

            Volley.newRequestQueue(context).add(request)

        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error: ${e.message}")
        }
    }

    private fun getMotivationalMessage(progress: Int): String {
        return when {
            progress >= 100 -> "🏆 Amazing! You've achieved your goal!"
            progress >= 75 -> "💪 Almost there! Keep pushing!"
            progress >= 50 -> "🌟 Great progress! Halfway there!"
            progress >= 25 -> "🚀 Good start! Keep going!"
            else -> "💪 Every step counts! Keep moving forward!"
        }
    }
}