package com.example.fitnessapp.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.GoalNotificationHelper
import kotlinx.coroutines.*

class GoalTrackingService : JobIntentService() {

    companion object {
        private const val JOB_ID = 1000

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GoalTrackingService::class.java, JOB_ID, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        checkGoalProgress()
    }

    private fun checkGoalProgress() {
        val repository = MainRepository(this)
        val notificationHelper = GoalNotificationHelper(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val goals = repository.getActiveGoals()
                val previousProgress = getPreviousProgress()

                goals.forEach { goal ->
                    val currentValue = when (goal.type) {
                        "Calories" -> repository.getTotalCaloriesThisWeek()
                        "Distance" -> repository.getTotalDistanceThisWeek()
                        "Workouts" -> repository.getTotalWorkoutsThisWeek().toDouble()
                        "Weight" -> repository.getCurrentWeight()
                        else -> goal.currentValue
                    }

                    val progress = if (goal.targetValue > 0) {
                        ((currentValue / goal.targetValue) * 100).toInt().coerceIn(0, 100)
                    } else 0

                    // Check if goal is achieved
                    if (progress >= 100) {
                        notificationHelper.sendGoalAchievedNotification(
                            goal.type,
                            goal.targetValue,
                            goal.unit
                        )
                        repository.updateGoalStatus(goal.id, "Completed")
                    }
                    // Check milestone (25%, 50%, 75%)
                    else if (progress >= 25 && (progress / 25) > (previousProgress / 25)) {
                        val milestone = (progress / 25) * 25
                        notificationHelper.sendMilestoneNotification(goal.type, milestone)
                    }
                    // Send progress update if significant change
                    else if (progress - previousProgress >= 5) {
                        val remaining = (goal.targetValue - currentValue).coerceAtLeast(0.0)
                        notificationHelper.sendGoalProgressNotification(
                            goal.type,
                            progress,
                            remaining,
                            goal.unit
                        )
                    }

                    saveProgress(progress)
                }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    private fun getPreviousProgress(): Int {
        val prefs = getSharedPreferences("goal_tracking", MODE_PRIVATE)
        return prefs.getInt("last_progress", 0)
    }

    private fun saveProgress(progress: Int) {
        val prefs = getSharedPreferences("goal_tracking", MODE_PRIVATE)
        prefs.edit().putInt("last_progress", progress).apply()
    }
}