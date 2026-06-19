package com.example.fitnessapp.models

import java.util.Date

data class Achievement(
    val id: Int,
    val name: String,
    val date: Date,
    val icon: String
)