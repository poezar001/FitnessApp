package com.example.fitnessapp.fragments

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
import com.example.fitnessapp.adapters.WorkoutProgramAdapter
import com.example.fitnessapp.databinding.FragmentFitnessExploreBinding
import com.example.fitnessapp.models.WorkoutProgram

class FitnessExploreFragment : Fragment() {

    private var _binding: FragmentFitnessExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var programAdapter: WorkoutProgramAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFitnessExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSamplerClick()
        setupActivityTypes()
        setupPrograms()
    }

    private fun setupSamplerClick() {
        binding.cardSampler.setOnClickListener {
            openYouTubeVideo("https://youtu.be/EvMTrP8eRvM?si=7855PSGqdjvZRWDO") // Morning Yoga Flow
        }
    }

    private fun setupActivityTypes() {
        binding.activityMeditation.setOnClickListener {
            openYouTubeVideo("https://youtu.be/3rTdYCWrm8c?si=_IJwxCmGZ5fVMJri")
        }
        binding.activityStrength.setOnClickListener {
            openYouTubeVideo("https://youtu.be/V09O2rwArjI?si=lC9ylSwSI5yHQYBR")
        }
        binding.activityYoga.setOnClickListener {
            openYouTubeVideo("https://youtu.be/EvMTrP8eRvM?si=7855PSGqdjvZRWDO")
        }
        binding.activityRunning.setOnClickListener {
            openYouTubeVideo("https://youtu.be/twuSS9Uu2p0?si=Af3zAO_6dmglbEPf")
        }
        binding.activityCycling.setOnClickListener {
            openYouTubeVideo("https://youtu.be/D3bQxOx8DAk?si=Rvtew_Jvp6Aho8IR")
        }
        binding.activityWeightlifting.setOnClickListener {
            openYouTubeVideo("https://youtu.be/1VvgNZsg9nc?si=kzv9PzYrSK7UensO")
        }
        binding.activityWalking.setOnClickListener {
            openYouTubeVideo("https://youtu.be/KaIeBaxzIqs?si=BqlOkSM1rYS4EqUj")
        }
        binding.activityPilates.setOnClickListener {
            openYouTubeVideo("https://youtu.be/-0S6qnm2EqU?si=-9sxzysUuOLMUBq3")
        }
        binding.activityKickboxing.setOnClickListener {
            openYouTubeVideo("https://youtu.be/Hri2rYgOLKI?si=IgHwsh7rGNxMn03g")
        }
        binding.activityTreadmill.setOnClickListener {
            openYouTubeVideo("https://youtu.be/8gWkLOFGzSo?si=OPCvSSrgwyR4BylF")
        }
    }

    private fun setupPrograms() {
        programAdapter = WorkoutProgramAdapter(getPrograms()) { videoUrl ->
            openYouTubeVideo(videoUrl)
        }
        binding.programsRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.programsRecycler.adapter = programAdapter
    }

    private fun getPrograms(): List<WorkoutProgram> {
        return listOf(
            WorkoutProgram(
                title = "Pilates Core Strength",
                description = "Build core stability",
                episodes = "15 EPISODES",
                imageResId = R.drawable.act_pilates,
                videoUrl = "https://www.youtube.com/watch?v=LD2Yeyq27H0",
                duration = "20 min",
                level = "Intermediate",
                activityType = "Pilates",
                approxBurn = 220
            ),
            WorkoutProgram(
                title = "Treadmill HIIT Workout",
                description = "Fat burning cardio",
                episodes = "12 EPISODES",
                imageResId = R.drawable.act_treadmill,
                videoUrl = "https://www.youtube.com/watch?v=HpGf7z5VxLM",
                duration = "30 min",
                level = "Beginner",
                activityType = "Treadmill",
                approxBurn = 350,
                approxDistance = 4.0
            ),
            WorkoutProgram(
                title = "Kickboxing Fury",
                description = "Full body workout",
                episodes = "20 EPISODES",
                imageResId = R.drawable.act_kickboxing,
                videoUrl = "https://www.youtube.com/watch?v=0oG8YeSi8as",
                duration = "25 min",
                level = "Advanced",
                activityType = "Kickboxing",
                approxBurn = 450
            ),
            WorkoutProgram(
                title = "Strength Training 101",
                description = "Build muscle mass",
                episodes = "30 EPISODES",
                imageResId = R.drawable.act_strength,
                videoUrl = "https://www.youtube.com/watch?v=ykJp1N6hQow",
                duration = "35 min",
                level = "Beginner",
                activityType = "Strength"
            ),
            WorkoutProgram(
                title = "Weightlifting Basics",
                description = "Proper form technique",
                episodes = "18 EPISODES",
                imageResId = R.drawable.act_weightlifting,
                videoUrl = "https://www.youtube.com/watch?v=1f4R08TzT-I",
                duration = "25 min",
                level = "Beginner",
                activityType = "Weightlifting"
            )
        )
    }

    private fun openYouTubeVideo(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open video", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}