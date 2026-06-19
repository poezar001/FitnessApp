package com.example.fitnessapp.models

data class DailyStats(
    val steps: Int,
    val caloriesBurned: Int,
    val workoutMinutes: Int,
    val distanceKm: Double
)