package com.example.fitnessapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnessapp.models.*
import com.google.gson.Gson
import org.json.JSONObject

object NetworkUtils {
    private const val BASE_URL = "http://10.0.2.2/fitness_app/"
    private lateinit var requestQueue: com.android.volley.RequestQueue

    fun init(context: Context) {
        if (!::requestQueue.isInitialized) {
            requestQueue = Volley.newRequestQueue(context)
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    fun registerUser(
        context: Context,
        username: String,
        email: String,
        password: String,
        callback: (AuthResponse) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(AuthResponse(false, "No internet connection"))
            return
        }

        val url = BASE_URL + "register.php"
        val jsonBody =
            Gson().toJson(mapOf("username" to username, "email" to email, "password" to password))

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val result = Gson().fromJson(response, AuthResponse::class.java)
                    callback(result)
                } catch (e: Exception) {
                    callback(AuthResponse(false, "Error parsing response"))
                }
            },
            { error -> callback(AuthResponse(false, "Network error: ${error.message}")) }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    fun loginUser(
        context: Context,
        email: String,
        password: String,
        callback: (AuthResponse) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(AuthResponse(false, "No internet connection"))
            return
        }

        val url = BASE_URL + "login.php"
        val jsonBody = Gson().toJson(mapOf("email" to email, "password" to password))

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val result = Gson().fromJson(response, AuthResponse::class.java)
                    callback(result)
                } catch (e: Exception) {
                    callback(AuthResponse(false, "Error parsing response"))
                }
            },
            { error -> callback(AuthResponse(false, "Network error: ${error.message}")) }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    fun saveUserProfile(
        context: Context,
        request: ProfileRequest,
        callback: (Boolean, String) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(false, "No internet connection")
            return
        }

        val url = BASE_URL + "save_profile.php"
        val jsonBody = Gson().toJson(request)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    callback(json.getBoolean("success"), json.getString("message"))
                } catch (e: Exception) {
                    callback(false, "Error parsing response")
                }
            },
            { error -> callback(false, "Network error: ${error.message}") }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        stringRequest.retryPolicy =
            DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(stringRequest)
    }

    fun getWorkouts(
        context: Context,
        userId: Int,
        filter: String,
        type: String,
        callback: (List<Workout>, WorkoutSummary) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(emptyList(), WorkoutSummary(0, 0, 0, 0.0))
            return
        }
        val filterParam = when (filter.lowercase()) {
            "today" -> "today"
            "weekly" -> "weekly"
            "monthly" -> "monthly"
            else -> "all"
        }

        val url = "${BASE_URL}get_workouts.php?user_id=$userId&filter=$filterParam&type=$type"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val workoutsArray = json.getJSONArray("workouts")
                        val workouts = mutableListOf<Workout>()
                        for (i in 0 until workoutsArray.length()) {
                            val obj = workoutsArray.getJSONObject(i)
                            workouts.add(
                                Workout(
                                    id = obj.getInt("id"),
                                    userId = obj.getInt("user_id"),
                                    activityType = obj.getString("activity_type"),
                                    durationMinutes = obj.getInt("duration_minutes"),
                                    caloriesBurned = obj.getInt("calories_burned"),
                                    distanceKm = if (obj.has("distance_km") && !obj.isNull("distance_km")) obj.getDouble(
                                        "distance_km"
                                    ) else null,
                                    speedKmh = if (obj.has("speed_kmh") && !obj.isNull("speed_kmh")) obj.getDouble(
                                        "speed_kmh"
                                    ) else null,
                                    exerciseName = if (obj.has("exercise_name") && !obj.isNull("exercise_name")) obj.getString(
                                        "exercise_name"
                                    ) else null,
                                    sets = if (obj.has("sets") && !obj.isNull("sets")) obj.getInt("sets") else null,
                                    reps = if (obj.has("reps") && !obj.isNull("reps")) obj.getInt("reps") else null,
                                    weightLiftedKg = if (obj.has("weight_lifted_kg") && !obj.isNull(
                                            "weight_lifted_kg"
                                        )
                                    ) obj.getDouble("weight_lifted_kg") else null,
                                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString(
                                        "notes"
                                    ) else null,
                                    workoutDate = java.text.SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        java.util.Locale.getDefault()
                                    ).parse(obj.getString("workout_date")) ?: java.util.Date(),
                                    workoutTime = if (obj.has("workout_time") && !obj.isNull("workout_time")) obj.getString(
                                        "workout_time"
                                    ) else null
                                )
                            )
                        }
                        val summaryObj = json.getJSONObject("summary")
                        val summary = WorkoutSummary(
                            totalWorkouts = summaryObj.getInt("total_workouts"),
                            totalCalories = summaryObj.getInt("total_calories"),
                            totalDuration = summaryObj.getInt("total_duration"),
                            totalDistance = summaryObj.getDouble("total_distance")
                        )
                        callback(workouts, summary)
                    } else {
                        callback(emptyList(), WorkoutSummary(0, 0, 0, 0.0))
                    }
                } catch (e: Exception) {
                    callback(emptyList(), WorkoutSummary(0, 0, 0, 0.0))
                }
            },
            { error -> callback(emptyList(), WorkoutSummary(0, 0, 0, 0.0)) }
        )
        requestQueue.add(request)
    }

    fun saveWorkout(context: Context, workout: Workout, callback: (Boolean, String) -> Unit) {
        if (!isNetworkAvailable(context)) {
            callback(false, "No internet connection")
            return
        }

        val url = BASE_URL + "save_workout.php"
        val jsonBody = Gson().toJson(
            mapOf(
                "user_id" to workout.userId,
                "activity_type" to workout.activityType,
                "duration_minutes" to workout.durationMinutes,
                "calories_burned" to workout.caloriesBurned,
                "distance_km" to workout.distanceKm,
                "speed_kmh" to workout.speedKmh,
                "exercise_name" to workout.exerciseName,
                "sets" to workout.sets,
                "reps" to workout.reps,
                "weight_lifted_kg" to workout.weightLiftedKg,
                "steps" to workout.steps,
                "intensity" to workout.intensity,
                "notes" to workout.notes,
                "workout_date" to java.text.SimpleDateFormat(
                    "yyyy-MM-dd",
                    java.util.Locale.getDefault()
                ).format(workout.workoutDate),
                "workout_time" to workout.workoutTime
            )
        )

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                // ADD THIS LOG: This will expose exactly what PHP is returning!
                android.util.Log.d("NetworkUtils", "RAW PHP RESPONSE: $response")

                try {
                    val json = JSONObject(response)
                    callback(json.getBoolean("success"), json.getString("message"))
                } catch (e: Exception) {
                    android.util.Log.e("NetworkUtils", "JSON Parse Exception: ${e.message}")
                    callback(false, "Error parsing response")
                }
            },
            { error ->
                android.util.Log.e("NetworkUtils", "Volley Error: ${error.message}")
                callback(false, "Network error: ${error.message}")
            }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    fun getAnalytics(
        context: Context,
        userId: Int,
        callback: (List<WeeklyProgress>, Map<String, Float>, List<ApiGoal>, Double) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(emptyList(), emptyMap(), emptyList(), 0.0)
            return
        }

        val url = "${BASE_URL}get_analytics.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)

                    if (json.getBoolean("success")) {

                        val weeklyArray = json.getJSONArray("weekly_progress")
                        val weeklyProgress = mutableListOf<WeeklyProgress>()

                        for (i in 0 until weeklyArray.length()) {
                            val obj = weeklyArray.getJSONObject(i)

                            weeklyProgress.add(WeeklyProgress(
                                day = obj.getString("day"),
                                calories = obj.getInt("total_calories"),  // Should be Int, not divided
                                duration = obj.getInt("total_duration")
                            ))
                        }

                        val distribution = mutableMapOf<String, Float>()
                        val distObj = json.getJSONObject("activity_distribution")
                        val keys = distObj.keys()

                        while (keys.hasNext()) {
                            val key = keys.next()
                            distribution[key] = distObj.getDouble(key).toFloat()
                        }

                        val goalsArray = json.getJSONArray("goals")
                        val goals = mutableListOf<ApiGoal>()

                        for (i in 0 until goalsArray.length()) {
                            val obj = goalsArray.getJSONObject(i)

                            goals.add(
                                ApiGoal(
                                    id = obj.getInt("id"),
                                    type = obj.getString("type"),
                                    targetValue = obj.getDouble("target"),
                                    currentValue = obj.getDouble("current"),
                                    unit = obj.getString("unit"),
                                    progress = obj.getDouble("progress")
                                )
                            )
                        }

                        val bmi = json.getDouble("bmi")
                        callback(weeklyProgress, distribution, goals, bmi)

                    } else {
                        callback(emptyList(), emptyMap(), emptyList(), 0.0)
                    }

                } catch (e: Exception) {
                    callback(emptyList(), emptyMap(), emptyList(), 0.0)
                }
            },

            { error ->
                callback(emptyList(), emptyMap(), emptyList(), 0.0)
            }

        )

        requestQueue.add(request)

    }




    fun setGoal(context: Context, userId: Int, goalType: String, targetValue: Double, unit: String, targetDate: String, callback: (Boolean, String) -> Unit) {
        if (!isNetworkAvailable(context)) {
            callback(false, "No internet connection")
            return
        }

        val url = BASE_URL + "set_goal.php"
        val jsonBody = Gson().toJson(mapOf(
            "user_id" to userId,
            "goal_type" to goalType,
            "target_value" to targetValue,
            "unit" to unit,
            "target_date" to targetDate
        ))

        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    callback(json.getBoolean("success"), json.getString("message"))
                } catch (e: Exception) {
                    callback(false, "Error parsing response")
                }
            },
            { error -> callback(false, "Network error: ${error.message}") }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    fun getGoals(
        context: Context,
        userId: Int,
        callback: (List<Goal>) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(emptyList())
            return
        }

        val url = "${BASE_URL}get_goals.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val goalsArray = json.getJSONArray("goals")
                        val goals = mutableListOf<Goal>()
                        for (i in 0 until goalsArray.length()) {
                            val obj = goalsArray.getJSONObject(i)
                            goals.add(
                                Goal(
                                    id = obj.getInt("id"),
                                    type = obj.getString("type"),
                                    targetValue = obj.getDouble("target"),
                                    currentValue = obj.getDouble("current"),
                                    unit = obj.getString("unit"),
                                    progress = obj.getDouble("progress"),
                                    targetDate = obj.getString("target_date"),
                                    status = obj.getString("status")
                                )
                            )
                        }
                        callback(goals)
                    } else {
                        callback(emptyList())
                    }
                } catch (e: Exception) {
                    callback(emptyList())
                }
            },
            { error -> callback(emptyList()) }
        )
        requestQueue.add(request)
    }


    fun getUnreadNotificationCount(
        context: Context,
        userId: Int,
        callback: (Int) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(0)
            return
        }

        val url = "${BASE_URL}get_notification_count.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val count = json.getInt("count")
                        callback(count)
                    } else {
                        callback(0)
                    }
                } catch (e: Exception) {
                    callback(0)
                }
            },
            { error -> callback(0) }
        )
        requestQueue.add(request)
    }

    fun saveNotification(
        context: Context,
        userId: Int,
        title: String,
        message: String,
        type: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(false, "No internet connection")
            return
        }

        val url = BASE_URL + "save_notification.php"
        val jsonBody = Gson().toJson(mapOf(
            "user_id" to userId,
            "title" to title,
            "message" to message,
            "type" to type
        ))

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    callback(json.getBoolean("success"), json.getString("message"))
                } catch (e: Exception) {
                    callback(false, "Error parsing response")
                }
            },
            { error -> callback(false, "Network error: ${error.message}") }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }



    fun resetPassword(
        context: Context,
        email: String,
        newPassword: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (!isNetworkAvailable(context)) {
            callback(false, "No internet connection")
            return
        }

        val url = BASE_URL + "reset_password.php"
        val jsonBody = Gson().toJson(mapOf(
            "email" to email,
            "new_password" to newPassword
        ))

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    callback(json.getBoolean("success"), json.getString("message"))
                } catch (e: Exception) {
                    callback(false, "Error parsing response")
                }
            },
            { error ->
                callback(false, "Network error: ${error.message}")
            }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }
}

data class ApiGoal(
    val id: Int,
    val type: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val progress: Double
)