package com.example.fitnessapp.utils

object CalorieCalculator {

    // MET values for different activities
    private val metValues = mapOf(
        "Running" to 9.8,
        "Cycling" to 7.5,
        "Weightlifting" to 6.0,
        "Walking" to 3.5,
        "Yoga" to 4.0
    )

    fun calculateCalories(
        activityType: String,
        durationMinutes: Int,
        weightKg: Float,
        distanceKm: Double? = null,
        speedKmh: Double? = null
    ): Int {
        var met = when (activityType) {
            "Running" -> 9.8
            "Cycling" -> 7.5
            "Weightlifting" -> 6.0
            "Walking" -> 3.5
            "Yoga" -> 4.0
            else -> 5.0
        }

        // Adjust MET based on speed if available for Running/Cycling
        if (speedKmh != null && speedKmh > 0) {
            met = when (activityType) {
                "Running" -> {
                    when {
                        speedKmh < 8 -> 6.0
                        speedKmh < 10 -> 9.8
                        speedKmh < 12 -> 11.5
                        else -> 12.8
                    }
                }
                "Cycling" -> {
                    when {
                        speedKmh < 15 -> 5.8
                        speedKmh < 20 -> 8.0
                        speedKmh < 25 -> 10.0
                        else -> 12.0
                    }
                }
                else -> met
            }
        }

        // Formula: Calories = MET × 3.5 × weight (kg) × duration (hours) / 200
        val durationHours = durationMinutes / 60.0
        val calories = (met * 3.5 * weightKg * durationHours / 0.2).toInt()

        // Adjust for distance if provided and results in higher calories
        return if (distanceKm != null && distanceKm > 0) {
            maxOf(calories, (weightKg * distanceKm * 0.75).toInt())
        } else {
            calories
        }
    }

    fun getCaloriesPerHour(activityType: String, weightKg: Float): Int {
        val met = metValues[activityType] ?: 5.0
        return ((met * 3.5 * weightKg) / 0.2).toInt()
    }
}