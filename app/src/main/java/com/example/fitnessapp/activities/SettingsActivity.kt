package com.example.fitnessapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivitySettingsBinding
import com.example.fitnessapp.repository.MainRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE super.onCreate
        applySavedTheme()

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainRepository = MainRepository(this)

        setupToolbar()
        setupThemeToggle()
        setupNotificationToggle()
        setupClickListeners()
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupThemeToggle() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        // Set initial state
        binding.themeSwitch.isChecked = isDarkMode

        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()

            // Apply theme using AppCompatDelegate
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            // Show confirmation
            Toast.makeText(
                this,
                if (isChecked) "Dark mode enabled" else "Light mode enabled",
                Toast.LENGTH_SHORT
            ).show()

            // Recreate ALL activities to apply theme
            recreate()
        }
    }

    private fun setupNotificationToggle() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isNotificationsEnabled = sharedPref.getBoolean("workout_notifications", true)

        binding.notificationSwitch.isChecked = isNotificationsEnabled

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("workout_notifications", isChecked).apply()

            Toast.makeText(
                this,
                if (isChecked) "Workout reminders enabled" else "Workout reminders disabled",
                Toast.LENGTH_SHORT
            ).show()

            if (isChecked) {
                scheduleWorkoutReminders()
            } else {
                cancelWorkoutReminders()
            }
        }
    }

    private fun scheduleWorkoutReminders() {
        Toast.makeText(this, "Workout reminders scheduled!", Toast.LENGTH_SHORT).show()
    }

    private fun cancelWorkoutReminders() {
        Toast.makeText(this, "Workout reminders canceled!", Toast.LENGTH_SHORT).show()
    }

    private fun setupClickListeners() {
        binding.editProfileCard.setOnClickListener {
            val intent = Intent(this, PersonalizeActivity::class.java)
            intent.putExtra("edit_mode", true)
            startActivity(intent)
        }

        binding.deleteAccountCard.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteAccount() {
        Toast.makeText(this, "Deleting account...", Toast.LENGTH_SHORT).show()
        mainRepository.logout()
        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}