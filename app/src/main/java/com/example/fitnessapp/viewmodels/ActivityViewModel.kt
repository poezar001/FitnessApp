package com.example.fitnessapp.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.models.WorkoutSummary
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.ApiGoal
import com.example.fitnessapp.utils.GoalNotificationHelper
import com.example.fitnessapp.utils.NotificationScopeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityViewModel(private val repository: MainRepository) : ViewModel() {

    private lateinit var context: Context
    private var notificationHelper: GoalNotificationHelper? = null

    private val _goals = MutableLiveData<List<ApiGoal>>()
    val goals: LiveData<List<ApiGoal>> = _goals

    private val _workouts = MutableLiveData<List<Workout>>()
    val workouts: LiveData<List<Workout>> = _workouts

    private val _filteredWorkouts = MutableLiveData<List<Workout>>()
    val filteredWorkouts: LiveData<List<Workout>> = _filteredWorkouts

    private val _workoutSummary = MutableLiveData<WorkoutSummary>()
    val workoutSummary: LiveData<WorkoutSummary> = _workoutSummary

    private val _selectedFilter = MutableLiveData("Today")
    val selectedFilter: LiveData<String> = _selectedFilter

    private val _selectedType = MutableLiveData("All")
    val selectedType: LiveData<String> = _selectedType

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    fun init(context: Context) {
        this.context = context.applicationContext
        notificationHelper = GoalNotificationHelper(this.context)
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            _isLoading.value = true
            val (allWorkouts, summary) = repository.getWorkouts(_selectedFilter.value ?: "Today", _selectedType.value ?: "All")
            _workouts.value = allWorkouts
            _workoutSummary.value = summary
            applyFilters()
            _isLoading.value = false
        }
    }

    fun addWorkout(workout: Workout) {
        // Use GlobalScope for the entire operation to prevent cancellation
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val success = repository.saveWorkout(workout)
                android.util.Log.d("ActivityViewModel", "Save result: $success")

                withContext(Dispatchers.Main) {
                    _saveResult.value = success
                }

                if (success) {
                    android.util.Log.d("ActivityViewModel", "✅ Workout saved: ${workout.activityType}")

                    // Update goal progress
                    try {
                        updateGoalProgress()
                    } catch (e: Exception) {
                        android.util.Log.e("ActivityViewModel", "❌ Goal progress update error: ${e.message}")
                    }

                    // Check and unlock achievements
                    try {
                        checkAndUnlockAchievements()
                    } catch (e: Exception) {
                        android.util.Log.e("ActivityViewModel", "❌ Achievement check error: ${e.message}")
                    }

                    // Refresh data on main thread
                    withContext(Dispatchers.Main) {
                        loadWorkouts()
                        refreshHomeData()
                        refreshAnalytics()
                    }

                    // ============ SEND NOTIFICATION WITH DELAY ============
                    // Wait for database to fully update
                    delay(2000)

                    try {
                        android.util.Log.d("ActivityViewModel", "🚀 Starting notification from GlobalScope")
                        sendWorkoutNotification(workout)
                        android.util.Log.d("ActivityViewModel", "✅ Notification completed successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("ActivityViewModel", "❌ Notification failed: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    _saveResult.value = false
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun sendWorkoutNotification(workout: Workout) {
        try {
            android.util.Log.d("ActivityViewModel", "========== 🚀 STARTING NOTIFICATION ==========")
            android.util.Log.d("ActivityViewModel", "📢 Workout: ${workout.activityType}, Calories: ${workout.caloriesBurned}")

            if (!::context.isInitialized) {
                android.util.Log.e("ActivityViewModel", "❌ Context not initialized!")
                return
            }

            val helper = notificationHelper ?: run {
                android.util.Log.e("ActivityViewModel", "❌ NotificationHelper is null, creating new one")
                GoalNotificationHelper(context).also { notificationHelper = it }
            }

            val userId = repository.getUserId()
            android.util.Log.d("ActivityViewModel", "📊 User ID: $userId")

            // Get goals with fresh data
            val goals = repository.getActiveGoals()
            android.util.Log.d("ActivityViewModel", "📊 Found ${goals.size} goals")

            if (goals.isEmpty()) {
                // No goals - send simple workout notification
                android.util.Log.d("ActivityViewModel", "ℹ️ No goals, sending workout completion notification")
                val activityEmoji = getActivityEmoji(workout.activityType)
                helper.sendCustomNotification(
                    title = "💪 Workout Complete!",
                    message = "$activityEmoji Great job! You completed ${workout.activityType} for ${workout.durationMinutes} minutes, burning ${workout.caloriesBurned} kcal!",
                    userId = userId
                )
                android.util.Log.d("ActivityViewModel", "✅ Simple notification sent")
                return
            }

            // Process each goal
            var sentAchievementNotification = false

            goals.forEach { goal ->
                try {
                    android.util.Log.d("ActivityViewModel", "📊 Processing goal: ${goal.type}")
                    android.util.Log.d("ActivityViewModel", "📊 Goal target: ${goal.targetValue}, Unit: ${goal.unit}")

                    // Get current value for this goal type
                    val currentValue = when (goal.type) {
                        "Calories" -> {
                            val value = repository.getTotalCaloriesThisWeek()
                            android.util.Log.d("ActivityViewModel", "📊 Calories this week: $value")
                            value
                        }
                        "Distance" -> {
                            val value = repository.getTotalDistanceThisWeek()
                            android.util.Log.d("ActivityViewModel", "📊 Distance this week: $value")
                            value
                        }
                        "Workouts" -> {
                            val value = repository.getTotalWorkoutsThisWeek()
                            android.util.Log.d("ActivityViewModel", "📊 Workouts this week: $value")
                            value
                        }
                        else -> {
                            android.util.Log.d("ActivityViewModel", "📊 Unknown goal type: ${goal.type}, using workout calories")
                            workout.caloriesBurned.toDouble()
                        }
                    }

                    val targetValue = goal.targetValue
                    android.util.Log.d("ActivityViewModel", "📊 Current: $currentValue, Target: $targetValue")

                    // Calculate progress
                    val progress = if (targetValue > 0) {
                        ((currentValue / targetValue) * 100).toInt().coerceIn(0, 100)
                    } else 0

                    val remaining = (targetValue - currentValue).coerceAtLeast(0.0)

                    android.util.Log.d("ActivityViewModel", "📊 Progress: $progress%, Remaining: $remaining")
                    android.util.Log.d("ActivityViewModel", "📊 Is goal achieved? ${progress >= 100}")

                    // ============ SEND PROGRESS NOTIFICATION ============
                    android.util.Log.d("ActivityViewModel", "📤 Sending progress notification...")
                    helper.sendGoalProgressNotification(
                        goalType = goal.type,
                        progress = progress,
                        remaining = remaining,
                        unit = goal.unit,
                        caloriesBurned = workout.caloriesBurned,
                        userId = userId,
                        activityType = workout.activityType
                    )
                    android.util.Log.d("ActivityViewModel", "✅ Progress notification sent")

                    // ============ CHECK IF GOAL IS ACHIEVED ============
                    if (progress >= 100 && !sentAchievementNotification) {
                        android.util.Log.d("ActivityViewModel", "🎉🎉🎉 GOAL ACHIEVED! Sending achievement notification! 🎉🎉🎉")

                        // Send achievement notification
                        try {
                            helper.sendGoalAchievedNotification(
                                goalType = goal.type,
                                targetValue = targetValue,
                                unit = goal.unit,
                                userId = userId,
                                activityType = workout.activityType
                            )
                            sentAchievementNotification = true
                            android.util.Log.d("ActivityViewModel", "✅ Achievement notification sent successfully!")
                        } catch (e: Exception) {
                            android.util.Log.e("ActivityViewModel", "❌ Failed to send achievement notification: ${e.message}")
                            e.printStackTrace()
                        }

                        // Update goal status
                        try {
                            repository.updateGoalStatus(goal.id, "Completed")
                        } catch (e: Exception) {
                            android.util.Log.e("ActivityViewModel", "❌ Failed to update goal status: ${e.message}")
                        }

                        // Unlock achievement
                        try {
                            repository.unlockAchievement("Goal Achieved", "🏆")
                        } catch (e: Exception) {
                            android.util.Log.e("ActivityViewModel", "❌ Failed to unlock achievement: ${e.message}")
                        }
                    } else if (progress >= 100) {
                        android.util.Log.d("ActivityViewModel", "📊 Goal already achieved, skipping duplicate notification")
                    } else {
                        android.util.Log.d("ActivityViewModel", "📊 Goal not yet achieved. Progress: $progress%")
                    }

                } catch (e: Exception) {
                    android.util.Log.e("ActivityViewModel", "❌ Error processing goal ${goal.type}: ${e.message}")
                    e.printStackTrace()
                }
            }

            android.util.Log.d("ActivityViewModel", "========== ✅ NOTIFICATION COMPLETE ==========")

        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Error sending notification: ${e.message}")
            e.printStackTrace()
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

    private suspend fun refreshHomeData() {
        try {
            repository.getDailyStats()
            repository.getWorkouts("Today", "All")
        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Home data refresh error: ${e.message}")
        }
    }

    private suspend fun refreshAnalytics() {
        try {
            repository.getAnalytics()
        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Analytics refresh error: ${e.message}")
        }
    }

    private suspend fun updateGoalProgress() {
        try {
            val goals = repository.getActiveGoals()
            goals.forEach { goal ->
                val current = when (goal.type) {
                    "Calories" -> repository.getTotalCaloriesThisWeek()
                    "Distance" -> repository.getTotalDistanceThisWeek()
                    "Workouts" -> repository.getTotalWorkoutsThisWeek()
                    else -> goal.currentValue
                }
                repository.updateGoalProgress(goal.id, current)
            }
            android.util.Log.d("ActivityViewModel", "✅ Goal progress updated")
        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Error updating goal progress: ${e.message}")
        }
    }

    private suspend fun checkAndUnlockAchievements() {
        try {
            val totalWorkouts = repository.getTotalWorkoutsCount()
            if (totalWorkouts == 1) {
                repository.unlockAchievement("First Workout", "🏆")
            }

            val totalCalories = repository.getTotalCaloriesAllTime()
            if (totalCalories >= 1000) {
                repository.unlockAchievement("1000 Calories Burned", "🔥")
            }

            val streak = repository.getCurrentStreak()
            if (streak >= 7) {
                repository.unlockAchievement("7-Day Streak", "⭐")
            }

            val totalDistance = repository.getTotalDistanceAllTime()
            if (totalDistance >= 5.0) {
                repository.unlockAchievement("5K Runner", "🏃")
            }

            android.util.Log.d("ActivityViewModel", "✅ Achievements checked")
        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Error checking achievements: ${e.message}")
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        loadWorkouts()
    }

    fun setType(type: String) {
        _selectedType.value = type
        loadWorkouts()
    }

    private fun applyFilters() {
        var filtered = _workouts.value ?: return
        if (_selectedType.value != "All") {
            filtered = filtered.filter { it.activityType == _selectedType.value }
        }
        _filteredWorkouts.value = filtered
    }
}