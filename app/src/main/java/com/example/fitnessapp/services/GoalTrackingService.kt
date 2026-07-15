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
                android.util.Log.d("GoalTrackingService", "📊 Checking ${goals.size} goals")

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

                    // Get previous progress for this specific goal
                    val previousProgress = getPreviousProgress(goal.id)
                    android.util.Log.d("GoalTrackingService", "📊 Goal: ${goal.type}, Progress: $progress%, Previous: $previousProgress")

                    // Check if goal is achieved
                    if (progress >= 100 && previousProgress < 100) {
                        android.util.Log.d("GoalTrackingService", "🎉 Goal achieved in service!")
                        notificationHelper.sendGoalAchievedNotification(
                            goal.type,
                            goal.targetValue,
                            goal.unit,
                            userId = repository.getUserId()
                        )
                        repository.updateGoalStatus(goal.id, "Completed")
                    }
                    // Check milestone (25%, 50%, 75%)
                    else if (progress >= 25 && (progress / 25) > (previousProgress / 25)) {
                        val milestone = (progress / 25) * 25
                        android.util.Log.d("GoalTrackingService", "🏆 Milestone reached: $milestone%")
                        notificationHelper.sendMilestoneNotification(
                            goal.type,
                            milestone,
                            userId = repository.getUserId()
                        )
                    }
                    // Send progress update if significant change
                    else if (progress - previousProgress >= 5 && progress < 100) {
                        val remaining = (goal.targetValue - currentValue).coerceAtLeast(0.0)
                        android.util.Log.d("GoalTrackingService", "📤 Sending progress update: $progress%")
                        notificationHelper.sendGoalProgressNotification(
                            goal.type,
                            progress,
                            remaining,
                            goal.unit,
                            userId = repository.getUserId()
                        )
                    }

                    // Save progress for this goal
                    saveProgress(goal.id, progress)
                }
            } catch (e: Exception) {
                android.util.Log.e("GoalTrackingService", "❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // FIXED: Get previous progress for specific goal
    private fun getPreviousProgress(goalId: Int): Int {
        val prefs = getSharedPreferences("goal_tracking", MODE_PRIVATE)
        return prefs.getInt("goal_progress_$goalId", 0)
    }

    // FIXED: Save progress for specific goal
    private fun saveProgress(goalId: Int, progress: Int) {
        val prefs = getSharedPreferences("goal_tracking", MODE_PRIVATE)
        prefs.edit().putInt("goal_progress_$goalId", progress).apply()
    }
}