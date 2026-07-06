package com.example.fitnessapp.models

data class WorkoutProgram(
    val title: String,
    val description: String,
    val episodes: String,
    val imageResId: Int,
    val videoUrl: String = "",
    val duration: String = "10 min"
)