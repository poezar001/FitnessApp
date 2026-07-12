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

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun setupRecyclerView() {
        binding.todayWorkoutsRecycler.layoutManager = LinearLayoutManager(requireContext())

        workoutAdapter = WorkoutAdapter(emptyList()) { workout ->
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



        binding.btnSetGoal.setOnClickListener {
            startActivity(Intent(requireContext(), GoalSettingActivity::class.java))
        }
    }

    override fun setupObservers() {
        viewModel.activeGoal.observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                val progress = goal.progress.toInt()
                binding.goalProgressText.text = "$progress% completed today"

                val activeChartColor = when(goal.type) {
                    "Weight" -> Color.parseColor("#3498db")
                    "Calories" -> Color.parseColor("#2ecc71") // Energetic Green
                    "Distance" -> Color.parseColor("#e67e22")
                    else -> Color.parseColor("#9b59b6")
                }

                when (goal.type) {
                    "Weight" -> {
                        binding.tvGoalHeader.text = "Overall Weight Goal"
                        val currentWeight = goal.currentValue
                        val targetWeight = goal.targetValue
                        val toLose = (currentWeight - targetWeight).coerceAtLeast(0.0)
                        binding.remainingText.text = if (toLose > 0) String.format("%.1f kg left to target!", toLose) else "🎉 Goal achieved!"
                    }
                    "Calories" -> {
                        // FIXED: Adjusted header to match contextual tracking balance across days
                        binding.tvGoalHeader.text = "Active Target Balance"
                        val remainingCalories = goal.currentValue

                        binding.remainingText.text = if (remainingCalories > 0) {
                            String.format("%.0f kcal remaining to go!", remainingCalories)
                        } else {
                            "🎉 Target cleared! Awesome job!"
                        }
                    }
                    else -> {
                        binding.tvGoalHeader.text = "Today's ${goal.type} Goal"
                        val remaining = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)
                        val unitLabel = goal.unit ?: when(goal.type) {
                            "Distance" -> "km"
                            "Workouts" -> "workouts"
                            else -> ""
                        }
                        binding.remainingText.text = if (remaining > 0) String.format("%.1f %s to go!", remaining, unitLabel) else "🎉 Today's target reached!"
                    }
                }

                ChartUtils.setupPieChart(
                    binding.progressChart,
                    progress.toFloat(),
                    activeChartColor
                )
            } else {
                binding.tvGoalHeader.text = "Goal Tracker"
                binding.goalProgressText.text = "No active goal"
                binding.remainingText.text = "Tap 'Set Goal' to start tracking!"
                ChartUtils.setupPieChart(binding.progressChart, 0f, Color.parseColor("#333333"))
            }
        }

        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.caloriesValue.text = it.caloriesBurned.toString()
                binding.stepsValue.text = it.steps.toString()
                binding.timeValue.text = it.workoutMinutes.toString()
            }
        }

        viewModel.todayWorkouts.observe(viewLifecycleOwner) { workouts ->
            workouts?.let {
                workoutAdapter.updateData(it)
            }
        }

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