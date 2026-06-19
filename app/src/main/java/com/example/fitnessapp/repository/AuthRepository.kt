package com.example.fitnessapp.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.fitnessapp.models.*
import com.example.fitnessapp.utils.NetworkUtils
// Helper function for suspend coroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
class AuthRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)

    init {
        NetworkUtils.init(context)
    }

    suspend fun register(username: String, email: String, password: String): AuthResponse {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.registerUser(context, username, email, password) { response ->
                if (response.success && response.user_id != null) {
                    saveUserSession(response.user_id, username, email)
                }
                continuation.resume(response)
            }
        }
    }

    suspend fun login(email: String, password: String): AuthResponse {
        return suspendCancellableCoroutine { continuation ->
            NetworkUtils.loginUser(context, email, password) { response ->
                if (response.success && response.user_id != null) {
                    saveUserSession(response.user_id, response.username ?: "", email)
                }
                continuation.resume(response)
            }
        }
    }

    suspend fun saveProfile(profile: UserProfile): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val request = ProfileRequest(
                user_id = profile.userId,
                birthday = profile.birthday,
                gender = profile.gender,
                height = profile.height,
                weight = profile.weight,
                activity_level = profile.activityLevel.displayName
            )

            NetworkUtils.saveUserProfile(context, request) { success, message ->
                if (success) {
                    saveProfileLocally(profile)
                }
                continuation.resume(success)
            }
        }
    }

    private fun saveUserSession(userId: Int, username: String, email: String) {
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", true)
            putInt("user_id", userId)
            putString("username", username)
            putString("email", email)
            apply()
        }
    }

    private fun saveProfileLocally(profile: UserProfile) {
        sharedPreferences.edit().apply {
            putString("birthday", profile.birthday)
            putString("gender", profile.gender)
            putFloat("height", profile.height)
            putFloat("weight", profile.weight)
            putString("activity_level", profile.activityLevel.name)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean("is_logged_in", false)

    fun getUserId(): Int = sharedPreferences.getInt("user_id", -1)

    fun getUsername(): String = sharedPreferences.getString("username", "") ?: ""

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }

    fun hasUserProfile(): Boolean {
        val birthday = sharedPreferences.getString("birthday", null)
        val gender = sharedPreferences.getString("gender", null)
        val height = sharedPreferences.getFloat("height", 0f)
        val weight = sharedPreferences.getFloat("weight", 0f)

        return birthday != null && gender != null && height > 0 && weight > 0
    }
}


