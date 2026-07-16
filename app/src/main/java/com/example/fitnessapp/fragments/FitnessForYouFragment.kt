package com.example.fitnessapp.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.adapters.TrainerAdapter
import com.example.fitnessapp.adapters.TrainerTipAdapter
import com.example.fitnessapp.adapters.WorkoutProgramAdapter
import com.example.fitnessapp.databinding.FragmentFitnessForYouBinding
import com.example.fitnessapp.models.Trainer
import com.example.fitnessapp.models.WorkoutProgram

class FitnessForYouFragment : Fragment() {

    private var _binding: FragmentFitnessForYouBinding? = null
    private val binding get() = _binding!!

    private lateinit var trainerAdapter: TrainerAdapter
    private lateinit var tipAdapter: TrainerTipAdapter
    private lateinit var programAdapter: WorkoutProgramAdapter

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            _binding?.let { b ->
                val currentItem = b.tipViewPager.currentItem
                val nextItem = if (currentItem < tipAdapter.itemCount - 1) currentItem + 1 else 0
                b.tipViewPager.setCurrentItem(nextItem, true)
                b.tipViewPager.postDelayed(this, 5000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFitnessForYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTrainerTips()
        setupTrainers()
    }

    override fun onResume() {
        super.onResume()
        // Load and recalculate layout selections whenever fragment returns to active focus state
        setupRecommendedPrograms()
    }

    private fun setupTrainerTips() {
        tipAdapter = TrainerTipAdapter(getTrainerTips())
        binding.tipViewPager.adapter = tipAdapter
        startAutoScroll()
    }

    private fun startAutoScroll() {
        binding.tipViewPager.postDelayed(autoScrollRunnable, 5000)
    }

    private fun setupTrainers() {
        val adapter = TrainerAdapter(
            trainers = getTrainers(),
            onItemClick = { trainer ->
                Toast.makeText(requireContext(), "Selected: ${trainer.name}", Toast.LENGTH_SHORT).show()
            },
            showQuote = true,
            showExperience = true,
            showSelection = false
        )
        binding.trainersRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.trainersRecycler.adapter = adapter
    }

    private fun setupRecommendedPrograms() {
        val sharedPrefs = requireContext().getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)

        val userLevel = sharedPrefs.getString("user_level", "Beginner") ?: "Beginner"
        val activeGoalType = sharedPrefs.getString("active_goal_type", "Calories") ?: "Calories"

        val targetValue = sharedPrefs.getFloat("active_goal_target", when(activeGoalType) {
            "Calories" -> 2000f
            "Distance" -> 20f
            "Workouts" -> 4f
            else -> 4f
        }).toDouble()

        val filteredPrograms = filterPrograms(userLevel, activeGoalType)

        // Calculate dynamic frequency targets per activity card
        filteredPrograms.forEach { program ->
            when (activeGoalType) {
                "Calories" -> {
                    val times = Math.ceil(targetValue / program.approxBurn).toInt()
                    program.calculatedTargetText = "🎯 Complete this $times times to hit your goal"
                }
                "Distance" -> {
                    if (program.approxDistance > 0) {
                        val times = Math.ceil(targetValue / program.approxDistance).toInt()
                        program.calculatedTargetText = "🎯 Run this layout $times times this week"
                    } else {
                        program.calculatedTargetText = "🎯 Perfect stamina base builder"
                    }
                }
                "Workouts" -> {
                    program.calculatedTargetText = "🎯 Complete $targetValue total sessions this week"
                }
                else -> {
                    program.calculatedTargetText = "🎯 Follow this track to maintain targets"
                }
            }
        }

        binding.tvRecommendationReason.text = "Goal Roadmap: Custom paths to reach your $activeGoalType target!"

        programAdapter = WorkoutProgramAdapter(filteredPrograms) { videoUrl ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to play video stream", Toast.LENGTH_SHORT).show()
            }
        }

