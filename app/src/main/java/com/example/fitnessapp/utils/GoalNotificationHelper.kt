package com.example.fitnessapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.MainDashboardActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class GoalNotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "fitness_goal_channel"
        private const val CHANNEL_NAME = "Fitness Goals"
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
                description = "Notifications for workout progress and achievements"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            android.util.Log.d("NotificationHelper", "✅ Notification channel created: $CHANNEL_ID")
        }
    }

    // ==================== NEW: Workout Progress Notification ====================

    fun sendWorkoutProgressNotification(
        userId: Int,
        activityType: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        goalType: String,
        goalTarget: Double,
        goalUnit: String,
        progress: Int,
        remaining: Double,
        workoutsDone: Int = 0,
        workoutsRemaining: Int = 0
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending workout progress notification: $activityType")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                return
            }

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val emoji = getActivityEmoji(activityType)

            // Build progress message based on goal type
            val message = when (goalType) {
                "Workouts" -> {
                    "$emoji Workout $workoutsDone of $goalTarget completed! 🎯\n" +
                            "$workoutsRemaining more workouts to reach your goal!"
                }
                "Calories" -> {
                    "$emoji You burned $caloriesBurned kcal this session!\n" +
                            "${String.format("%.1f", remaining)} $goalUnit remaining to reach ${goalTarget.toInt()} $goalUnit goal! 💪"
                }
                "Distance" -> {
                    "$emoji You covered ${String.format("%.1f", caloriesBurned.toDouble() / 1000)} km!\n" +
                            "${String.format("%.1f", remaining)} $goalUnit remaining to reach ${goalTarget.toInt()} $goalUnit goal! 🏃"
                }
                else -> {
                    "$emoji Great $activityType session!\n" +
                            "Progress: $progress% toward your $goalType goal!"
                }
            }

            val title = when {
                progress >= 75 -> "🔥 Almost there! $progress% complete!"
                progress >= 50 -> "💪 Halfway to your goal! $progress% complete!"
                progress >= 25 -> "🚀 Great start! $progress% complete!"
                else -> "🎯 Keep going! $progress% complete!"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_fitness_plus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null) {
                val notificationId = (NOTIFICATION_ID + System.currentTimeMillis() % 10000).toInt()
                manager.notify(notificationId, notification)
                android.util.Log.d("NotificationHelper", "✅ Workout progress notification displayed! ID: $notificationId")
            }

            // Save to database
            if (userId > 0) {
                saveNotificationToDatabase(userId, title, message, "workout_progress")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // ==================== NEW: Goal Reminder Notification ====================

    fun sendGoalReminderNotification(
        userId: Int,
        goalType: String,
        targetValue: Double,
        unit: String,
        currentValue: Double,
        remaining: Double,
        daysRemaining: Int,
        progress: Int
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending goal reminder notification")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                return
            }

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_goals", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progressEmoji = when {
                progress >= 75 -> "🔥"
                progress >= 50 -> "💪"
                progress >= 25 -> "🚀"
                else -> "⏰"
            }

            val title = "⏰ Goal Reminder: $goalType"

            val message = buildString {
                append("$progressEmoji You're at $progress% of your $goalType goal!\n")
                append("📊 ${String.format("%.1f", currentValue)} of ${String.format("%.1f", targetValue)} $unit completed\n")
                append("📅 $daysRemaining days remaining\n")
                if (remaining > 0) {
                    append("💪 Need ${String.format("%.1f", remaining)} more $unit to reach your goal!")
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("You're at $progress% of your $goalType goal!")
                .setSmallIcon(R.drawable.ic_fitness_plus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null) {
                val notificationId = (NOTIFICATION_ID + 3000 + System.currentTimeMillis() % 5000).toInt()
                manager.notify(notificationId, notification)
                android.util.Log.d("NotificationHelper", "✅ Goal reminder notification displayed! ID: $notificationId")
            }

            // Save to database
            if (userId > 0) {
                saveNotificationToDatabase(userId, title, message, "goal_reminder")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // ==================== NEW: Daily Activity Reminder ====================

    fun sendDailyActivityReminder(
        userId: Int,
        activityType: String = "",
        message: String = ""
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending daily activity reminder")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                return
            }

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_add_workout", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🏃 Don't forget your workout today!"
            val defaultMessage = "Stay active and keep pushing toward your fitness goals! 💪"
            val finalMessage = if (message.isNotEmpty()) message else defaultMessage

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(finalMessage)
                .setSmallIcon(R.drawable.ic_fitness_plus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(finalMessage))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null) {
                val notificationId = (NOTIFICATION_ID + 4000 + System.currentTimeMillis() % 5000).toInt()
                manager.notify(notificationId, notification)
                android.util.Log.d("NotificationHelper", "✅ Daily activity reminder displayed! ID: $notificationId")
            }

            // Save to database
            if (userId > 0) {
                saveNotificationToDatabase(userId, title, finalMessage, "daily_reminder")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // ==================== EXISTING METHODS ====================

    fun sendGoalProgressNotification(
        goalType: String,
        progress: Int,
        remaining: Double,
        unit: String,
        caloriesBurned: Int = 0,
        userId: Int = 0,
        activityType: String = ""
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending progress notification: $goalType, progress: $progress%")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                if (userId > 0) {
                    val title = when {
                        progress >= 100 -> "🎉 Goal Achieved!"
                        progress >= 75 -> "💪 Almost There!"
                        progress >= 50 -> "🌟 Great Progress!"
                        else -> "🎯 Goal Progress"
                    }
                    val message = getProgressMessage(goalType, progress, remaining, unit, caloriesBurned, activityType)
                    saveNotificationToDatabase(userId, title, message, "goal_progress")
                }
                return
            }

            val validProgress = progress.coerceIn(0, 100)

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val message = getProgressMessage(goalType, validProgress, remaining, unit, caloriesBurned, activityType)
            val title = getProgressTitle(validProgress)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_fitness_plus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null) {
                val notificationId = (NOTIFICATION_ID + System.currentTimeMillis() % 10000).toInt()
                manager.notify(notificationId, notification)
                android.util.Log.d("NotificationHelper", "✅ Progress notification displayed! ID: $notificationId")
            }

            if (userId > 0) {
                saveNotificationToDatabase(userId, title, message, "goal_progress")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun sendGoalAchievedNotification(
        goalType: String,
        targetValue: Double,
        unit: String,
        userId: Int = 0,
        activityType: String = ""
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending goal achieved notification")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                if (userId > 0) {
                    val title = "🎉 Goal Achieved!"
                    val message = "Congratulations! You reached your $goalType goal! 🏆"
                    saveNotificationToDatabase(userId, title, message, "goal_achieved")
                }
                return
            }

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val activityEmoji = getActivityEmoji(activityType)
            val title = "🎉 Goal Achieved!"
            val message = "$activityEmoji Congratulations! You reached your $goalType goal of ${String.format("%.1f", targetValue)} $unit! 🏆"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_fitness_plus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID + 999 + (System.currentTimeMillis() % 1000).toInt(), notification)
            android.util.Log.d("NotificationHelper", "✅ Goal achieved notification sent!")

            if (userId > 0) {
                saveNotificationToDatabase(userId, title, message, "goal_achieved")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
        }
    }

    fun sendCustomNotification(
        title: String,
        message: String,
        userId: Int = 0
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending custom notification: $title")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                if (userId > 0) {
                    saveNotificationToDatabase(userId, title, message, "workout_complete")
                }
                return
            }

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
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
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null) {
                val notificationId = (NOTIFICATION_ID + 1000 + (System.currentTimeMillis() % 5000).toInt())
                manager.notify(notificationId, notification)
                android.util.Log.d("NotificationHelper", "✅ Custom notification displayed! ID: $notificationId")
            }

            if (userId > 0) {
                saveNotificationToDatabase(userId, title, message, "workout_complete")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getProgressMessage(goalType: String, progress: Int, remaining: Double, unit: String, caloriesBurned: Int, activityType: String): String {
        val emoji = getActivityEmoji(activityType)
        return when {
            progress >= 100 -> "🎉 Amazing! You reached your $goalType goal!"
            caloriesBurned > 0 && progress >= 75 -> "$emoji You burned $caloriesBurned kcal! Only ${String.format("%.1f", remaining)} $unit to go!"
            caloriesBurned > 0 && progress >= 50 -> "$emoji Great job! $caloriesBurned kcal burned, ${String.format("%.1f", remaining)} $unit to go!"
            progress >= 75 -> "💪 Almost there! Only ${String.format("%.1f", remaining)} $unit to go!"
            progress >= 50 -> "🌟 Halfway there! ${String.format("%.1f", remaining)} $unit to go!"
            progress >= 25 -> "🚀 Good progress! ${String.format("%.1f", remaining)} $unit to go!"
            else -> "💪 Keep going! You're $progress% to your $goalType goal!"
        }
    }

    private fun getProgressTitle(progress: Int): String {
        return when {
            progress >= 100 -> "🎉 Goal Achieved!"
            progress >= 75 -> "💪 Almost There!"
            progress >= 50 -> "🌟 Great Progress!"
            progress >= 25 -> "🚀 Good Start!"
            else -> "🎯 Goal Progress"
        }
    }



    private fun getActivityEmoji(activityType: String): String {
        return when (activityType.lowercase()) {
            "running" -> "🏃"
            "cycling" -> "🚲"
            "walking" -> "🚶"
            "weightlifting", "strength" -> "💪"
            "yoga" -> "🧘"
            "meditation" -> "🧠"
            "pilates" -> "🧘‍♀️"
            "kickboxing" -> "🥊"
            "treadmill" -> "🏃‍♂️"
            else -> "💪"
        }
    }
    fun sendMilestoneNotification(
        goalType: String,
        milestone: Int,
        userId: Int = 0
    ) {
        try {
            android.util.Log.d("NotificationHelper", "📢 Sending milestone notification: $milestone%")

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "⚠️ Notifications are disabled")
                if (userId > 0) {
                    val title = "🌟 Milestone Reached!"
                    val message = "You've completed $milestone% of your $goalType goal! Keep going! 💪"
                    saveNotificationToDatabase(userId, title, message, "milestone")
                }
                return
            }

            val intent = Intent(context, MainDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🌟 Milestone Reached!"
            val message = "You've completed $milestone% of your $goalType goal! Keep going! 💪"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_fitness_plus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID + milestone + (System.currentTimeMillis() % 1000).toInt(), notification)
            android.util.Log.d("NotificationHelper", "✅ Milestone notification sent!")

            if (userId > 0) {
                saveNotificationToDatabase(userId, title, message, "milestone")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
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
                    android.util.Log.d("NotificationHelper", "✅ Notification saved to DB")
                    // Send broadcast to refresh badge
                    val intent = Intent("REFRESH_NOTIFICATION_BADGE")
                    context.sendBroadcast(intent)
                    android.util.Log.d("NotificationHelper", "📤 Broadcast sent to refresh badge")
                },
                { error ->
                    android.util.Log.e("NotificationHelper", "❌ Failed to save: ${error.message}")
                }
            ) {
                override fun getBodyContentType(): String = "application/json"
            }

            Volley.newRequestQueue(context).add(request)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error: ${e.message}")
        }
    }
}