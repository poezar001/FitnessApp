package com.example.fitnessapp.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.fitnessapp.models.*
import com.example.fitnessapp.utils.ApiGoal
import com.example.fitnessapp.utils.NetworkUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)

    init {
        NetworkUtils.init(context)
    }

    fun getUserId(): Int = sharedPreferences.getInt("user_id", -1)
    fun getUsername(): String = sharedPreferences.getString("username", "") ?: ""
    fun getUserEmail(): String = sharedPreferences.getString("email", "") ?: ""

    suspend fun getWorkouts(filter: String = "all", type: String = ""): Pair<List<Workout>, WorkoutSummary> {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.getWorkouts(context, getUserId(), filter, type) { workouts, summary ->
                continuation.resume(Pair(workouts, summary))
            }
        }
    }

    suspend fun getDailyStats(): DailyStats {
        val (workouts, summary) = getWorkouts("today")
        android.util.Log.d("MainRepository", "getDailyStats - workouts: ${workouts.size}, summary: $summary")
        val steps = calculateStepsFromWorkouts(workouts)
        return DailyStats(
            steps = steps,
            caloriesBurned = summary.totalCalories,
            workoutMinutes = summary.totalDuration,
            distanceKm = summary.totalDistance
        )
    }

    suspend fun getTodayWorkouts(): List<Workout> {
        val (workouts, _) = getWorkouts("today")
        android.util.Log.d("MainRepository", "getTodayWorkouts - count: ${workouts.size}")
        return workouts
    }

    suspend fun getWeeklyProgress(): List<WeeklyProgress> {
        return getAnalytics().weeklyProgress
    }

    suspend fun saveWorkout(workout: Workout): Boolean {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.saveWorkout(context, workout) { success, message ->
                continuation.resume(success)
            }
        }
    }

    suspend fun getAnalytics(): AnalyticsData {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.getAnalytics(context, getUserId()) { weeklyProgress, distribution, goals, bmi ->
                continuation.resume(AnalyticsData(weeklyProgress, distribution, goals, bmi))
            }
        }
    }

    suspend fun setGoal(goalType: String, targetValue: Double, unit: String, targetDate: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.setGoal(context, getUserId(), goalType, targetValue, unit, targetDate) { success, message ->
                continuation.resume(success)
            }
        }
    }

    fun getUserProfile(): UserProfile? {
        val birthday = sharedPreferences.getString("birthday", null) ?: return null
        val gender = sharedPreferences.getString("gender", null) ?: return null
        val height = sharedPreferences.getFloat("height", 0f)
        val weight = sharedPreferences.getFloat("weight", 0f)
        val activityLevelStr = sharedPreferences.getString("activity_level", null) ?: return null

        return try {
            UserProfile(
                userId = getUserId(),
                birthday = birthday,
                gender = gender,
                height = height,
                weight = weight,
                activityLevel = ActivityLevel.valueOf(activityLevelStr)
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateGoalStatus(goalId: Int, status: String) {
        return suspendCancellableCoroutine { continuation ->
            // Update goal status in database
            continuation.resume(Unit)
        }
    }

    suspend fun getCurrentWeight(): Double {
        return suspendCancellableCoroutine { continuation ->
            val profile = getUserProfile()
            continuation.resume(profile?.weight?.toDouble() ?: 70.0)
        }
    }

    fun getAchievements(): List<Achievement> {
        return listOf(
            Achievement(1, "First Workout", java.util.Date(), "🏆"),
            Achievement(2, "Goal Setter", java.util.Date(), "🎯"),
            Achievement(3, "1000 Calories", java.util.Date(), "🔥")
        )
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }

    // ==================== GOAL RELATED METHODS ====================

    suspend fun getActiveGoals(): List<Goal> {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.getGoals(context, getUserId()) { goals ->
                continuation.resume(goals)
            }
        }
    }



    suspend fun getTotalCaloriesThisWeek(): Double {
        val (_, summary) = getWorkouts("weekly")
        return summary.totalCalories.toDouble()
    }

    suspend fun getTotalDistanceThisWeek(): Double {
        val (_, summary) = getWorkouts("weekly")
        return summary.totalDistance
    }

    suspend fun getTotalWorkoutsThisWeek(): Double {
        val (_, summary) = getWorkouts("weekly")
        return summary.totalWorkouts.toDouble()
    }

    suspend fun getTotalWorkoutsCount(): Int {
        val (workouts, _) = getWorkouts("all")
        return workouts.size
    }

    suspend fun getTotalCaloriesAllTime(): Int {
        val (_, summary) = getWorkouts("all")
        return summary.totalCalories
    }

    suspend fun getTotalDistanceAllTime(): Double {
        val (_, summary) = getWorkouts("all")
        return summary.totalDistance
    }

    suspend fun getCurrentStreak(): Int {
        val (workouts, _) = getWorkouts("all")
        var streak = 0
        var currentDate = java.util.Calendar.getInstance()
        currentDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
        currentDate.set(java.util.Calendar.MINUTE, 0)
        currentDate.set(java.util.Calendar.SECOND, 0)

        val workoutDates = workouts.map { it.workoutDate }.sortedDescending()
        for (date in workoutDates) {
            val calendar = java.util.Calendar.getInstance().apply { time = date }
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)

            if (calendar.timeInMillis == currentDate.timeInMillis) {
                streak++
                currentDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    suspend fun updateGoalProgress(goalId: Int, currentValue: Double) {
        android.util.Log.d("MainRepository", "Update goal $goalId to $currentValue")
    }

    suspend fun unlockAchievement(name: String, icon: String) {
        android.util.Log.d("MainRepository", "Unlock achievement: $name with icon $icon")
    }

    // ==================== HELPER METHODS ====================

    private fun calculateStepsFromWorkouts(workouts: List<Workout>): Int {
        var totalSteps = 0
        workouts.forEach { workout ->
            if (workout.activityType == "Walking" && workout.distanceKm != null) {
                totalSteps += (workout.distanceKm * 1300).toInt()
            }
            workout.steps?.let { totalSteps += it }
        }
        return totalSteps
    }
}

data class AnalyticsData(
    val weeklyProgress: List<WeeklyProgress>,
    val activityDistribution: Map<String, Float>,
    val goals: List<ApiGoal>,
    val bmi: Double
)