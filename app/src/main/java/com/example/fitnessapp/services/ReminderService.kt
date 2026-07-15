package com.example.fitnessapp.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.JobIntentService
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.GoalNotificationHelper
import kotlinx.coroutines.*

class ReminderService : JobIntentService() {

    companion object {
        private const val JOB_ID = 2001
        private const val REMINDER_TIME_HOUR = 20 // 8 PM
        private const val REMINDER_TIME_MINUTE = 0

        fun enqueueWork(context: Context) {
            enqueueWork(context, ReminderService::class.java, JOB_ID, Intent())
        }

        fun scheduleDailyReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderService::class.java)

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, REMINDER_TIME_HOUR)
                set(java.util.Calendar.MINUTE, REMINDER_TIME_MINUTE)
                set(java.util.Calendar.SECOND, 0)

                // If time already passed, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            android.util.Log.d("ReminderService", "📅 Daily reminder scheduled for ${calendar.time}")
        }

        fun cancelReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderService::class.java)
            val pendingIntent = PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            android.util.Log.d("ReminderService", "📅 Reminder cancelled")
        }
    }

    override fun onHandleWork(intent: Intent) {
        checkAndSendReminders()
    }

    private fun checkAndSendReminders() {
        val repository = MainRepository(this)
        val notificationHelper = GoalNotificationHelper(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = repository.getUserId()
                if (userId == -1) return@launch

                val goals = repository.getActiveGoals()
                android.util.Log.d("ReminderService", "📊 Checking reminders for ${goals.size} goals")

                goals.forEach { goal ->
                    val currentValue = when (goal.type) {
                        "Calories" -> repository.getTotalCaloriesThisWeek()
                        "Distance" -> repository.getTotalDistanceThisWeek()
                        "Workouts" -> repository.getTotalWorkoutsThisWeek()
                        else -> goal.currentValue
                    }

                    val progress = if (goal.targetValue > 0) {
                        ((currentValue / goal.targetValue) * 100).toInt().coerceIn(0, 100)
                    } else 0

                    val remaining = (goal.targetValue - currentValue).coerceAtLeast(0.0)

                    // Only send reminder if progress is less than 100%
                    if (progress < 100) {
                        // Calculate days remaining
                        val daysRemaining = calculateDaysRemaining(goal.targetDate)

                        // Send reminder if days remaining <= 3 OR if no activity today
                        if (daysRemaining <= 3) {
                            android.util.Log.d("ReminderService", "⏰ Sending reminder for ${goal.type} (${daysRemaining} days left)")

                            notificationHelper.sendGoalReminderNotification(
                                userId = userId,
                                goalType = goal.type,
                                targetValue = goal.targetValue,
                                unit = goal.unit,
                                currentValue = currentValue,
                                remaining = remaining,
                                daysRemaining = daysRemaining,
                                progress = progress
                            )
                        }
                    }
                }

                // Check if user has done any workout today
                val todayWorkouts = repository.getTodayWorkouts()
                if (todayWorkouts.isEmpty()) {
                    android.util.Log.d("ReminderService", "⏰ No workout today, sending activity reminder")
                    notificationHelper.sendDailyActivityReminder(
                        userId = userId,
                        message = "You haven't logged any activity today! Get moving and reach your fitness goals! 💪"
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("ReminderService", "❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun calculateDaysRemaining(targetDate: String): Int {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val target = format.parse(targetDate) ?: java.util.Date()
            val today = java.util.Date()

            val diff = target.time - today.time
            val days = (diff / (24 * 60 * 60 * 1000)).toInt()
            days.coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }
}