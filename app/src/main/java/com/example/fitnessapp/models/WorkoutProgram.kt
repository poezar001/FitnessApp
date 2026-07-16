package com.example.fitnessapp.models

data class WorkoutProgram(
    val title: String,
    val description: String,
    val episodes: String,
    val imageResId: Int,
    val videoUrl: String = "",
    val duration: String = "10 min",
    val level:String = "Beginner",
    val activityType: String,
    val approxBurn: Int = 200,
    val approxDistance: Double = 0.0,
    var calculatedTargetText: String = ""
)