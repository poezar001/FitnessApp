package com.example.fitnessapp.models

data class ActivityTypeUI(
    val name: String,
    val iconResId: Int,
    var isSelected: Boolean = false  // Add selection state
)