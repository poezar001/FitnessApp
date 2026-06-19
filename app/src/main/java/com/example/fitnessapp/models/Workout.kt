package com.example.fitnessapp.models

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
    val workoutTime: String? = null
)

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