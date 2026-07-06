package com.example.fitnessapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivityAddWorkoutBinding
import com.example.fitnessapp.models.ActivityType
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.services.TrackerService
import com.example.fitnessapp.viewmodels.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

class AddWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddWorkoutBinding
    private lateinit var mainRepository: MainRepository
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var isTracking = false

    // Real-time tracking data
    private var trackedDistance = 0.0
    private var trackedCalories = 0
    private var trackedDuration = 0
    private var trackedSteps = 0

    private val activityViewModel: ActivityViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                mainRepository = MainRepository(this@AddWorkoutActivity)
                return ActivityViewModel(mainRepository) as T
            }
        }
    }

    private val trackingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TrackerService.ACTION_UPDATE) {
                trackedDistance = intent.getDoubleExtra("distance", 0.0)
                trackedCalories = intent.getIntExtra("calories", 0)
                trackedDuration = intent.getIntExtra("duration", 0)
                trackedSteps = intent.getIntExtra("steps", 0)
                updateTrackingUI()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainRepository = MainRepository(this)
        activityViewModel.init(this)

        setupToolbar()
        setupSpinner()
        setupDatePicker()
        setupTimePicker()
        setupDynamicFields()
        setupTrackingUI()
        setupObservers()
        setupSaveButton()

        // Register tracking receiver with proper flag
        val filter = IntentFilter(TrackerService.ACTION_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(trackingReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(trackingReceiver, filter)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val activities = ActivityType.entries.map { it.displayName }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, activities)
        (binding.activityTypeSpinner as? AutoCompleteTextView)?.setAdapter(adapter)

        binding.activityTypeSpinner.setOnItemClickListener { _, _, position, _ ->
            val selected = ActivityType.entries[position]
            updateDynamicFields(selected.displayName)
            updateTrackingUI(selected)
        }
    }

    private fun setupDynamicFields() {
        binding.distanceLayout.visibility = View.GONE
        binding.speedLayout.visibility = View.GONE
        binding.exerciseNameLayout.visibility = View.GONE
        binding.setsLayout.visibility = View.GONE
        binding.weightLiftedLayout.visibility = View.GONE
        binding.stepsLayout.visibility = View.GONE
        binding.intensityLayout.visibility = View.GONE
        binding.trackingLayout.visibility = View.GONE
    }

    private fun updateDynamicFields(activityType: String) {
        setupDynamicFields()

        val selected = ActivityType.entries.find { it.displayName == activityType }
        selected?.let {
            if (it.requiresTracking) {
                binding.trackingLayout.visibility = View.VISIBLE
                binding.durationLayout.visibility = View.GONE
                binding.caloriesLayout.visibility = View.GONE
                binding.distanceLayout.visibility = View.GONE
                binding.speedLayout.visibility = View.GONE
                binding.stepsLayout.visibility = View.GONE
            } else {
                binding.durationLayout.visibility = View.VISIBLE
                binding.caloriesLayout.visibility = View.VISIBLE
                binding.trackingLayout.visibility = View.GONE

                when (activityType) {
                    "Weightlifting", "Strength" -> {
                        binding.exerciseNameLayout.visibility = View.VISIBLE
                        binding.setsLayout.visibility = View.VISIBLE
                        binding.weightLiftedLayout.visibility = View.VISIBLE
                    }
                    "Yoga", "Meditation", "Pilates", "Kickboxing" -> {
                        // Intensity layout should be VISIBLE for these activities
                        binding.intensityLayout.visibility = View.VISIBLE
                        // Fill intensity spinner
                        setupIntensitySpinner()
                    }
                    "Treadmill" -> {
                        binding.distanceLayout.visibility = View.VISIBLE
                        binding.speedLayout.visibility = View.VISIBLE
                    }
                    else -> {
                        // For other activities, hide intensity
                        binding.intensityLayout.visibility = View.GONE
                    }
                }
            }
        }
    }

    // Add this function to setup intensity spinner
    private fun setupIntensitySpinner() {
        val intensities = arrayOf("Beginner", "Intermediate", "Advanced")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intensities)
        (binding.intensityInput as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupTrackingUI() {
        binding.trackingLayout.visibility = View.GONE
        binding.btnStartTracking.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }

    private fun updateTrackingUI(selected: ActivityType? = null) {
        binding.tvTrackedDistance.text = String.format(Locale.getDefault(), "%.2f km", trackedDistance)
        binding.tvTrackedCalories.text = "$trackedCalories kcal"
        binding.tvTrackedDuration.text = "$trackedDuration min"
        binding.tvTrackedSteps.text = "$trackedSteps steps"

        if (selected?.requiresTracking == true) {
            binding.trackingLayout.visibility = View.VISIBLE
        }
    }

    private fun startTracking() {
        val activityType = binding.activityTypeSpinner.text.toString()
        if (activityType.isEmpty()) {
            Toast.makeText(this, "Please select an activity first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1001
                )
                return
            }
        }

        val intent = Intent(this, TrackerService::class.java)
        intent.action = TrackerService.ACTION_START
        intent.putExtra("activity_type", activityType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isTracking = true
        binding.btnStartTracking.text = "Stop Tracking"
        binding.btnStartTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.error))
        Toast.makeText(this, "Tracking started for $activityType", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        val intent = Intent(this, TrackerService::class.java)
        intent.action = TrackerService.ACTION_STOP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isTracking = false
        binding.btnStartTracking.text = "Start Tracking"
        binding.btnStartTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun setupDatePicker() {
        binding.dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = "$year-${month + 1}-$day"
                    binding.dateInput.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePicker() {
        binding.timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                binding.timeInput.setText(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    private fun setupObservers() {
        activityViewModel.saveResult.observe(this) { success ->
            binding.progressBar.visibility = View.GONE
            binding.saveButton.isEnabled = true
            if (success) {
                Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                binding.errorText.text = "Failed to save workout. Please try again."
                binding.errorText.visibility = View.VISIBLE
            }
        }

        activityViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.saveButton.isEnabled = false
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val activityType = binding.activityTypeSpinner.text.toString()
            val selected = ActivityType.entries.find { it.displayName == activityType }

            if (activityType.isEmpty()) {
                binding.errorText.text = "Please select an activity"
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (selectedDate.isEmpty()) {
                binding.errorText.text = "Please select a date"
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Get user weight for calorie calculation
            val userProfile = mainRepository.getUserProfile()
            val weight = userProfile?.weight?.toDouble() ?: 70.0

            val duration = binding.durationInput.text.toString().toIntOrNull() ?: 0

            // Calculate calories based on activity type
            fun calculateCalories(activityType: String, durationMinutes: Int, weightKg: Double): Int {
                val durationHours = durationMinutes.toDouble() / 60.0
                val met = when (activityType) {
                    "Running" -> 9.8
                    "Cycling" -> 7.5
                    "Walking" -> 3.5
                    "Weightlifting" -> 6.0
                    "Yoga" -> 4.0
                    "Meditation" -> 2.5
                    "Strength" -> 6.0
                    "Pilates" -> 4.0
                    "Kickboxing" -> 8.0
                    "Treadmill" -> 7.0
                    else -> 5.0
                }
                return (met * 3.5 * weightKg * durationHours / 0.2).toInt()
            }

            val calories = calculateCalories(activityType, duration, weight)

            // Display calories in the calories input field (make sure it's editable or displayed)
            binding.caloriesInput.setText(calories.toString())

            // Get intensity value
            val intensity = binding.intensityInput.text?.toString()?.takeIf { it.isNotEmpty() }

            val workout = Workout(
                id = 0,
                userId = mainRepository.getUserId(),
                activityType = activityType,
                durationMinutes = duration,
                caloriesBurned = calories,
                distanceKm = binding.distanceInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                speedKmh = binding.speedInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                exerciseName = binding.exerciseNameInput.text?.toString()?.takeIf { it.isNotEmpty() },
                sets = binding.setsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toIntOrNull(),
                reps = binding.repsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toIntOrNull(),
                weightLiftedKg = binding.weightLiftedInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                intensity = intensity,
                notes = binding.notesInput.text?.toString()?.takeIf { it.isNotEmpty() },
                workoutDate = try { dateFormat.parse(selectedDate) ?: Date() } catch (e: Exception) { Date() },
                workoutTime = selectedTime,
                isTracked = false
            )

            activityViewModel.addWorkout(workout)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTracking()
        } else {
            Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(trackingReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}