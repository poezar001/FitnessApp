package com.example.fitnessapp.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.fitnessapp.models.*
import com.example.fitnessapp.utils.ApiGoal
import com.example.fitnessapp.utils.NetworkUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

class MainRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)

    private val cachePreferences: SharedPreferences =
        context.getSharedPreferences("fitness_app_offline_cache", Context.MODE_PRIVATE)

    init {
        NetworkUtils.init(context)
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }

    fun getUserId(): Int = sharedPreferences.getInt("user_id", -1)
    fun getUsername(): String = sharedPreferences.getString("username", "") ?: ""
    fun getUserEmail(): String = sharedPreferences.getString("email", "") ?: ""

    // ==================== WORKOUT CACHING SYSTEM ====================

    suspend fun getWorkouts(filter: String = "all", type: String = ""): Pair<List<Workout>, WorkoutSummary> {
        if (!isOnline()) {
            android.util.Log.d("MainRepository", "Offline check: Loading cached layout states.")
            return loadCachedWorkouts(filter, type)
        }

        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.getWorkouts(context, getUserId(), filter, type) { workouts, summary ->
                if (filter == "all" && type.isEmpty()) {
                    cacheWorkoutsData(workouts, summary)
                }
                continuation.resume(Pair(workouts, summary))
            }
        }
    }

    private fun cacheWorkoutsData(workouts: List<Workout>, summary: WorkoutSummary) {
        try {
            val jsonArray = JSONArray()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (w in workouts) {
                val obj = JSONObject().apply {
                    put("id", w.id)
                    put("userId", w.userId)
                    put("activityType", w.activityType)
                    put("durationMinutes", w.durationMinutes)
                    put("caloriesBurned", w.caloriesBurned)
                    put("distanceKm", w.distanceKm ?: 0.0)
                    put("workoutDate", w.workoutDate.let { sdf.format(it) })
                    put("notes", w.notes ?: "")
                    put("steps", w.steps ?: 0)
                }
                jsonArray.put(obj)
            }

            val summaryObj = JSONObject().apply {
                put("totalWorkouts", summary.totalWorkouts)
                put("totalCalories", summary.totalCalories)
                put("totalDuration", summary.totalDuration)
                put("totalDistance", summary.totalDistance)
            }

            cachePreferences.edit().apply {
                putString("cached_workouts_list", jsonArray.toString())
                putString("cached_workouts_summary", summaryObj.toString())
                apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainRepository", "Error saving offline collection layout", e)
        }
    }

    private fun loadCachedWorkouts(filter: String, type: String): Pair<List<Workout>, WorkoutSummary> {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val rawWorkouts = cachePreferences.getString("cached_workouts_list", "[]") ?: "[]"

        val workouts = mutableListOf<Workout>()
        var totalWorkoutsCount = 0
        var totalCaloriesBurned = 0
        var totalDurationMins = 0
        var totalDistanceKm = 0.0

        try {
            val array = JSONArray(rawWorkouts)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dateStr = obj.getString("workoutDate")
                val parsedDate = if (dateStr.isNotEmpty()) sdf.parse(dateStr) else java.util.Date()

                workouts.add(Workout(
                    id = obj.getInt("id"),
                    userId = obj.getInt("userId"),
                    activityType = obj.getString("activityType"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    caloriesBurned = obj.getInt("caloriesBurned"),
                    distanceKm = if (obj.isNull("distanceKm")) null else obj.getDouble("distanceKm"),
                    workoutDate = parsedDate,
                    notes = obj.optString("notes", null),
                    steps = if (obj.isNull("steps")) null else obj.getInt("steps")
                ))
            }

            workouts.addAll(getOfflineQueueWorkouts())

            val filteredWorkouts = workouts.filter { w ->
                val typeMatches = type.isEmpty() || w.activityType.lowercase() == type.lowercase()
                val dateMatches = when (filter) {
                    "today" -> {
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        fmt.format(w.workoutDate) == fmt.format(java.util.Date())
                    }
                    else -> true
                }
                typeMatches && dateMatches
            }

            filteredWorkouts.forEach {
                totalWorkoutsCount++
                totalCaloriesBurned += it.caloriesBurned
                totalDurationMins += it.durationMinutes
                totalDistanceKm += (it.distanceKm ?: 0.0)
            }

            val summary = WorkoutSummary(
                totalWorkouts = totalWorkoutsCount,
                totalCalories = totalCaloriesBurned,
                totalDuration = totalDurationMins,
                totalDistance = totalDistanceKm
            )

            return Pair(filteredWorkouts, summary)
        } catch (e: Exception) {
            return Pair(emptyList(), WorkoutSummary(0, 0, 0, 0.0))
        }
    }

    // ==================== DAILY & STREAM METRICS ====================

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

    // ==================== OFFLINE SAVE SUBMISSIONS SYSTEM ====================

    suspend fun saveWorkout(workout: Workout): Boolean {
        if (!isOnline()) {
            android.util.Log.d("MainRepository", "Offline context captured. Redirecting output parameters.")
            saveToOfflineQueue(workout)
            return true
        }

        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            android.util.Log.d("MainRepository", "Saving workout: ${workout.activityType}, Calories: ${workout.caloriesBurned}")

            NetworkUtils.saveWorkout(context.applicationContext, workout) { success, message ->
                android.util.Log.d("MainRepository", "Save result: success=$success, message=$message")

                if (continuation.isActive) {
                    continuation.resume(success) { }
                }
            }
        }
    }

    private fun saveToOfflineQueue(workout: Workout) {
        try {
            val currentQueueRaw = cachePreferences.getString("offline_workout_queue", "[]") ?: "[]"
            val queueArray = JSONArray(currentQueueRaw)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val obj = JSONObject().apply {
                put("id", -1)
                put("userId", getUserId())
                put("activityType", workout.activityType)
                put("durationMinutes", workout.durationMinutes)
                put("caloriesBurned", workout.caloriesBurned)
                put("distanceKm", workout.distanceKm ?: 0.0)
                put("workoutDate", workout.workoutDate.let { sdf.format(it) })
                put("notes", workout.notes ?: "")
                put("steps", workout.steps ?: 0)
            }
            queueArray.put(obj)
            cachePreferences.edit().putString("offline_workout_queue", queueArray.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("MainRepository", "Error backing up submission locally", e)
        }
    }

    private fun getOfflineQueueWorkouts(): List<Workout> {
        val list = mutableListOf<Workout>()
        val rawQueue = cachePreferences.getString("offline_workout_queue", "[]") ?: "[]"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        try {
            val array = JSONArray(rawQueue)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dateStr = obj.getString("workoutDate")
                list.add(Workout(
                    id = obj.getInt("id"),
                    userId = obj.getInt("userId"),
                    activityType = obj.getString("activityType"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    caloriesBurned = obj.getInt("caloriesBurned"),
                    distanceKm = if (obj.isNull("distanceKm")) null else obj.getDouble("distanceKm"),
                    workoutDate = sdf.parse(dateStr),
                    notes = obj.optString("notes", null),
                    steps = if (obj.isNull("steps")) null else obj.getInt("steps")
                ))
            }
        } catch (e: Exception) { /* Safe catch block */ }
        return list
    }

    // ==================== ANALYTICS & GOALS CACHING ====================

    suspend fun getAnalytics(): AnalyticsData {
        if (!isOnline()) {
            android.util.Log.d("MainRepository", "Offline: Loading analytical structure configurations.")
            return loadCachedAnalytics()
        }

        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.getAnalytics(context, getUserId()) { weeklyProgress, distribution, goals, bmi ->
                val data = AnalyticsData(weeklyProgress, distribution, goals, bmi)
                cacheAnalyticsData(data)
                continuation.resume(data)
            }
        }
    }

    private fun cacheAnalyticsData(data: AnalyticsData) {
        try {
            val obj = JSONObject().apply {
                put("bmi", data.bmi)

                val weeklyArr = JSONArray()
                data.weeklyProgress.forEach {
                    weeklyArr.put(JSONObject().apply {
                        put("day", it.day)
                        put("calories", it.calories)
                        put("duration", it.duration)
                    })
                }
                put("weeklyProgress", weeklyArr)
            }
            cachePreferences.edit().putString("cached_analytics", obj.toString()).apply()
        } catch (e: Exception) { /* Safe catch block */ }
    }

    private fun loadCachedAnalytics(): AnalyticsData {
        val raw = cachePreferences.getString("cached_analytics", "{}") ?: "{}"
        try {
            val obj = JSONObject(raw)
            val bmi = obj.optDouble("bmi", 0.0)

            val weeklyProgressList = mutableListOf<WeeklyProgress>()
            val weeklyArr = obj.optJSONArray("weeklyProgress")
            if (weeklyArr != null) {
                for (i in 0 until weeklyArr.length()) {
                    val entry = weeklyArr.getJSONObject(i)
                    weeklyProgressList.add(WeeklyProgress(
                        day = entry.getString("day"),
                        calories = entry.getInt("calories"),
                        duration = entry.getInt("duration")
                    ))
                }
            }
            return AnalyticsData(weeklyProgressList, emptyMap(), emptyList(), bmi)
        } catch (e: Exception) {
            return AnalyticsData(emptyList(), emptyMap(), emptyList(), 0.0)
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
        cachePreferences.edit().clear().apply()
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
        val currentDate = java.util.Calendar.getInstance()
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

    suspend fun getUnreadNotificationCount(): Int {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.getUnreadNotificationCount(context, getUserId()) { count ->
                continuation.resume(count)
            }
        }
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