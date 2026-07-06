package com.example.fitnessapp.models

data class Trainer(
    val name: String,
    val role: String,
    val imageResId: Int,
    val quote: String = "",
    val experience: String = ""
)