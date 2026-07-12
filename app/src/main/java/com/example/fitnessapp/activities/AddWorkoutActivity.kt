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
                updateTrackingMetricsUI()
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
        }
    }

    private fun setupDynamicFields() {
        binding.treadmillLayout.visibility = View.GONE
        binding.exerciseNameLayout.visibility = View.GONE
        binding.setsLayout.visibility = View.GONE
        binding.weightLiftedLayout.visibility = View.GONE
        binding.intensityLayout.visibility = View.GONE
        binding.trackingLayout.visibility = View.GONE
        binding.durationLayout.visibility = View.GONE
    }

    private fun updateDynamicFields(activityType: String) {
        setupDynamicFields()

        val selected = ActivityType.entries.find { it.displayName == activityType }
        selected?.let {
            if (it.requiresTracking) {
                binding.trackingLayout.visibility = View.VISIBLE
            } else {
                binding.durationLayout.visibility = View.VISIBLE

                when (activityType) {
                    "Weightlifting", "Strength" -> {
                        binding.exerciseNameLayout.visibility = View.VISIBLE
                        binding.setsLayout.visibility = View.VISIBLE
                        binding.weightLiftedLayout.visibility = View.VISIBLE
                    }
                    "Yoga", "Meditation", "Pilates", "Kickboxing" -> {
                        binding.intensityLayout.visibility = View.VISIBLE
                        setupIntensitySpinner()
                    }
                    "Treadmill" -> {
                        binding.treadmillLayout.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupIntensitySpinner() {
        val intensities = arrayOf("Beginner", "Intermediate", "Advanced")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intensities)
        (binding.intensityInput as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupTrackingUI() {
        binding.btnStartTracking.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }

    private fun updateTrackingMetricsUI() {
        binding.tvTrackedDistance.text = String.format(Locale.getDefault(), "%.2f km", trackedDistance)
        binding.tvTrackedCalories.text = "$trackedCalories kcal"
        binding.tvTrackedDuration.text = "$trackedDuration min"
        binding.tvTrackedSteps.text = "$trackedSteps steps"
    }

    private fun startTracking() {
        val activityType = binding.activityTypeSpinner.text.toString()
        if (activityType.isEmpty()) {
            Toast.makeText(this, "Please select an activity first", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        val intent = Intent(this, TrackerService::class.java).apply {
            action = TrackerService.ACTION_START
            putExtra("activity_type", activityType)
        }

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
        val intent = Intent(this, TrackerService::class.java).apply {
            action = TrackerService.ACTION_STOP
        }

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

    // Moved outside to class body level context to resolve syntax constraint
    private fun calculateCalories(activityType: String, durationMinutes: Int, weightKg: Double): Int {
        val met = when (activityType) {
            "Running" -> 9.8
            "Cycling" -> 7.5
            "Walking" -> 3.5
            "Weightlifting" -> 6.0
            "Yoga" -> 4.0
            "Meditation" -> 1.5
            "Strength" -> 6.0
            "Pilates" -> 4.0
            "Kickboxing" -> 8.0
            "Treadmill" -> 7.0
            else -> 5.0
        }
        return ((met * 3.5 * weightKg) / 200 * durationMinutes).toInt()
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
            val userProfile = mainRepository.getUserProfile()
            val weight = userProfile?.weight?.toDouble() ?: 70.0

            // Conditional assignment handles GPS vs Manual fields correctly
            val duration = if (selected?.requiresTracking == true) trackedDuration else (binding.durationInput.text.toString().toIntOrNull() ?: 0)
            val calories = if (selected?.requiresTracking == true) trackedCalories else calculateCalories(activityType, duration, weight)

            val workout = Workout(
                id = 0,
                userId = mainRepository.getUserId(),
                activityType = activityType,
                durationMinutes = duration,
                caloriesBurned = calories,
                distanceKm = if (selected?.requiresTracking == true) trackedDistance else binding.distanceInput.text?.toString()?.toDoubleOrNull(),
                speedKmh = if (selected?.requiresTracking == true) null else binding.speedInput.text?.toString()?.toDoubleOrNull(),
                exerciseName = binding.exerciseNameInput.text?.toString()?.takeIf { it.isNotEmpty() },
                sets = binding.setsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toIntOrNull(),
                reps = binding.repsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toIntOrNull(),
                weightLiftedKg = binding.weightLiftedInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                intensity = binding.intensityInput.text?.toString()?.takeIf { it.isNotEmpty() },
                notes = binding.notesInput.text?.toString()?.takeIf { it.isNotEmpty() },
                workoutDate = try { dateFormat.parse(selectedDate) ?: Date() } catch (e: Exception) { Date() },
                workoutTime = selectedTime,
                isTracked = selected?.requiresTracking == true
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