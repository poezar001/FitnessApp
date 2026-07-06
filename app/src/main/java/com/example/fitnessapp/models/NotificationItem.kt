package com.example.fitnessapp.models

import java.util.Date

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: Date,
    val isRead: Boolean = false,
    val type: String = "general"
)

data class NotificationResponse(
    val success: Boolean,
    val notifications: List<NotificationItem>? = null,
    val message: String = ""
)