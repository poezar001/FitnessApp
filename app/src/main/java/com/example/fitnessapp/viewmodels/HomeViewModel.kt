package com.example.fitnessapp.viewmodels

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
import java.util.Calendar

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

    private val _unreadNotificationCount = MutableLiveData<Int>()
    val unreadNotificationCount: LiveData<Int> = _unreadNotificationCount

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            _dailyStats.value = repository.getDailyStats()
            _todayWorkouts.value = repository.getTodayWorkouts()
            _username.value = repository.getUsername()
            _unreadNotificationCount.value = repository.getUnreadNotificationCount()

            val stats = _dailyStats.value
            loadActiveGoal(stats)

            _isLoading.value = false
        }
    }

    private suspend fun loadActiveGoal(todayStats: DailyStats?) {
        val goals = repository.getActiveGoals()
        if (goals.isNotEmpty()) {
            val goal = goals.first()
            val stats = todayStats ?: repository.getDailyStats()

            if (goal.type == "Calories") {
                val totalMasterGoal = goal.targetValue // e.g., 2000.0
                val todayBurned = stats?.caloriesBurned?.toDouble() ?: 0.0 // e.g., 233.0

                // Get all-time workouts to separate history from today
                val (allWorkouts, _) = repository.getWorkouts("all")

                // Get today's calendar boundaries to isolate older records
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // Sum up calories burned strictly before today
                val yesterdayBurned = allWorkouts
                    .filter { it.workoutDate.before(todayStart) }
                    .sumOf { it.caloriesBurned.toDouble() } // e.g., 170.0

                // Implement calculation rules
                val remainingAtStartOfToday = (totalMasterGoal - yesterdayBurned).coerceAtLeast(0.0) // 1830.0
                val currentRemaining = (remainingAtStartOfToday - todayBurned).coerceAtLeast(0.0) // 1597.0

                // Today's effort percentage against the balance remaining this morning
                val progressPercentage = if (remainingAtStartOfToday > 0.0) {
                    ((todayBurned / remainingAtStartOfToday) * 100).coerceIn(0.0, 100.0)
                } else {
                    0.0
                }

                // FIXED: Mark the goal complete if target is cleared and not yet status marked
                if (currentRemaining <= 0.0 && goal.status != "completed") {
                    repository.updateGoalStatus(goal.id, "completed")
                    repository.unlockAchievement("Goal Achieved", "🔥")
                }

                // Pass the remaining calories balance downstream into custom view fields
                val updatedGoal = goal.copy(
                    currentValue = currentRemaining,
                    progress = progressPercentage
                )
                _activeGoal.value = updatedGoal

            } else {
                // Fallback architecture logic handles structural non-calories variables (Weight, Distance, Workouts)
                val currentValue = when (goal.type) {
                    "Distance" -> _todayWorkouts.value?.sumOf { it.distanceKm ?: 0.0 } ?: 0.0
                    "Workouts" -> _todayWorkouts.value?.size?.toDouble() ?: 0.0
                    "Weight" -> {
                        val profile = repository.getUserProfile()
                        profile?.weight?.toDouble() ?: 70.0
                    }
                    else -> goal.currentValue
                }

                val progress = when {
                    goal.type == "Weight" -> {
                        val profile = repository.getUserProfile()
                        val startWeight = goal.startWeight ?: profile?.weight?.toDouble() ?: 70.0
                        val currentWeight = profile?.weight?.toDouble() ?: 70.0
                        val totalToLose = startWeight - goal.targetValue
                        val lostSoFar = startWeight - currentWeight
                        if (totalToLose > 0) ((lostSoFar / totalToLose) * 100).toFloat().coerceIn(0f, 100f) else 0f
                    }
                    goal.targetValue > 0 -> ((currentValue / goal.targetValue) * 100).toFloat().coerceIn(0f, 100f)
                    else -> 0f
                }

                // FIXED: Handle goal completion checks for non-calories types as well
                if (goal.type != "Weight" && currentValue >= goal.targetValue && goal.status != "completed") {
                    repository.updateGoalStatus(goal.id, "completed")
                    repository.unlockAchievement("Goal Achieved", "🏆")
                } else if (goal.type == "Weight" && currentValue <= goal.targetValue && goal.status != "completed") {
                    repository.updateGoalStatus(goal.id, "completed")
                    repository.unlockAchievement("Goal Achieved", "🎯")
                }

                _activeGoal.value = goal.copy(currentValue = currentValue, progress = progress.toDouble())
            }
        } else {
            _activeGoal.value = null
        }
    }
}