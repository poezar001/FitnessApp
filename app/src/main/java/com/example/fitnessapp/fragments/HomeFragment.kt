package com.example.fitnessapp.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.AddWorkoutActivity
import com.example.fitnessapp.activities.GoalSettingActivity
import com.example.fitnessapp.base.BaseFragment
import com.example.fitnessapp.adapters.WorkoutAdapter
import com.example.fitnessapp.databinding.FragmentHomeBinding
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.ChartUtils
import com.example.fitnessapp.viewmodels.HomeViewModel
import java.util.Calendar

class HomeFragment : BaseFragment<FragmentHomeBinding>(R.layout.fragment_home) {

    private lateinit var viewModel: HomeViewModel
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun initViewModel() {
        val context = requireContext()
        val repository = MainRepository(context)
        val sharedPrefs = context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(repository, sharedPrefs) as T
            }
        }).get(HomeViewModel::class.java)
    }

    override fun setupUI() {
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        // Set LayoutManager for RecyclerView
        binding.todayWorkoutsRecycler.layoutManager = LinearLayoutManager(requireContext())

        workoutAdapter = WorkoutAdapter(emptyList()) { workout ->
            showToast("Selected: ${workout.activityType}")
        }
        binding.todayWorkoutsRecycler.adapter = workoutAdapter
    }

    private fun setupClickListeners() {
        binding.btnAddWorkout.setOnClickListener {
            startActivity(Intent(requireContext(), AddWorkoutActivity::class.java))
        }

        binding.btnStartRunning.setOnClickListener {
            showToast("Starting running tracking...")
        }

        binding.btnSetGoal.setOnClickListener {
            startActivity(Intent(requireContext(), GoalSettingActivity::class.java))
        }
    }

    override fun setupObservers() {
        // Daily stats observer
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.caloriesValue.text = it.caloriesBurned.toString()
                binding.stepsValue.text = it.steps.toString()
                binding.timeValue.text = it.workoutMinutes.toString()
            }
        }

        // Today workouts observer - Shows REAL today's activities
        viewModel.todayWorkouts.observe(viewLifecycleOwner) { workouts ->
            workouts?.let {
                workoutAdapter.updateData(it)
            }
        }

        // Username observer
        viewModel.username.observe(viewLifecycleOwner) { username ->
            username?.let {
                binding.welcomeText.text = "Good ${getTimeOfDay()}, $username!"
            }
        }

        // Loading observer
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) {
                // Show loading indicator if you have one
            }
        }

        // Goal Progress observer
        viewModel.goalProgress.observe(viewLifecycleOwner) { goalProgress ->
            if (goalProgress != null) {
                binding.goalProgressText.text = "${goalProgress.progress}% completed"

                when (goalProgress.goalType) {
                    "Weight" -> {
                        binding.remainingText.text = String.format("%.1f %s to go!",
                            goalProgress.remaining,
                            goalProgress.unit)
                    }
                    else -> {
                        binding.remainingText.text = "${goalProgress.remaining.toInt()} ${goalProgress.unit} to go!"
                    }
                }

                // Update progress chart
                ChartUtils.setupPieChart(
                    binding.progressChart,
                    goalProgress.progress.toFloat(),
                    Color.parseColor("#00FF00")
                )
            } else {
                binding.goalProgressText.text = "No active goal"
                binding.remainingText.text = "Set a goal to start!"
                ChartUtils.setupPieChart(
                    binding.progressChart,
                    0f,
                    Color.parseColor("#CCCCCC")
                )
            }
        }
    }

    override fun loadData() {
        viewModel.loadData()
    }

    private fun getTimeOfDay(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> "Evening"
        }
    }
}