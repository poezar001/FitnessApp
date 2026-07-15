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
import kotlinx.coroutines.launch

class ActivityViewModel(private val repository: MainRepository) : ViewModel() {

    private lateinit var context: Context

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
        this.context = context
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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // This now waits until NetworkUtils calls callback(...) and returns the actual server result
                val success = repository.saveWorkout(workout)
                android.util.Log.d("ActivityViewModel", "Save result: $success")

                _saveResult.value = success

                if (success) {
                    android.util.Log.d("ActivityViewModel", "✅ Workout saved: ${workout.activityType}")

                    // Perform data updates on background thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        updateGoalProgress()
                        checkAndUnlockAchievements()
                    }

                    loadWorkouts()
                    refreshHomeData()
                    refreshAnalytics()

                    // Send push or local goal achievements
                    sendWorkoutNotification(workout)
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Error: ${e.message}")
                _saveResult.value = false
            }
            _isLoading.value = false
        }
    }

    private suspend fun sendWorkoutNotification(workout: Workout) {
        try {
            android.util.Log.d("ActivityViewModel", "📢 Sending notification for: ${workout.activityType}")

            if (!::context.isInitialized) {
                android.util.Log.e("ActivityViewModel", "❌ Context not initialized!")
                return
            }

            val notificationHelper = GoalNotificationHelper(context)
            val userId = repository.getUserId()
            val goals = repository.getActiveGoals()

            android.util.Log.d("ActivityViewModel", "📊 User ID: $userId, Goals: ${goals.size}")

            // ✅ ALWAYS send notification, even without goals
            val activityEmoji = getActivityEmoji(workout.activityType)

            if (goals.isNotEmpty()) {
                val goal = goals.first()
                val currentCalories = repository.getTotalCaloriesThisWeek()
                val targetCalories = goal.targetValue
                val progress = if (targetCalories > 0) {
                    ((currentCalories / targetCalories) * 100).toInt().coerceIn(0, 100)
                } else 0
                val remaining = (targetCalories - currentCalories).coerceAtLeast(0.0)

                android.util.Log.d("ActivityViewModel", "📊 Progress: $progress%, Remaining: $remaining")

                if (progress >= 100) {
                    notificationHelper.sendGoalAchievedNotification(
                        goal.type, targetCalories, goal.unit, userId, workout.activityType
                    )
                } else {
                    notificationHelper.sendGoalProgressNotification(
                        goal.type, progress, remaining, goal.unit,
                        workout.caloriesBurned, userId, workout.activityType
                    )
                }
            } else {
                // ✅ No goals - send workout completion notification
                android.util.Log.d("ActivityViewModel", "ℹ️ No goals, sending workout completion notification")
                notificationHelper.sendCustomNotification(
                    title = "💪 Workout Complete!",
                    message = "$activityEmoji Great job! You completed ${workout.activityType} for ${workout.durationMinutes} minutes, burning ${workout.caloriesBurned} kcal!",
                    userId = userId
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Error sending notification: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun sendGoalNotification(
        workout: Workout,
        notificationHelper: GoalNotificationHelper,
        userId: Int
    ) {
        try {
            val goals = repository.getActiveGoals()
            if (goals.isNotEmpty()) {
                val goal = goals.first()
                val currentCalories = repository.getTotalCaloriesThisWeek()
                val targetCalories = goal.targetValue
                val progress = if (targetCalories > 0) {
                    ((currentCalories / targetCalories) * 100).toInt().coerceIn(0, 100)
                } else 0
                val remaining = (targetCalories - currentCalories).coerceAtLeast(0.0)

                android.util.Log.d("ActivityViewModel", "📊 Goal: ${goal.type}, Progress: $progress%, Remaining: $remaining, Calories: $currentCalories")

                val activityEmoji = getActivityEmoji(workout.activityType)

                // ✅ FIXED: Send notification for ALL progress levels, not just milestones
                if (progress >= 100) {
                    notificationHelper.sendGoalAchievedNotification(
                        goal.type,
                        targetCalories,
                        goal.unit,
                        userId,
                        workout.activityType
                    )
                } else {
                    // ✅ ALWAYS send progress notification
                    notificationHelper.sendGoalProgressNotification(
                        goal.type,
                        progress,
                        remaining,
                        goal.unit,
                        workout.caloriesBurned,
                        userId,
                        workout.activityType
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityViewModel", "❌ Error in sendGoalNotification: ${e.message}")
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

    private fun refreshHomeData() {
        viewModelScope.launch {
            repository.getDailyStats()
            repository.getWorkouts("Today", "All")
        }
    }

    private fun refreshAnalytics() {
        viewModelScope.launch {
            repository.getAnalytics()
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