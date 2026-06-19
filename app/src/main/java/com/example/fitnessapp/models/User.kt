package com.example.fitnessapp.models

data class User(
    val id: Int,
    val username: String,
    val email: String
)

data class AuthRequest(
    val email: String,
    val password: String,
    val username: String? = null
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user_id: Int? = null,
    val username: String? = null,
    val email: String? = null
)

data class ProfileRequest(
    val user_id: Int,
    val birthday: String,
    val gender: String,
    val height: Float,
    val weight: Float,
    val activity_level: String
)

enum class ActivityLevel(val displayName: String, val description: String) {
    BEGINNER("Beginner", "Little to no regular exercise"),
    INTERMEDIATE("Intermediate", "Exercise 2-3 times per week"),
    ADVANCED("Advanced", "Exercise 4+ times per week or athletic training")
}

data class UserProfile(
    val userId: Int,
    val birthday: String,
    val gender: String,
    val height: Float,
    val weight: Float,
    val activityLevel: ActivityLevel
)