package com.example.fitnessapp.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnessapp.databinding.ActivityGoalSettingBinding
import com.example.fitnessapp.repository.MainRepository
import kotlinx.coroutines.launch
import java.util.*

class GoalSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalSettingBinding
    private lateinit var mainRepository: MainRepository
    private var selectedDate: String = ""
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainRepository = MainRepository(this)

        setupToolbar()
        setupSpinner()
        setupDatePicker()
        setupSaveButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val goalTypes = arrayOf("Calories", "Weight", "Distance", "Workouts")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, goalTypes)
        (binding.goalTypeSpinner as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.targetDateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "$year-${month + 1}-$day"
                binding.targetDateInput.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (isSaving) return@setOnClickListener

            val goalType = binding.goalTypeSpinner.text.toString()
            val targetValue = binding.targetValueInput.text.toString()
            val unit = when (goalType) {
                "Calories" -> "kcal"
                "Weight" -> "kg"
                "Distance" -> "km"
                "Workouts" -> "workouts"
                else -> ""
            }

            if (goalType.isEmpty() || targetValue.isEmpty() || selectedDate.isEmpty()) {
                binding.errorText.text = "Please fill all fields"
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            isSaving = true
            binding.progressBar.visibility = View.VISIBLE
            binding.saveButton.isEnabled = false

            lifecycleScope.launch {
                val success = mainRepository.setGoal(goalType, targetValue.toDouble(), unit, selectedDate)
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                isSaving = false

                if (success) {
                    // Clear out the tracking cache so the dashboard/service forces a new evaluation
                    val prefs = getSharedPreferences("goal_tracking", MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    Toast.makeText(this@GoalSettingActivity, "Goal set successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.errorText.text = "Failed to set goal. Please try again."
                    binding.errorText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupGoalProgress() {
        lifecycleScope.launch {
            val goals = mainRepository.getActiveGoals()
            goals.forEach { goal ->
                val currentValue = when (goal.type) {
                    "Calories" -> mainRepository.getTotalCaloriesThisWeek()
                    "Distance" -> mainRepository.getTotalDistanceThisWeek()
                    "Workouts" -> mainRepository.getTotalWorkoutsThisWeek().toDouble()
                    else -> goal.currentValue
                }
                val progress = ((currentValue / goal.targetValue) * 100).toInt().coerceIn(0, 100)
                showGoalProgress(goal.type, progress, currentValue, goal.targetValue, goal.unit)
            }
        }
    }

    private fun showGoalProgress(type: String, progress: Int, current: Double, target: Double, unit: String) {
        // Display progress in UI
        // This could be a progress bar or text view
    }
}