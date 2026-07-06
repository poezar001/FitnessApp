package com.example.fitnessapp.models

import java.io.Serializable
import java.util.Date

data class Workout(
    val id: Int,
    val userId: Int,
    val activityType: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val distanceKm: Double? = null,
    val speedKmh: Double? = null,
    val exerciseName: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightLiftedKg: Double? = null,
    val steps: Int? = null,
    val notes: String? = null,
    val workoutDate: Date,
    val workoutTime: String? = null,
    val intensity: String? = null,  // For Yoga, Meditation
    val isTracked: Boolean = false  // True for real-time tracked workouts
): Serializable

data class WorkoutSummary(
    val totalWorkouts: Int,
    val totalCalories: Int,
    val totalDuration: Int,
    val totalDistance: Double
)

data class WeeklyProgress(
    val day: String,
    val calories: Int,
    val duration: Int
)

enum class ActivityType(val displayName: String, val requiresTracking: Boolean) {
    RUNNING("Running", true),
    CYCLING("Cycling", true),
    WALKING("Walking", true),
    WEIGHTLIFTING("Weightlifting", false),
    YOGA("Yoga", false),
    MEDITATION("Meditation", false),
    STRENGTH("Strength", false),
    PILATES("Pilates", false),
    KICKBOXING("Kickboxing", false),
    TREADMILL("Treadmill", false)
}