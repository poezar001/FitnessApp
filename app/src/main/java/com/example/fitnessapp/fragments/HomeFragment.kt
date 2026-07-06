package com.example.fitnessapp.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.AddWorkoutActivity
import com.example.fitnessapp.activities.GoalSettingActivity
import com.example.fitnessapp.activities.WorkoutDetailActivity
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
        binding.todayWorkoutsRecycler.layoutManager = LinearLayoutManager(requireContext())

        workoutAdapter = WorkoutAdapter(emptyList()) { workout ->
            // Navigate to WorkoutDetailActivity
            val intent = Intent(requireContext(), WorkoutDetailActivity::class.java)
            intent.putExtra("workout_data", workout)
            startActivity(intent)
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
        viewModel.activeGoal.observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                when (goal.type) {
                    "Weight" -> {
                        val currentWeight = goal.currentValue
                        val targetWeight = goal.targetValue
                        val progress = goal.progress.toInt()
                        val toLose = (currentWeight - targetWeight).coerceAtLeast(0.0)

                        binding.goalProgressText.text = "$progress% completed"
                        binding.remainingText.text = if (toLose > 0) {
                            String.format("%.1f kg to go!", toLose)
                        } else {
                            "🎉 Goal achieved!"
                        }

                        ChartUtils.setupPieChart(
                            binding.progressChart,
                            progress.toFloat(),
                            Color.parseColor("#00FF00")
                        )
                    }
                    else -> {
                        // Handle other goal types
                        val progress = goal.progress.toInt()
                        val remaining = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)
                        binding.goalProgressText.text = "$progress% completed"
                        binding.remainingText.text = String.format("%.1f %s to go!", remaining, goal.unit)
                        ChartUtils.setupPieChart(
                            binding.progressChart,
                            progress.toFloat(),
                            Color.parseColor("#00FF00")
                        )
                    }
                }
            }
        }

        // ====== DAILY STATS OBSERVER ======
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.caloriesValue.text = it.caloriesBurned.toString()
                binding.stepsValue.text = it.steps.toString()
                binding.timeValue.text = it.workoutMinutes.toString()
            }
        }

        // ====== TODAY WORKOUTS OBSERVER ======
        viewModel.todayWorkouts.observe(viewLifecycleOwner) { workouts ->
            workouts?.let {
                workoutAdapter.updateData(it)
            }
        }

        // ====== USERNAME OBSERVER ======
        viewModel.username.observe(viewLifecycleOwner) { username ->
            username?.let {
                binding.welcomeText.text = "Good ${getTimeOfDay()}, $username!"
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