package com.example.fitnessapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.adapters.TrainerAdapter
import com.example.fitnessapp.adapters.TrainerTipAdapter
import com.example.fitnessapp.databinding.FragmentFitnessForYouBinding
import com.example.fitnessapp.models.Trainer

class FitnessForYouFragment : Fragment() {

    private var _binding: FragmentFitnessForYouBinding? = null
    private val binding get() = _binding!!

    private lateinit var trainerAdapter: TrainerAdapter
    private lateinit var tipAdapter: TrainerTipAdapter

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

    private fun setupTrainerTips() {
        tipAdapter = TrainerTipAdapter(getTrainerTips())
        binding.tipViewPager.adapter = tipAdapter

        // Auto-scroll
        startAutoScroll()
    }

    private fun startAutoScroll() {
        binding.tipViewPager.postDelayed(autoScrollRunnable, 5000)
    }

    private fun setupTrainers() {
        trainerAdapter = TrainerAdapter(getTrainers())
        binding.trainersRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.trainersRecycler.adapter = trainerAdapter
    }

    private fun getTrainerTips(): List<TrainerTip> {
        return listOf(
            TrainerTip(
                trainerName = "Dice Iida-Klein",
                trainerRole = "Yoga Expert",
                tip = "Feel more confident doing backbends in yoga",
                imageResId = R.drawable.trainer_dice
            ),
            TrainerTip(
                trainerName = "Anja Garcia",
                trainerRole = "Strength Coach",
                tip = "Build strength with proper form and consistency",
                imageResId = R.drawable.trainer_anja
            ),
            TrainerTip(
                trainerName = "Bakari Williams",
                trainerRole = "HIIT Specialist",
                tip = "Push your limits with high-intensity interval training",
                imageResId = R.drawable.trainer_bakari
            ),
            TrainerTip(
                trainerName = "Kim Ngo",
                trainerRole = "Pilates Instructor",
                tip = "Core strength is the foundation of all movement",
                imageResId = R.drawable.trainer_kim
            ),
            TrainerTip(
                trainerName = "Jamie Ray",
                trainerRole = "Dance Fitness",
                tip = "Find your rhythm and let the music move you",
                imageResId = R.drawable.trainer_jamie
            )
        )
    }

    private fun getTrainers(): List<Trainer> {
        return listOf(
            Trainer(
                name = "Dice",
                role = "Yoga Expert",
                imageResId = R.drawable.trainer_dice,
                quote = "\"Find your inner peace\"",
                experience = "10+ years"
            ),
            Trainer(
                name = "Anja",
                role = "Strength Coach",
                imageResId = R.drawable.trainer_anja,
                quote = "\"Strong is beautiful\"",
                experience = "8+ years"
            ),
            Trainer(
                name = "Bakari",
                role = "HIIT Specialist",
                imageResId = R.drawable.trainer_bakari,
                quote = "\"Embrace the burn\"",
                experience = "6+ years"
            ),
            Trainer(
                name = "Kim",
                role = "Pilates Instructor",
                imageResId = R.drawable.trainer_kim,
                quote = "\"Core confidence\"",
                experience = "7+ years"
            ),
            Trainer(
                name = "Jamie",
                role = "Dance Fitness",
                imageResId = R.drawable.trainer_jamie,
                quote = "\"Move to the beat\"",
                experience = "5+ years"
            )
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