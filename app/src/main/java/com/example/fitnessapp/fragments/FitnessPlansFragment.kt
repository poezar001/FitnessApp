package com.example.fitnessapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.SubscriptionActivity
import com.example.fitnessapp.adapters.*
import com.example.fitnessapp.databinding.FragmentFitnessPlansBinding
import com.example.fitnessapp.models.DaySchedule
import com.example.fitnessapp.models.ActivityTypeUI
import com.example.fitnessapp.models.Trainer
import com.example.fitnessapp.models.MusicGenre

class FitnessPlansFragment : Fragment() {

    private var _binding: FragmentFitnessPlansBinding? = null
    private val binding get() = _binding!!

    private lateinit var dayAdapter: DayScheduleAdapter
    private lateinit var activityAdapter: ActivityTypeAdapter
    private lateinit var categoryTrainerAdapter: CategoryTrainerAdapter
    private lateinit var categoryMusicAdapter: CategoryMusicAdapter

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

        binding.tryFreeButton.setOnClickListener {
            startActivity(Intent(requireContext(), SubscriptionActivity::class.java))
        }
        binding.tryFreeButtonBuild.setOnClickListener {
            startActivity(Intent(requireContext(), SubscriptionActivity::class.java))
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

        binding.scheduleSubtitle.text = "Starts Saturday 20 June for 4 Weeks"

        binding.tabSchedule.setOnClickListener {
            selectTab(binding.tabSchedule)
            showScheduleContent()
        }
        binding.tabTrainers.setOnClickListener {
            selectTab(binding.tabTrainers)
            showTrainersContent()
        }
        binding.tabMusic.setOnClickListener {
            selectTab(binding.tabMusic)
            showMusicContent()
        }

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

        showScheduleContent()
    }

    private fun showScheduleContent() {
        binding.scheduleContent.visibility = View.VISIBLE
        binding.trainersContent.visibility = View.GONE
        binding.musicContent.visibility = View.GONE
    }

    // In FitnessPlansFragment.kt, update the trainer and music setup:

