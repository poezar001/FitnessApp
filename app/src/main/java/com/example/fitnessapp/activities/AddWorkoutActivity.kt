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
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
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
import com.example.fitnessapp.utils.NetworkUtils
import com.example.fitnessapp.viewmodels.ActivityViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.text.SimpleDateFormat
import java.util.*

class AddWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddWorkoutBinding
    private lateinit var mainRepository: MainRepository
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var isTracking = false
    private var isReceiverRegistered = false

    private var trackedDistance = 0.0
    private var trackedCalories = 0
    private var trackedDuration = 0
    private var trackedSteps = 0

    private var googleMap: GoogleMap? = null
    private var currentRoutePoints = ArrayList<LatLng>()
    private val mainHandler = Handler(Looper.getMainLooper())

    // ============ NEW: Track when tracking started ============
    private var trackingStartTime: Date? = null

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
                android.util.Log.d("AddWorkoutActivity", "✅ Received update broadcast!")

                val distance = intent.getDoubleExtra("distance", 0.0)
                val calories = intent.getIntExtra("calories", 0)
                val duration = intent.getIntExtra("duration", 0)
                val steps = intent.getIntExtra("steps", 0)

                android.util.Log.d("AddWorkoutActivity", "Raw data - Distance: $distance, Calories: $calories, Duration: $duration, Steps: $steps")

                trackedDistance = distance
                trackedCalories = calories
                trackedDuration = duration
                trackedSteps = steps

                val points = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra("pathPoints", LatLng::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<LatLng>("pathPoints")
                }

                points?.let {
                    currentRoutePoints = it
                    drawRoutePath(it)
                }

                mainHandler.post {
                    updateTrackingMetricsUI()
                }
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

        // Initialize MapView
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            }
        }

        setupToolbar()
        setupSpinner()
        setupDatePicker()
        setupTimePicker()
        setupDynamicFields()
        setupTrackingUI()
        setupObservers()
        setupSaveButton()
        registerTrackingReceiver()
        checkNotificationPermission()

        // ============ NEW: Auto-fill today's date ============
        autoFillTodayDate()
    }

    // ============ NEW: Auto-fill today's date ============
    private fun autoFillTodayDate() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat.format(calendar.time)
        binding.dateInput.setText(selectedDate)
        android.util.Log.d("AddWorkoutActivity", "📅 Auto-filled date: $selectedDate")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerTrackingReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(TrackerService.ACTION_UPDATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(trackingReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(trackingReceiver, filter)
            }
            isReceiverRegistered = true
            android.util.Log.d("AddWorkoutActivity", "Receiver registered for ${TrackerService.ACTION_UPDATE}")
        }
    }

    private fun unregisterTrackingReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(trackingReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                android.util.Log.d("AddWorkoutActivity", "Receiver already unregistered")
            }
        }
    }

    private fun drawRoutePath(points: List<LatLng>) {
        if (points.isEmpty() || googleMap == null) return

        googleMap?.clear()
        val polylineOptions = PolylineOptions()
            .addAll(points)
            .color(ContextCompat.getColor(this, R.color.primary))
            .width(10f)

        googleMap?.addPolyline(polylineOptions)

        if (points.isNotEmpty()) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(points.last(), 16f))
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
        binding.durationLayout.visibility = View.VISIBLE

        // ============ NEW: Show/hide date/time based on activity type ============
        // By default, date/time are visible for manual activities
        binding.dateInput.isEnabled = true
        binding.timeInput.isEnabled = true
    }

    private fun updateDynamicFields(activityType: String) {
        setupDynamicFields()
        val selected = ActivityType.entries.find { it.displayName == activityType }
        selected?.let {
            if (it.requiresTracking) {
                // ============ TRACKING ACTIVITY ============
                binding.trackingLayout.visibility = View.VISIBLE
                binding.durationLayout.visibility = View.GONE
                binding.mapView.visibility = View.VISIBLE

                // ============ AUTO-FILL DATE & TIME FOR TRACKING ============
                autoFillTodayDate()
                autoFillCurrentTime()

                // Disable manual date/time editing for tracking activities
                binding.dateInput.isEnabled = false
                binding.timeInput.isEnabled = false

                // Show hint that date/time are auto-filled
                binding.dateInput.hint = "Auto (Today)"
                binding.timeInput.hint = "Auto (Now)"

                android.util.Log.d("AddWorkoutActivity", "📅 Tracking mode: Date/Time auto-filled")

            } else {
                // ============ MANUAL ACTIVITY ============
                binding.durationLayout.visibility = View.VISIBLE
                binding.trackingLayout.visibility = View.GONE
                binding.mapView.visibility = View.GONE

                // Enable manual date/time editing
                binding.dateInput.isEnabled = true
                binding.timeInput.isEnabled = true
                binding.dateInput.hint = "Select Date"
                binding.timeInput.hint = "Select Time"

                // Keep today's date as default
                autoFillTodayDate()

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

    // ============ NEW: Auto-fill current time ============
    private fun autoFillCurrentTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        selectedTime = timeFormat.format(calendar.time)
        binding.timeInput.setText(selectedTime)
        android.util.Log.d("AddWorkoutActivity", "⏰ Auto-filled time: $selectedTime")
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
                // ============ FIX: Set start time when tracking starts ============
                trackingStartTime = Date()
                startTracking()
            }
        }
    }

    private fun updateTrackingMetricsUI() {
        android.util.Log.d("AddWorkoutActivity", "Updating UI - Distance: $trackedDistance, Steps: $trackedSteps")

        // FIXED: Use binding directly to guarantee UI delivery without findViewById null hazards
        binding.tvTrackedDistance.text = String.format(Locale.getDefault(), "%.3f km", trackedDistance)
        binding.tvTrackedCalories.text = "${trackedCalories} kcal"
        binding.tvTrackedDuration.text = "${trackedDuration} min"
        binding.tvTrackedSteps.text = "$trackedSteps steps"
    }

    private fun startTracking() {
        val activityType = binding.activityTypeSpinner.text.toString()
        if (activityType.isEmpty()) {
            Toast.makeText(this, "Please select an activity first", Toast.LENGTH_SHORT).show()
            return
        }

        val permissionsNeeded = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
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
        trackingStartTime = Date() // Record start time
        binding.btnStartTracking.text = "Stop Tracking"
        binding.btnStartTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.error))

        // Reset values
        trackedDistance = 0.0
        trackedCalories = 0
        trackedDuration = 0
        trackedSteps = 0
        updateTrackingMetricsUI()

        // ============ AUTO-FILL DATE & TIME WHEN TRACKING STARTS ============
        autoFillTodayDate()
        autoFillCurrentTime()
        binding.dateInput.isEnabled = false
        binding.timeInput.isEnabled = false

        android.util.Log.d("AddWorkoutActivity", "Tracking started for: $activityType at $selectedTime")
        Toast.makeText(this, "Tracking started for $activityType!", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        val intent = Intent(this, TrackerService::class.java).apply {
            action = TrackerService.ACTION_STOP
        }
        startService(intent)
        isTracking = false
        binding.btnStartTracking.text = "Start Tracking"
        binding.btnStartTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        android.util.Log.d("AddWorkoutActivity", "Tracking stopped")
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun setupDatePicker() {
        binding.dateInput.setOnClickListener {
            // Only allow manual date selection if date input is enabled
            if (!binding.dateInput.isEnabled) {
                Toast.makeText(this, "Date is auto-filled for tracking activities", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)
                binding.dateInput.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupTimePicker() {
        binding.timeInput.setOnClickListener {
            // Only allow manual time selection if time input is enabled
            if (!binding.timeInput.isEnabled) {
                Toast.makeText(this, "Time is auto-filled for tracking activities", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                // Check network status to display the correct message
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                val isOnline = capabilities != null && (
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                        )

                if (isOnline) {
                    // Online Save Flow
                    android.util.Log.d("AddWorkoutActivity", "✅ Workout saved successfully online!")
                    Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show()

                    binding.errorText.text = "Sending notification..."
                    binding.errorText.visibility = View.VISIBLE
                    binding.errorText.setTextColor(ContextCompat.getColor(this, R.color.primary))

                    // Wait 2 seconds then finish
                    binding.saveButton.postDelayed({
                        finish()
                    }, 2000)
                } else {
                    // Offline Save Flow (Airplane Mode)
                    android.util.Log.d("AddWorkoutActivity", "📦 Workout saved to Offline Queue!")
                    Toast.makeText(this, "Workout saved successfully (Offline Queue)", Toast.LENGTH_SHORT).show()

                    // Close the screen immediately when offline so the user can look at their updated history tab
                    finish()
                }

            } else {
                android.util.Log.e("AddWorkoutActivity", "❌ Failed to save workout")
                binding.errorText.text = "Failed to save workout. Please try again."
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.setTextColor(ContextCompat.getColor(this, R.color.error))
            }
        }

        activityViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.saveButton.isEnabled = false
                binding.errorText.visibility = View.GONE
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("AddWorkoutActivity", "✅ Notification permission granted")
                }
                else -> {
                    android.util.Log.d("AddWorkoutActivity", "🔔 Requesting notification permission")
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1002
                    )
                }
            }
        }
    }

    private fun calculateCalories(activityType: String, durationMinutes: Int, weightKg: Double): Int {
        val met = when (activityType) {
            "Running" -> 9.8
            "Cycling" -> 7.5
            "Walking" -> 3.5
            "Weightlifting", "Strength" -> 6.0
            "Yoga", "Pilates" -> 4.0
            "Meditation" -> 1.5
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

            if (activityType.isEmpty() || selectedDate.isEmpty()) {
                binding.errorText.text = "Please fill all required fields"
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val userProfile = mainRepository.getUserProfile()
            val weight = userProfile?.weight?.toDouble() ?: 70.0

            val duration = if (selected?.requiresTracking == true) {
                if (trackedDuration > 0) trackedDuration else 1
            } else {
                binding.durationInput.text.toString().toIntOrNull() ?: 0
            }

            if (duration <= 0 && selected?.requiresTracking == false) {
                binding.errorText.text = "Please enter a valid duration"
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val calories = if (selected?.requiresTracking == true) trackedCalories else calculateCalories(activityType, duration, weight)

            val manualDistance = if (activityType == "Treadmill") {
                binding.distanceInput.text?.toString()?.toDoubleOrNull()
            } else null

            val manualSpeed = if (activityType == "Treadmill") {
                binding.speedInput.text?.toString()?.toDoubleOrNull()
            } else null

            val workout = Workout(
                id = 0,
                userId = mainRepository.getUserId(),
                activityType = activityType,
                durationMinutes = duration,
                caloriesBurned = calories,
                distanceKm = if (selected?.requiresTracking == true) trackedDistance else manualDistance,
                steps = if (selected?.requiresTracking == true) trackedSteps else null,
                speedKmh = if (selected?.requiresTracking == true) null else manualSpeed,
                exerciseName = if (activityType == "Weightlifting" || activityType == "Strength") binding.exerciseNameInput.text?.toString()?.takeIf { it.isNotEmpty() } else null,
                sets = if (activityType == "Weightlifting" || activityType == "Strength") binding.setsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toIntOrNull() else null,
                reps = if (activityType == "Weightlifting" || activityType == "Strength") binding.repsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toIntOrNull() else null,
                weightLiftedKg = if (activityType == "Weightlifting" || activityType == "Strength") binding.weightLiftedInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull() else null,
                intensity = if (binding.intensityLayout.visibility == View.VISIBLE) binding.intensityInput.text?.toString()?.takeIf { it.isNotEmpty() } else null,
                notes = binding.notesInput.text?.toString()?.takeIf { it.isNotEmpty() },
                workoutDate = try { dateFormat.parse(selectedDate) ?: Date() } catch (e: Exception) { Date() },
                workoutTime = selectedTime,
                isTracked = selected?.requiresTracking == true,
                routePoints = if (selected?.requiresTracking == true) convertPathPointsToJson(currentRoutePoints) else null
            )

            activityViewModel.addWorkout(workout)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startTracking()
                } else {
                    Toast.makeText(this, "Permissions required for tracking", Toast.LENGTH_SHORT).show()
                }
            }
            1002 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.d("AddWorkoutActivity", "✅ Notification permission granted!")
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.w("AddWorkoutActivity", "⚠️ Notification permission denied")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        registerTrackingReceiver()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        unregisterTrackingReceiver()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun convertPathPointsToJson(points: List<LatLng>): String? {
        if (points.isEmpty()) return null
        return com.google.gson.Gson().toJson(points)
    }
}