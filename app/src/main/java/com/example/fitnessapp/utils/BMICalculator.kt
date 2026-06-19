package com.example.fitnessapp.utils

object BMICalculator {

    fun calculateBMI(weightKg: Double, heightCm: Double): Double {
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    fun getBMIStatus(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal weight"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }

    fun getBMIColor(bmi: Double): Int {
        return when {
            bmi < 18.5 -> android.graphics.Color.parseColor("#2196F3")
            bmi < 25 -> android.graphics.Color.parseColor("#4CAF50")
            bmi < 30 -> android.graphics.Color.parseColor("#FF9800")
            else -> android.graphics.Color.parseColor("#F44336")
        }
    }
}