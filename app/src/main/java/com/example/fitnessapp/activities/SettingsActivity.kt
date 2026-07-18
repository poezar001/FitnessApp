package com.example.fitnessapp.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.fitnessapp.NotificationReceiver
import com.example.fitnessapp.databinding.ActivitySettingsBinding
import com.example.fitnessapp.repository.MainRepository
import java.util.Calendar

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
        val isNotificationsEnabled = sharedPref.getBoolean("workout_reminders_enabled", false)

        binding.notificationSwitch.isChecked = isNotificationsEnabled

        binding.notificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)

                TimePickerDialog(
                    this,
                    { _, selectedHour, selectedMinute ->
                        // Save states to SharedPreferences
                        sharedPref.edit().apply {
                            putBoolean("workout_reminders_enabled", true)
                            putInt("reminder_hour", selectedHour)
                            putInt("reminder_minute", selectedMinute)
                            apply()
                        }

                        // Schedule the real broadcast intent
                        scheduleOfflineReminder(selectedHour, selectedMinute)

                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        Toast.makeText(
                            this,
                            "Daily reminder set offline for $formattedTime",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    currentHour,
                    currentMinute,
                    false
                ).apply {
                    setOnCancelListener {
                        buttonView.isChecked = false
                    }
                    show()
                }
            } else {
                sharedPref.edit().apply {
                    putBoolean("workout_reminders_enabled", false)
                    remove("reminder_hour")
                    remove("reminder_minute")
                    apply()
                }
                cancelOfflineReminder()
                Toast.makeText(this, "Workout reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleOfflineReminder(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Test trigger: exactly 10 seconds from right now
        val triggerTime = System.currentTimeMillis() + 10000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun cancelOfflineReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
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