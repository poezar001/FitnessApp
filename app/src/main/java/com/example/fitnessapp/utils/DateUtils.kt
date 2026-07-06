package com.example.fitnessapp.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun formatDate(date: Date): String {
        return displayDateFormat.format(date)
    }

    fun formatDateForDb(date: Date): String {
        return dateFormat.format(date)
    }

    fun formatTime(time: String?): String {
        return time ?: ""
    }

    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    fun getCurrentTime(): String {
        return timeFormat.format(Date())
    }

    fun parseDate(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun getWeekDays(): List<String> {
        return listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    fun formatTimeAgo(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}