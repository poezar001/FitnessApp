package com.example.fitnessapp.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.models.DailyStats
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.repository.Goal
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

    private val _goalProgress = MutableLiveData<GoalProgress?>()
    val goalProgress: LiveData<GoalProgress?> = _goalProgress

    private val _currentWeight = MutableLiveData<Double>()
    val currentWeight: LiveData<Double> = _currentWeight

    private val _activeGoal = MutableLiveData<Goal?>()
    val activeGoal: LiveData<Goal?> = _activeGoal

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            // Load daily stats
            _dailyStats.value = repository.getDailyStats()

            // Load today's workouts (REAL data from database)
            _todayWorkouts.value = repository.getTodayWorkouts()

            _username.value = repository.getUsername()

            // Load current weight from profile
            val profile = repository.getUserProfile()
            _currentWeight.value = profile?.weight?.toDouble() ?: 70.0

            // Load goal progress
            loadGoalProgress()

            _isLoading.value = false
        }
    }

    private fun loadGoalProgress() {
        viewModelScope.launch {
            val goals = repository.getActiveGoals()
            _activeGoal.value = goals.firstOrNull()

            if (goals.isNotEmpty()) {
                val goal = goals.first()

                when (goal.type) {
                    "Weight" -> {
                        val currentWeight = _currentWeight.value ?: 70.0
                        val targetWeight = goal.targetValue

                        if (currentWeight > 0 && targetWeight > 0) {
                            val difference = currentWeight - targetWeight

                            if (difference > 0) {
                                // Losing weight
                                val startingWeight = getStartingWeight()
                                val totalToLose = startingWeight - targetWeight
                                val lostSoFar = startingWeight - currentWeight

                                val progress = if (totalToLose > 0) {
                                    ((lostSoFar / totalToLose) * 100).toInt().coerceIn(0, 100)
                                } else 0

                                _goalProgress.value = GoalProgress(
                                    progress = progress,
                                    remaining = difference,
                                    unit = goal.unit,
                                    goalType = "Weight"
                                )
                            } else if (difference < 0) {
                                // Gaining weight
                                _goalProgress.value = GoalProgress(
                                    progress = 0,
                                    remaining = -difference,
                                    unit = goal.unit,
                                    goalType = "Weight"
                                )
                            } else {
                                _goalProgress.value = GoalProgress(
                                    progress = 100,
                                    remaining = 0.0,
                                    unit = goal.unit,
                                    goalType = "Weight"
                                )
                            }
                        }
                    }
                    else -> {
                        // For Calories, Distance, Workouts goals
                        val progress = if (goal.targetValue > 0) {
                            ((goal.currentValue / goal.targetValue) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        val remaining = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)

                        _goalProgress.value = GoalProgress(
                            progress = progress,
                            remaining = remaining,
                            unit = goal.unit,
                            goalType = goal.type
                        )
                    }
                }
            } else {
                _goalProgress.value = null
            }
        }
    }

    private fun getStartingWeight(): Double {
        val startingWeight = sharedPrefs.getFloat("starting_weight", 0f).toDouble()
        return if (startingWeight > 0) {
            startingWeight
        } else {
            val current = _currentWeight.value ?: 70.0
            sharedPrefs.edit().putFloat("starting_weight", current.toFloat()).apply()
            current
        }
    }
}