package com.example.fitnessapp.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.databinding.ActivityAddWorkoutBinding
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.CalorieCalculator
import com.example.fitnessapp.viewmodels.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

class AddWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddWorkoutBinding
    private lateinit var mainRepository: MainRepository
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private val activityViewModel: ActivityViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                mainRepository = MainRepository(this@AddWorkoutActivity)
                return ActivityViewModel(mainRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinner()
        setupDatePicker()
        setupTimePicker()
        setupDynamicFields()
        setupObservers()
        setupSaveButton()
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

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val activities = arrayOf("Running", "Cycling", "Weightlifting", "Walking", "Yoga")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, activities)
        (binding.activityTypeSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(adapter)

        binding.activityTypeSpinner.setOnItemClickListener { _, _, position, _ ->
            updateDynamicFields(activities[position])
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
    }

    private fun updateDynamicFields(activityType: String) {
        setupDynamicFields()
        when (activityType) {
            "Running", "Cycling" -> {
                binding.distanceLayout.visibility = View.VISIBLE
                binding.speedLayout.visibility = View.VISIBLE
            }
            "Weightlifting" -> {
                binding.exerciseNameLayout.visibility = View.VISIBLE
                binding.setsLayout.visibility = View.VISIBLE
                binding.weightLiftedLayout.visibility = View.VISIBLE
            }
            "Walking" -> {
                binding.distanceLayout.visibility = View.VISIBLE
                binding.stepsLayout.visibility = View.VISIBLE
            }
            "Yoga" -> binding.intensityLayout.visibility = View.VISIBLE
        }
    }

    private fun setupDatePicker() {
        binding.dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "$year-${month + 1}-$day"
                binding.dateInput.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupTimePicker() {
        binding.timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                binding.timeInput.setText(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val activityType = binding.activityTypeSpinner.text.toString()
            val duration = binding.durationInput.text.toString()

            if (activityType.isEmpty() || duration.isEmpty() || selectedDate.isEmpty()) {
                binding.errorText.text = "Please fill all required fields"
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Get user weight from repository
            val userProfile = mainRepository.getUserProfile()
            val weight = userProfile?.weight ?: 70f

            // Get distance and speed if available
            val distance = binding.distanceInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDouble()
            val speed = binding.speedInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDouble()

            // Auto-calculate calories
            val calories = CalorieCalculator.calculateCalories(
                activityType = activityType,
                durationMinutes = duration.toInt(),
                weightKg = weight,
                distanceKm = distance,
                speedKmh = speed
            )

            // Display calculated calories to user
            binding.caloriesInput.setText(calories.toString())

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val workout = Workout(
                id = 0,
                userId = mainRepository.getUserId(),
                activityType = activityType,
                durationMinutes = duration.toInt(),
                caloriesBurned = calories,
                distanceKm = distance,
                speedKmh = speed,
                exerciseName = binding.exerciseNameInput.text?.toString()?.takeIf { it.isNotEmpty() },
                sets = binding.setsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toInt(),
                reps = binding.repsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toInt(),
                weightLiftedKg = binding.weightLiftedInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toDouble(),
                steps = binding.stepsInput.text?.toString()?.takeIf { it.isNotEmpty() }?.toInt(),
                notes = binding.notesInput.text?.toString()?.takeIf { it.isNotEmpty() },
                workoutDate = dateFormat.parse(selectedDate) ?: Date(),
                workoutTime = selectedTime
            )

            activityViewModel.addWorkout(workout)
        }
    }
}