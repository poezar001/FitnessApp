package com.example.fitnessapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.fitnessapp.utils.NetworkUtils

class FitnessApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply saved theme when app starts
        applySavedTheme()

        NetworkUtils.init(this)
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}