package com.example.fitnessapp.fragments

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.AddWorkoutActivity
import com.example.fitnessapp.activities.WorkoutDetailActivity
import com.example.fitnessapp.base.BaseFragment
import com.example.fitnessapp.adapters.WorkoutAdapter
import com.example.fitnessapp.databinding.FragmentActivityBinding
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.viewmodels.ActivityViewModel

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
            // Navigate to WorkoutDetailActivity
            val intent = Intent(requireContext(), WorkoutDetailActivity::class.java)
            intent.putExtra("workout_data", workout)
            startActivity(intent)
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
        // Add new chips
        binding.chipKickboxing.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Kickboxing")
        }
        binding.chipMeditation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Meditation")
        }
        binding.chipStrength.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Strength")
        }
        binding.chipPilates.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Pilates")
        }
        binding.chipTreadmill.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setType("Treadmill")
        }
    }

    private fun setupFilterListeners() {
        binding.filterToday.setOnClickListener {
            viewModel.setFilter("Today")
            updateFilterButtonColors("Today")
        }
        binding.filterWeekly.setOnClickListener {
            viewModel.setFilter("Weekly")
            updateFilterButtonColors("Weekly")
        }
        binding.filterMonthly.setOnClickListener {
            viewModel.setFilter("Monthly")
            updateFilterButtonColors("Monthly")
        }
    }

    private fun updateFilterButtonColors(selected: String) {
        val activeColor = requireContext().getColor(R.color.primary)
        val inactiveColor = requireContext().getColor(R.color.surface)

        binding.filterToday.setBackgroundColor(if (selected == "Today") activeColor else inactiveColor)
        binding.filterWeekly.setBackgroundColor(if (selected == "Weekly") activeColor else inactiveColor)
        binding.filterMonthly.setBackgroundColor(if (selected == "Monthly") activeColor else inactiveColor)
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
            // Fix: Display calories correctly
            binding.totalWorkoutsValue.text = summary.totalWorkouts.toString()
            binding.totalCaloriesValue.text = summary.totalCalories.toString()
            val hours = summary.totalDuration / 60
            binding.totalHoursValue.text = hours.toString()
        }
    }

    override fun loadData() {
        viewModel.loadWorkouts()
    }
}