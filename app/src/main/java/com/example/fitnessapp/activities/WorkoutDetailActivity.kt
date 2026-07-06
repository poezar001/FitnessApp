package com.example.fitnessapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivityWorkoutDetailBinding
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        displayWorkoutDetails()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun displayWorkoutDetails() {
        // Get workout data from intent
        val workout = intent.getSerializableExtra("workout_data") as? Workout

        workout?.let {
            // Display workout details
            binding.tvActivityType.text = it.activityType

            // Format and display date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(it.workoutDate)

            // Display time
            binding.tvTime.text = it.workoutTime ?: "N/A"

            // Display duration
            binding.tvDuration.text = "${it.durationMinutes} mins"

            // Display calories
            binding.tvCalories.text = "${it.caloriesBurned} kcal"

            // Display distance if available
            if (it.distanceKm != null && it.distanceKm > 0.0) {
                binding.tvDistance.text = String.format("%.2f km", it.distanceKm)
                binding.distanceLayout.visibility = android.view.View.VISIBLE
            } else {
                binding.distanceLayout.visibility = android.view.View.GONE
            }

            // Display intensity if available
            if (!it.intensity.isNullOrEmpty()) {
                // You can add a field for intensity if needed
            }

            // Display notes
            binding.tvNotes.text = it.notes ?: "No notes"

        } ?: run {
            // If no workout data, show default or finish
            binding.tvActivityType.text = "No workout data"
        }
    }
}