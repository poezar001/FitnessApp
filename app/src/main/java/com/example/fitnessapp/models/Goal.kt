package com.example.fitnessapp.models

import java.util.Date

data class Goal(
    val id: Int,
    val userId: Int,
    val type: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val startDate: Date,
    val targetDate: Date,
    val status: String
)