        binding.programsRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.programsRecycler.adapter = programAdapter
    }
    private fun filterPrograms(level: String, goalType: String): List<WorkoutProgram> {
        val masterList = getMasterWorkoutPrograms()

        return masterList.filter { program ->
            val matchesLevel = program.level.equals(level, ignoreCase = true)

            val matchesGoal = when (goalType) {
                "Calories" -> {
                    program.activityType == "Kickboxing" ||
                            program.activityType == "Treadmill" ||
                            program.activityType == "Running" ||
                            program.activityType == "Cycling"
                }
                "Distance" -> {
                    program.activityType == "Running" ||
                            program.activityType == "Treadmill" ||
                            program.activityType == "Walking" ||
                            program.activityType == "Cycling"
                }
                "Workouts" -> {
                    program.activityType == "Weightlifting" ||
                            program.activityType == "Strength" ||
                            program.activityType == "Pilates" ||
                            program.activityType == "Yoga" ||
                            program.activityType == "Meditation"
                }
                else -> program.activityType == "Yoga" || program.activityType == "Meditation"
            }

            matchesLevel && matchesGoal
        }
    }

    private fun getTrainerTips(): List<TrainerTip> {
        return listOf(
            TrainerTip("Dice Iida-Klein", "Yoga Expert", "Feel more confident doing backbends in yoga", R.drawable.trainer_dice),
            TrainerTip("Anja Garcia", "Strength Coach", "Build strength with proper form and consistency", R.drawable.trainer_anja),
            TrainerTip("Bakari Williams", "HIIT Specialist", "Push your limits with high-intensity interval training", R.drawable.trainer_bakari),
            TrainerTip("Kim Ngo", "Pilates Instructor", "Core strength is the foundation of all movement", R.drawable.trainer_kim),
            TrainerTip("Jamie Ray", "Dance Fitness", "Find your rhythm and let the music move you", R.drawable.trainer_jamie)
        )
    }

    private fun getTrainers(): List<Trainer> {
        return listOf(
            Trainer("Dice", "Yoga Expert", R.drawable.trainer_dice, "\"Find your inner peace\"", "10+ years"),
            Trainer("Anja", "Strength Coach", R.drawable.trainer_anja, "\"Strong is beautiful\"", "8+ years"),
            Trainer("Bakari", "HIIT Specialist", R.drawable.trainer_bakari, "\"Embrace the burn\"", "6+ years"),
            Trainer("Kim", "Pilates Instructor", R.drawable.trainer_kim, "\"Core confidence\"", "7+ years"),
            Trainer("Jamie", "Dance Fitness", R.drawable.trainer_jamie, "\"Move to the beat\"", "5+ years")
        )
    }

    private fun getMasterWorkoutPrograms(): List<WorkoutProgram> {
        return listOf(
            // === CALORIES TARGETS ===
            WorkoutProgram("Cardio Kickboxing Shred", "High energy combat cardio.", "10 Episodes", R.drawable.act_kickboxing, "url_kick_1", "20 min", "Beginner", "Kickboxing", approxBurn = 250),
            WorkoutProgram("Treadmill Interval Burn", "Fat burning indoor intervals.", "12 Episodes", R.drawable.act_treadmill, "url_tread_1", "30 min", "Beginner", "Treadmill", approxBurn = 280),
            WorkoutProgram("Speed Endurance Run", "Outdoor calorie crushing loops.", "15 Episodes", R.drawable.act_running, "url_run_1", "45 min", "Intermediate", "Running", approxBurn = 400),
            WorkoutProgram("Metabolic Cycling Focus", "High-intensity indoor road simulation.", "8 Episodes", R.drawable.act_cycling, "url_cyc_1", "30 min", "Intermediate", "Cycling", approxBurn = 350),
            WorkoutProgram("Elite Kickboxing Fury", "Challenging combinations and core drills.", "20 Episodes", R.drawable.act_kickboxing, "url_kick_2", "45 min", "Advanced", "Kickboxing", approxBurn = 500),

            // === DISTANCE TARGETS ===
            // Add an explicit approxBurn value to your distance base builders:
            WorkoutProgram("Intro Base Pacing", "Build breathing consistency on the road.", "6 Episodes", R.drawable.act_running, "url_run_2", "20 min", "Beginner", "Running", approxBurn = 200, approxDistance = 3.0),
            WorkoutProgram("Treadmill Walk & Jog", "Low impact distance builder.", "8 Episodes", R.drawable.act_treadmill, "url_tread_2", "25 min", "Beginner", "Treadmill", approxBurn = 200, approxDistance = 2.5),
            WorkoutProgram("Tempo Cardio Walk", "Fast-paced outdoor walking progression.", "10 Episodes", R.drawable.act_walking, "url_walk_1", "30 min", "Intermediate", "Walking", approxDistance = 4.0),
            WorkoutProgram("Threshold Road Cycling", "Long distance aerobic endurance build.", "12 Episodes", R.drawable.act_cycling, "url_cyc_2", "60 min", "Intermediate", "Cycling", approxDistance = 15.0),
            WorkoutProgram("Advanced Marathon Prep", "Elite progressive distance training.", "15 Episodes", R.drawable.act_running, "url_run_3", "50 min", "Advanced", "Running", approxDistance = 8.0),

            // === WORKOUTS / REPETITIONS TARGETS ===
            WorkoutProgram("Foundational Strength 101", "Learn basic lifting mechanics safely.", "8 Episodes", R.drawable.act_strength, "url_str_1", "20 min", "Beginner", "Strength"),
            WorkoutProgram("Weightlifting Form Basics", "Master compound lifting techniques.", "6 Episodes", R.drawable.act_weightlifting, "url_weight_1", "25 min", "Beginner", "Weightlifting"),
            WorkoutProgram("Core Balance Pilates", "Refine deep abdominal stabilization.", "10 Episodes", R.drawable.act_pilates, "url_pil_1", "30 min", "Intermediate", "Pilates"),
            WorkoutProgram("Hypertrophy Split Routine", "Targeted muscle mass builder groups.", "12 Episodes", R.drawable.act_strength, "url_str_2", "40 min", "Intermediate", "Strength"),
            WorkoutProgram("Elite Powerlifting Track", "Max output heavy resistance sets.", "10 Episodes", R.drawable.act_weightlifting, "url_weight_2", "50 min", "Advanced", "Weightlifting"),

            // === GENERAL WELLNESS / FALLBACKS ===
            WorkoutProgram("Morning Flow Yoga", "Gentle flexibility foundation setups.", "6 Episodes", R.drawable.act_yoga, "url_yoga_1", "15 min", "Beginner", "Yoga"),
            WorkoutProgram("Mindful Breathing Intro", "Stress relief and focus control.", "5 Episodes", R.drawable.act_meditation, "url_med_1", "10 min", "Beginner", "Meditation"),
            WorkoutProgram("Power Vinyasa Flow", "Challenging dynamic balance sequences.", "10 Episodes", R.drawable.act_yoga, "url_yoga_2", "45 min", "Intermediate", "Yoga"),
            WorkoutProgram("Deep Zen Meditation", "Advanced emotional grounding and recovery.", "8 Episodes", R.drawable.act_meditation, "url_med_2", "20 min", "Advanced", "Meditation")
        )
    }

    override fun onDestroyView() {
        binding.tipViewPager.removeCallbacks(autoScrollRunnable)
        super.onDestroyView()
        _binding = null
    }
}

data class TrainerTip(
    val trainerName: String,
    val trainerRole: String,
    val tip: String,
    val imageResId: Int
)