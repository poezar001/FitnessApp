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
            val success = repository.saveWorkout(workout)
            _saveResult.value = success
            if (success) {
                loadWorkouts()
                refreshHomeData()
                refreshAnalytics()
                checkAndUnlockAchievements()
                updateGoalProgress()

                // Send notification after workout
                sendGoalNotification(workout)
            }
            _isLoading.value = false
        }
    }

    private suspend fun sendGoalNotification(workout: Workout) {
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
                val userId = repository.getUserId()

                val notificationHelper = GoalNotificationHelper(context)

                if (progress >= 100) {
                    notificationHelper.sendGoalAchievedNotification(
                        goal.type,
                        targetCalories,
                        goal.unit,
                        userId
                    )
                } else {
                    notificationHelper.sendGoalProgressNotification(
                        goal.type,
                        progress,
                        remaining,
                        goal.unit,
                        workout.caloriesBurned,
                        userId
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    private fun refreshHomeData() {
        viewModelScope.launch {
            val stats = repository.getDailyStats()
            val todayWorkouts = repository.getWorkouts("Today", "All")
        }
    }

    private fun refreshAnalytics() {
        viewModelScope.launch {
            val analytics = repository.getAnalytics()
        }
    }

    private suspend fun updateGoalProgress() {
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
    }

    private suspend fun checkAndUnlockAchievements() {
        val stats = repository.getDailyStats()
        val totalWorkouts = repository.getTotalWorkoutsCount()

        // Check for First Workout achievement
        if (totalWorkouts == 1) {
            repository.unlockAchievement("First Workout", "🏆")
        }

        // Check for 1000 Calories achievement
        val totalCalories = repository.getTotalCaloriesAllTime()
        if (totalCalories >= 1000) {
            repository.unlockAchievement("1000 Calories Burned", "🔥")
        }

        // Check for 7-Day Streak
        val streak = repository.getCurrentStreak()
        if (streak >= 7) {
            repository.unlockAchievement("7-Day Streak", "⭐")
        }

        // Check for 5K Run
        val totalDistance = repository.getTotalDistanceAllTime()
        if (totalDistance >= 5.0) {
            repository.unlockAchievement("5K Runner", "🏃")
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