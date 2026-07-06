package com.example.fitnessapp.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.models.DailyStats
import com.example.fitnessapp.models.Goal
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.repository.MainRepository
import kotlinx.coroutines.launch

data class GoalProgress(
    val progress: Int,
    val remaining: Double,
    val unit: String,
    val goalType: String
)

class HomeViewModel(
    private val repository: MainRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _dailyStats = MutableLiveData<DailyStats>()
    val dailyStats: LiveData<DailyStats> = _dailyStats

    private val _todayWorkouts = MutableLiveData<List<Workout>>()
    val todayWorkouts: LiveData<List<Workout>> = _todayWorkouts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _activeGoal = MutableLiveData<Goal?>()
    val activeGoal: LiveData<Goal?> = _activeGoal

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            _dailyStats.value = repository.getDailyStats()
            _todayWorkouts.value = repository.getTodayWorkouts()
            _username.value = repository.getUsername()

            // Load active goal with current progress
            loadActiveGoal()

            _isLoading.value = false
        }
    }

    private fun loadActiveGoal() {
        viewModelScope.launch {
            val goals = repository.getActiveGoals()
            if (goals.isNotEmpty()) {
                val goal = goals.first()

                // Get current progress based on goal type
                val currentValue = when (goal.type) {
                    "Calories" -> repository.getTotalCaloriesThisWeek()
                    "Distance" -> repository.getTotalDistanceThisWeek()
                    "Workouts" -> repository.getTotalWorkoutsThisWeek()
                    "Weight" -> {
                        val profile = repository.getUserProfile()
                        // Get starting weight (from when goal was set)
                        val startWeight = goal.startWeight ?: profile?.weight?.toDouble() ?: 70.0
                        val currentWeight = profile?.weight?.toDouble() ?: 70.0

                        // Calculate progress based on weight lost
                        val totalToLose = startWeight - goal.targetValue
                        val lostSoFar = startWeight - currentWeight

                        // FIXED: Return the progress value, not using return@launch
                        if (totalToLose > 0) {
                            lostSoFar / totalToLose
                        } else {
                            0.0
                        }
                    }
                    else -> goal.currentValue
                }

                // Calculate progress percentage
                val progress = if (goal.targetValue > 0 && goal.type != "Weight") {
                    ((currentValue / goal.targetValue) * 100).toFloat().coerceIn(0f, 100f)
                } else if (goal.type == "Weight") {
                    // For weight, currentValue is already a percentage (0.0 to 1.0)
                    (currentValue * 100).toFloat().coerceIn(0f, 100f)
                } else {
                    0f
                }

                val updatedGoal = goal.copy(
                    currentValue = if (goal.type == "Weight") {
                        // Store the weight itself in currentValue
                        repository.getUserProfile()?.weight?.toDouble() ?: 70.0
                    } else {
                        currentValue
                    },
                    progress = progress.toDouble()
                )
                _activeGoal.value = updatedGoal
            } else {
                _activeGoal.value = null
            }
        }
    }

    fun calculateGoalProgress(): Int {
        val stats = _dailyStats.value ?: return 0
        val goal = 800
        return (stats.caloriesBurned.toFloat() / goal * 100).toInt()
    }
}