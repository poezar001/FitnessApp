package com.example.fitnessapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.adapters.DayScheduleAdapter
import com.example.fitnessapp.adapters.ActivityTypeAdapter
import com.example.fitnessapp.databinding.FragmentFitnessPlansBinding
import com.example.fitnessapp.models.DaySchedule
import com.example.fitnessapp.models.ActivityTypeUI

class FitnessPlansFragment : Fragment() {

    private var _binding: FragmentFitnessPlansBinding? = null
    private val binding get() = _binding!!

    private lateinit var dayAdapter: DayScheduleAdapter
    private lateinit var activityAdapter: ActivityTypeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFitnessPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showMainPlans()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.getStartedCard.setOnClickListener {
            showMadeForYou()
        }

        binding.planPreview.setOnClickListener {
            showMadeForYou()
        }

        binding.buildOwnPlanCard.setOnClickListener {
            showBuildYourOwn()
        }

        binding.backToPlans.setOnClickListener {
            showMainPlans()
        }

        binding.backToPlansBuild.setOnClickListener {
            showMainPlans()
        }

        binding.addDayButton.setOnClickListener {
            showAddDayDialog()
        }

        binding.timeOptions.setOnClickListener {
            showTimePickerDialog()
        }

        binding.lengthOptions.setOnClickListener {
            showLengthPickerDialog()
        }
    }

    private fun showMainPlans() {
        binding.mainPlansLayout.visibility = View.VISIBLE
        binding.scheduleLayout.visibility = View.GONE
        binding.buildOwnLayout.visibility = View.GONE
    }

    private fun showMadeForYou() {
        binding.mainPlansLayout.visibility = View.GONE
        binding.scheduleLayout.visibility = View.VISIBLE
        binding.buildOwnLayout.visibility = View.GONE
        setupSchedule()
    }

    private fun showBuildYourOwn() {
        binding.mainPlansLayout.visibility = View.GONE
        binding.scheduleLayout.visibility = View.GONE
        binding.buildOwnLayout.visibility = View.VISIBLE
        setupBuildYourOwn()
    }

    private fun setupSchedule() {
        binding.scheduleTitle.text = "Get Started"
        binding.scheduleSubtitle.text = "Starts Saturday 20 June for 4 Weeks"

        // Setup tabs
        binding.tabSchedule.setOnClickListener {
            selectTab(binding.tabSchedule)
            Toast.makeText(requireContext(), "Schedule", Toast.LENGTH_SHORT).show()
        }
        binding.tabTrainers.setOnClickListener {
            selectTab(binding.tabTrainers)
            Toast.makeText(requireContext(), "Trainers feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.tabMusic.setOnClickListener {
            selectTab(binding.tabMusic)
            Toast.makeText(requireContext(), "Music feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Setup schedule list
        val scheduleList = getScheduleData()
        dayAdapter = DayScheduleAdapter(scheduleList) { day, position ->
            if (day.activity != null) {
                showEditActivityDialog(day, position)
            } else {
                showAddActivityDialog(day, position)
            }
        }
        binding.scheduleRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.scheduleRecycler.adapter = dayAdapter
    }

    private fun selectTab(selectedTab: TextView) {
        val tabs = listOf(binding.tabSchedule, binding.tabTrainers, binding.tabMusic)
        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(resources.getColor(R.color.text_primary, null))
            } else {
                tab.setBackgroundResource(R.drawable.bg_tab_default)
                tab.setTextColor(resources.getColor(R.color.text_secondary, null))
            }
        }
    }

    private fun getScheduleData(): List<DaySchedule> {
        return listOf(
            DaySchedule("Monday", "HIIT", "10min"),
            DaySchedule("Wednesday", "Yoga", "10min"),
            DaySchedule("Saturday", null, null)
        )
    }

    private fun showAddActivityDialog(day: DaySchedule, position: Int) {
        val activityTypes = arrayOf("HIIT", "Yoga", "Strength", "Pilates", "Running", "Cycling",
            "Weightlifting", "Walking", "Kickboxing", "Meditation")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Activity for ${day.day}")
            .setItems(activityTypes) { _, which ->
                val selectedActivity = activityTypes[which]
                val updatedList = dayAdapter.getData().toMutableList()
                updatedList[position] = day.copy(activity = selectedActivity, duration = "10min")
                dayAdapter.updateData(updatedList)
                Toast.makeText(requireContext(), "$selectedActivity added for ${day.day}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditActivityDialog(day: DaySchedule, position: Int) {
        val options = arrayOf("Edit Duration", "Change Activity", "Remove")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit ${day.day} - ${day.activity}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDurationDialog(day, position)
                    1 -> showAddActivityDialog(day, position)
                    2 -> {
                        val updatedList = dayAdapter.getData().toMutableList()
                        updatedList[position] = day.copy(activity = null, duration = null)
                        dayAdapter.updateData(updatedList)
                        Toast.makeText(requireContext(), "Activity removed from ${day.day}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDurationDialog(day: DaySchedule, position: Int) {
        val durations = arrayOf("5min", "10min", "20min", "30min", "45min")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Duration for ${day.day}")
            .setItems(durations) { _, which ->
                val selectedDuration = durations[which]
                val updatedList = dayAdapter.getData().toMutableList()
                updatedList[position] = day.copy(duration = selectedDuration)
                dayAdapter.updateData(updatedList)
                Toast.makeText(requireContext(), "Duration set to $selectedDuration", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBuildYourOwn() {
        // Setup week days selection
        val dayViews = listOf(
            binding.dayMon, binding.dayTue, binding.dayWed,
            binding.dayThu, binding.dayFri, binding.daySat, binding.daySun
        )

        dayViews.forEach { textView ->
            textView.setOnClickListener {
                val isSelected = !textView.isSelected
                textView.isSelected = isSelected
                textView.setBackgroundResource(
                    if (isSelected) R.drawable.bg_day_selected
                    else R.drawable.bg_day_default
                )
            }
        }

        // Setup activity types with multi-selection
        setupActivityTypes()
    }

    private fun setupActivityTypes() {
        val activityTypes = getActivityTypes()
        activityAdapter = ActivityTypeAdapter(activityTypes) { activity, position ->
            // Update selected count
            val selectedCount = activityAdapter.getSelectedActivities().size
            binding.selectedCountText.text = "$selectedCount activities selected"

            // Show selected activities
            if (selectedCount > 0) {
                val selectedNames = activityAdapter.getSelectedActivities()
                    .joinToString(", ") { it.name }
                Toast.makeText(
                    requireContext(),
                    "Selected: $selectedNames",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.activityTypesRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.activityTypesRecycler.adapter = activityAdapter
    }

    private fun getActivityTypes(): List<ActivityTypeUI> {
        return listOf(
            ActivityTypeUI("Core", R.drawable.act_strength),
            ActivityTypeUI("Cycling", R.drawable.act_cycling),
            ActivityTypeUI("Dance", R.drawable.act_running),
            ActivityTypeUI("HIIT", R.drawable.act_kickboxing),
            ActivityTypeUI("Kickboxing", R.drawable.act_kickboxing),
            ActivityTypeUI("Meditation", R.drawable.act_meditation),
            ActivityTypeUI("Running", R.drawable.act_running),
            ActivityTypeUI("Strength", R.drawable.act_strength),
            ActivityTypeUI("Walking", R.drawable.act_walking),
            ActivityTypeUI("Yoga", R.drawable.act_yoga)
        )
    }

    private fun showTimePickerDialog() {
        val times = arrayOf("10min", "20min", "30min", "45min", "60min")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Total Time Per Day")
            .setItems(times) { _, which ->
                binding.timeOptions.text = times[which]
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLengthPickerDialog() {
        val lengths = arrayOf("2 Weeks", "3 Weeks", "4 Weeks", "6 Weeks", "8 Weeks")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Length of Plan")
            .setItems(lengths) { _, which ->
                binding.lengthOptions.text = lengths[which]
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDayDialog() {
        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add a Day")
            .setItems(days) { _, which ->
                val selectedDay = days[which]
                Toast.makeText(requireContext(), "Added $selectedDay to your schedule", Toast.LENGTH_SHORT).show()
                val currentList = dayAdapter.getData().toMutableList()
                currentList.add(DaySchedule(selectedDay, null, null))
                dayAdapter.updateData(currentList)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}