    private fun showTrainersContent() {
        binding.scheduleContent.visibility = View.GONE
        binding.trainersContent.visibility = View.VISIBLE
        binding.musicContent.visibility = View.GONE

        val trainers = getAllTrainers().toMutableMap()
        categoryTrainerAdapter = CategoryTrainerAdapter(trainers)
        categoryTrainerAdapter.setOnTrainerClickListener { trainer, position, category ->
            // Handle selection
            Toast.makeText(
                requireContext(),
                if (trainer.isSelected) "Selected: ${trainer.name}" else "Deselected: ${trainer.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.trainersRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.trainersRecycler.adapter = categoryTrainerAdapter
    }

    private fun showMusicContent() {
        binding.scheduleContent.visibility = View.GONE
        binding.trainersContent.visibility = View.GONE
        binding.musicContent.visibility = View.VISIBLE

        val musicGenres = getAllMusicGenres().toMutableMap()
        categoryMusicAdapter = CategoryMusicAdapter(musicGenres)
        categoryMusicAdapter.setOnMusicClickListener { genre, position, category ->
            Toast.makeText(
                requireContext(),
                if (genre.isSelected) "Selected: ${genre.name}" else "Deselected: ${genre.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.musicRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.musicRecycler.adapter = categoryMusicAdapter
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

    // ==================== SCHEDULE METHODS ====================

    private fun getScheduleData(): List<DaySchedule> {
        return listOf(
            DaySchedule("Monday", "HIIT", "10min"),
            DaySchedule("Wednesday", "Yoga", "10min"),
            DaySchedule("Saturday", null, null)
        )
    }

    private fun showAddActivityDialog(day: DaySchedule, position: Int) {
        val activityTypes = arrayOf("HIIT", "Yoga", "Strength", "Pilates", "Running", "Cycling",
            "Weightlifting", "Walking", "Kickboxing", "Meditation", "Treadmill")

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

    // ==================== TRAINERS DATA (ALL 10 ACTIVITY TYPES) ====================

    private fun getAllTrainers(): Map<String, List<Trainer>> {
        return mapOf(
            "Running" to listOf(
                Trainer("Emily Johnson", "Marathon Runner", R.drawable.trainer_anja),
                Trainer("Michael Chen", "Sprint Coach", R.drawable.trainer_dice),
                Trainer("Sarah Williams", "Trail Running Expert", R.drawable.trainer_bakari),
                Trainer("David Kim", "Running Form Specialist", R.drawable.trainer_bakari)
            ),
            "Cycling" to listOf(
                Trainer("Laura Martinez", "Cycling Coach", R.drawable.trainer_bakari),
                Trainer("James Wilson", "Spin Instructor", R.drawable.trainer_anja),
                Trainer("Anna Kowalski", "Road Cycling Expert", R.drawable.trainer_kim),
                Trainer("Tom Harrison", "Mountain Bike Trainer", R.drawable.trainer_dice)
            ),
            "Walking" to listOf(
                Trainer("Patricia Lee", "Walking Coach", R.drawable.trainer_kim),
                Trainer("Robert Taylor", "Nordic Walking Expert", R.drawable.trainer_anja),
                Trainer("Maria Garcia", "Power Walking Trainer", R.drawable.trainer_kim),
                Trainer("John Anderson", "Walking for Health", R.drawable.trainer_jamie)
            ),
            "Weightlifting" to listOf(
                Trainer("Betina Gozo", "Strength Coach", R.drawable.trainer_bakari),
                Trainer("Gregg Cook", "Power Lifting", R.drawable.trainer_anja),
                Trainer("Jenn Lau", "Olympic Weightlifting", R.drawable.trainer_dice),
                Trainer("Kim Ngo", "Strength & Conditioning", R.drawable.trainer_kim)
            ),
            "Yoga" to listOf(
                Trainer("Dice Iida-Klein", "Yoga Expert", R.drawable.trainer_dice),
                Trainer("Jessica Skye", "Vinyasa Yoga", R.drawable.trainer_kim),
                Trainer("Jonelle Lewis", "Hatha Yoga", R.drawable.trainer_anja),
                Trainer("Molly Fox", "Yoga Instructor", R.drawable.trainer_bakari)
            ),
            "Meditation" to listOf(
                Trainer("David Chen", "Mindfulness Coach", R.drawable.trainer_dice),
                Trainer("Lisa Park", "Meditation Guide", R.drawable.trainer_anja),
                Trainer("Rachel Green", "Zen Meditation", R.drawable.trainer_kim),
                Trainer("Marcus Brown", "Guided Meditation", R.drawable.trainer_bakari)
            ),
            "Strength" to listOf(
                Trainer("Anja Garcia", "Strength Specialist", R.drawable.trainer_anja),
                Trainer("Bakari Williams", "Strength Coach", R.drawable.trainer_bakari),
                Trainer("Brian Cochrane", "Functional Strength", R.drawable.trainer_kim),
                Trainer("Jamie-Ray Hartshorne", "Strength Trainer", R.drawable.trainer_jamie)
            ),
            "Pilates" to listOf(
                Trainer("Kate Johnson", "Pilates Instructor", R.drawable.trainer_jamie),
                Trainer("Emma Wilson", "Reformer Pilates", R.drawable.trainer_anja),
                Trainer("Sophia Martinez", "Mat Pilates", R.drawable.trainer_bakari),
                Trainer("Oliver Brown", "Pilates Coach", R.drawable.trainer_dice)
            ),
            "Kickboxing" to listOf(
                Trainer("Mike Tyson Jr", "Kickboxing Coach", R.drawable.trainer_jamie),
                Trainer("Amanda Lee", "Muay Thai Expert", R.drawable.trainer_kim),
                Trainer("Carlos Santos", "Boxing Trainer", R.drawable.trainer_bakari),
                Trainer("Nina Williams", "Kickboxing Specialist", R.drawable.trainer_anja)
            ),
            "Treadmill" to listOf(
                Trainer("Chris Evans", "Treadmill Coach", R.drawable.trainer_jamie),
                Trainer("Megan Fox", "Incline Training Expert", R.drawable.trainer_kim),
                Trainer("Ryan Reynolds", "Interval Running", R.drawable.trainer_bakari),
                Trainer("Scarlett Johansson", "Treadmill Specialist", R.drawable.trainer_anja)
            )
        )
    }

// ==================== MUSIC DATA (ALL 10 ACTIVITY TYPES) ====================

    private fun getAllMusicGenres(): Map<String, List<MusicGenre>> {
        return mapOf(
            "Running" to listOf(
                MusicGenre("Upbeat Pop"),
                MusicGenre("Electronic"),
                MusicGenre("Rock"),
                MusicGenre("Dance")
            ),
            "Cycling" to listOf(
                MusicGenre("EDM"),
                MusicGenre("House"),
                MusicGenre("Techno"),
                MusicGenre("Drum & Bass")
            ),
            "Walking" to listOf(
                MusicGenre("Acoustic"),
                MusicGenre("Folk"),
                MusicGenre("Classical"),
                MusicGenre("Jazz")
            ),
            "Weightlifting" to listOf(
                MusicGenre("Heavy Metal"),
                MusicGenre("Hard Rock"),
                MusicGenre("Hip-Hop"),
                MusicGenre("Rap")
            ),
            "Yoga" to listOf(
                MusicGenre("Chill Vibes"),
                MusicGenre("Ambient"),
                MusicGenre("World Music"),
                MusicGenre("Instrumental")
            ),
            "Meditation" to listOf(
                MusicGenre("Nature Sounds"),
                MusicGenre("Binaural Beats"),
                MusicGenre("Zen"),
                MusicGenre("Tibetan Bowls")
            ),
            "Strength" to listOf(
                MusicGenre("Rock"),
                MusicGenre("Hip-Hop/R&B"),
                MusicGenre("Pop"),
                MusicGenre("Alternative")
            ),
            "Pilates" to listOf(
                MusicGenre("Soft Pop"),
                MusicGenre("Acoustic"),
                MusicGenre("Chill"),
                MusicGenre("Classical")
            ),
            "Kickboxing" to listOf(
                MusicGenre("Hip-Hop"),
                MusicGenre("Rap"),
                MusicGenre("Rock"),
                MusicGenre("Electronic")
            ),
            "Treadmill" to listOf(
                MusicGenre("Pop"),
                MusicGenre("Dance"),
                MusicGenre("Rock"),
                MusicGenre("EDM")
            )
        )
    }
    // ==================== BUILD YOUR OWN METHODS ====================

    private fun setupBuildYourOwn() {
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

        setupActivityTypes()
    }

    private fun setupActivityTypes() {
        val activityTypes = getActivityTypes()
        activityAdapter = ActivityTypeAdapter(activityTypes) { activity, position ->
            val selectedCount = activityAdapter.getSelectedActivities().size
            binding.selectedCountText.text = "$selectedCount activities selected"

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
            ActivityTypeUI("Running", R.drawable.act_running),
            ActivityTypeUI("Cycling", R.drawable.act_cycling),
            ActivityTypeUI("Walking", R.drawable.act_walking),
            ActivityTypeUI("Weightlifting", R.drawable.act_weightlifting),
            ActivityTypeUI("Yoga", R.drawable.act_yoga),
            ActivityTypeUI("Meditation", R.drawable.act_meditation),
            ActivityTypeUI("Strength", R.drawable.act_strength),
            ActivityTypeUI("Pilates", R.drawable.act_pilates),
            ActivityTypeUI("Kickboxing", R.drawable.act_kickboxing),
            ActivityTypeUI("Treadmill", R.drawable.act_treadmill)
        )
    }

    // ==================== DIALOGS ====================

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
                val dayOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                val insertIndex = dayOrder.indexOf(selectedDay)
                currentList.add(insertIndex, DaySchedule(selectedDay, null, null))
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