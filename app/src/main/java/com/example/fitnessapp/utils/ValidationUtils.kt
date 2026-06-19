package com.example.fitnessapp.utils

import java.util.regex.Pattern

object ValidationUtils {

    // Email validation
    fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)\$"
        )
        return emailPattern.matcher(email).matches()
    }

    // Password strength checker (8+ chars, uppercase, lowercase, number, special)
    fun checkPasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) {
            return PasswordStrength.WEAK
        }

        var hasUppercase = false
        var hasLowercase = false
        var hasDigit = false
        var hasSpecialChar = false

        for (char in password) {
            when {
                char.isUpperCase() -> hasUppercase = true
                char.isLowerCase() -> hasLowercase = true
                char.isDigit() -> hasDigit = true
                !char.isLetterOrDigit() -> hasSpecialChar = true
            }
        }

        val score = listOf(hasUppercase, hasLowercase, hasDigit, hasSpecialChar).count { it }

        return when (score) {
            4 -> PasswordStrength.STRONG
            3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    fun getPasswordStrengthMessage(strength: PasswordStrength): String {
        return when (strength) {
            PasswordStrength.WEAK -> "Weak - Use 8+ chars with uppercase, lowercase, number & special character"
            PasswordStrength.MEDIUM -> "Medium - Good, but could be stronger"
            PasswordStrength.STRONG -> "Strong - Excellent password!"
        }
    }

    fun getPasswordStrengthColor(strength: PasswordStrength): Int {
        return when (strength) {
            PasswordStrength.WEAK -> android.graphics.Color.parseColor("#FF3B30")
            PasswordStrength.MEDIUM -> android.graphics.Color.parseColor("#FF9500")
            PasswordStrength.STRONG -> android.graphics.Color.parseColor("#34C759")
        }
    }

    fun getPasswordStrengthProgress(strength: PasswordStrength): Int {
        return when (strength) {
            PasswordStrength.WEAK -> 1
            PasswordStrength.MEDIUM -> 2
            PasswordStrength.STRONG -> 4
        }
    }
}

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG
}