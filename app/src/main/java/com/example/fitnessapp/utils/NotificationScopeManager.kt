package com.example.fitnessapp.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NotificationScopeManager {
    // Use GlobalScope - it never gets cancelled
    fun launch(block: suspend () -> Unit) {
        GlobalScope.launch {
            try {
                block()
            } catch (e: Exception) {
                android.util.Log.e("NotificationScope", "Error in notification: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}