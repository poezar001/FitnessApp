package com.example.fitnessapp.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NotificationScopeManager {
    fun launch(block: suspend () -> Unit) {
        GlobalScope.launch {
            try {
                block()
            } catch (e: Exception) {
                android.util.Log.e("NotificationScope", "Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}