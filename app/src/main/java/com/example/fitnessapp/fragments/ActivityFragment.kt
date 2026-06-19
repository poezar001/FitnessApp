package com.example.fitnessapp.fragments

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.AddWorkoutActivity
import com.example.fitnessapp.base.BaseFragment
import com.example.fitnessapp.adapters.WorkoutAdapter
import com.example.fitnessapp.databinding.FragmentActivityBinding
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.viewmodels.ActivityViewModel
import com.google.android.material.chip.Chip
import androidx.recyclerview.widget.LinearLayoutManager

class ActivityFragment : BaseFragment<FragmentActivityBinding>(R.layout.fragment_activity) {

    private lateinit var viewModel: ActivityViewModel
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun initViewModel() {
        val repository = MainRepository(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ActivityViewModel(repository) as T
            }
        }).get(ActivityViewModel::class.java)
    }

    override fun setupUI() {
        setupRecyclerView()
        setupChipListeners()
        setupFilterListeners()
        setupFab()
    }

    private fun setupRecyclerView() {
        binding.workoutsRecycler.layoutManager = LinearLayoutManager(requireContext())
        workoutAdapter = WorkoutAdapter(emptyList()) { workout ->
            showToast("Viewing: ${workout.activityType}")
        }
        binding.workoutsRecycler.adapter = workoutAdapter
    }

    private fun setupChipListeners() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("All")
        }
        binding.chipRunning.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Running")
        }
        binding.chipCycling.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Cycling")
        }
        binding.chipWeightlifting.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Weightlifting")
        }
        binding.chipWalking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Walking")
        }
        binding.chipYoga.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Yoga")
        }
    }

    private fun setupFilterListeners() {
        binding.filterToday.setOnClickListener {
            viewModel.setFilter("Today")
        }
        binding.filterWeekly.setOnClickListener {
            viewModel.setFilter("Weekly")
        }
        binding.filterMonthly.setOnClickListener {
            viewModel.setFilter("Monthly")
        }
    }

    private fun setupFab() {
        binding.fabAddWorkout.setOnClickListener {
            startActivity(Intent(requireContext(), AddWorkoutActivity::class.java))
        }
    }

    override fun setupObservers() {
        viewModel.filteredWorkouts.observe(viewLifecycleOwner) { workouts ->
            workoutAdapter.updateData(workouts)
        }

        viewModel.workoutSummary.observe(viewLifecycleOwner) { summary ->
            binding.totalWorkoutsValue.text = summary.totalWorkouts.toString()
            binding.totalCaloriesValue.text = summary.totalCalories.toString()
            val hours = summary.totalDuration / 60
            binding.totalHoursValue.text = hours.toString()
        }
    }

    override fun loadData() {
        val repository = MainRepository(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ActivityViewModel(repository) as T
            }
        }).get(ActivityViewModel::class.java)

        viewModel.loadWorkouts()
    }
}