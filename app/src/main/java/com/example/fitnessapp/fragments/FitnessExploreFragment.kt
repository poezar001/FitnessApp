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
            openYouTubeVideo("https://www.youtube.com/watch?v=wTXVv1cYh0w") // Morning Yoga Flow
        }
    }

    private fun setupActivityTypes() {
        // Each activity card has its own click listener
        binding.activityMeditation.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=inpok4MKVLM")
        }
        binding.activityStrength.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=ykJp1N6hQow")
        }
        binding.activityYoga.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=wTXVv1cYh0w")
        }
        binding.activityRunning.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=HoUzR8q-Qyo")
        }
        binding.activityCycling.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=OBYV7sHR2xU")
        }
        binding.activityWeightlifting.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=1f4R08TzT-I")
        }
        binding.activityWalking.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=1TgJvMTbsMs")
        }
        binding.activityPilates.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=LD2Yeyq27H0")
        }
        binding.activityKickboxing.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=0oG8YeSi8as")
        }
        binding.activityTreadmill.setOnClickListener {
            openYouTubeVideo("https://www.youtube.com/watch?v=HpGf7z5VxLM")
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
                duration = "20 min"
            ),
            WorkoutProgram(
                title = "Treadmill HIIT Workout",
                description = "Fat burning cardio",
                episodes = "12 EPISODES",
                imageResId = R.drawable.act_treadmill,
                videoUrl = "https://www.youtube.com/watch?v=HpGf7z5VxLM",
                duration = "30 min"
            ),
            WorkoutProgram(
                title = "Kickboxing Fury",
                description = "Full body workout",
                episodes = "20 EPISODES",
                imageResId = R.drawable.act_kickboxing,
                videoUrl = "https://www.youtube.com/watch?v=0oG8YeSi8as",
                duration = "25 min"
            ),
            WorkoutProgram(
                title = "Strength Training 101",
                description = "Build muscle mass",
                episodes = "30 EPISODES",
                imageResId = R.drawable.act_strength,
                videoUrl = "https://www.youtube.com/watch?v=ykJp1N6hQow",
                duration = "35 min"
            ),
            WorkoutProgram(
                title = "Weightlifting Basics",
                description = "Proper form technique",
                episodes = "18 EPISODES",
                imageResId = R.drawable.act_weightlifting,
                videoUrl = "https://www.youtube.com/watch?v=1f4R08TzT-I",
                duration = "25 min"
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