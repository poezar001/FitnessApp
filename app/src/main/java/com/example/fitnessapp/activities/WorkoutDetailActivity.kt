package com.example.fitnessapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivityWorkoutDetailBinding

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
        val workoutData = intent.getSerializableExtra("workout_data")
        // Display workout details - update UI with data
    }
}