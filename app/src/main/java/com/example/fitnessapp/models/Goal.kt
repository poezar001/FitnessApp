package com.example.fitnessapp.models

data class Goal(
    val id: Int,
    val type: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val progress: Double = 0.0,
    val targetDate: String = "",
    val status: String = "Active",
    val startWeight: Double? = null
)