package com.example.fitnessapp.fragments

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnessapp.R
import com.example.fitnessapp.activities.LoginActivity
import com.example.fitnessapp.activities.SettingsActivity
import com.example.fitnessapp.base.BaseFragment
import com.example.fitnessapp.adapters.AchievementAdapter
import com.example.fitnessapp.databinding.FragmentProfileBinding
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.BMICalculator
import com.example.fitnessapp.utils.DateUtils
import com.example.fitnessapp.viewmodels.ProfileViewModel

class ProfileFragment : BaseFragment<FragmentProfileBinding>(R.layout.fragment_profile) {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var achievementAdapter: AchievementAdapter

    override fun initViewModel() {
        val repository = MainRepository(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(repository) as T
            }
        }).get(ProfileViewModel::class.java)
    }

    override fun setupUI() {
        setupRecyclerView()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Ensure data refreshes when returning to this tab screen
        loadData()
    }

    private fun setupRecyclerView() {
        // Essential layout manager assignment so the items render structurally
        binding.achievementsRecycler.layoutManager = LinearLayoutManager(requireContext())

        achievementAdapter = AchievementAdapter(emptyList())
        binding.achievementsRecycler.adapter = achievementAdapter
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    override fun setupObservers() {
        viewModel.username.observe(viewLifecycleOwner) { username ->
            binding.profileUsername.text = username
        }

        viewModel.email.observe(viewLifecycleOwner) { email ->
            binding.profileEmail.text = email
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                binding.profileAge.text = calculateAge(profile.birthday).toString()
                binding.profileWeight.text = "${profile.weight} kg"
                binding.profileHeight.text = "${profile.height} cm"

                // Maps activity level cleanly using name fallback if displayName isn't configured
                binding.profileFitnessLevel.text = try {
                    profile.activityLevel.displayName
                } catch (e: Exception) {
                    profile.activityLevel.name.replace("_", " ")
                }

                val bmi = BMICalculator.calculateBMI(profile.weight.toDouble(), profile.height.toDouble())
                binding.profileFitnessLevel.append(" (BMI: ${String.format("%.1f", bmi)})")
            }
        }

        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            achievements?.let {
                achievementAdapter.updateData(it)
            }
        }
    }

    override fun loadData() {
        viewModel.loadUserData()
        binding.memberSince.text = "Member since: ${DateUtils.getCurrentDate()}"
    }

    private fun calculateAge(birthday: String): Int {
        return try {
            val birthDate = DateUtils.parseDate(birthday)
            val calendar = java.util.Calendar.getInstance()
            val today = java.util.Calendar.getInstance()
            birthDate?.let {
                calendar.time = it
                var age = today.get(java.util.Calendar.YEAR) - calendar.get(java.util.Calendar.YEAR)
                if (today.get(java.util.Calendar.DAY_OF_YEAR) < calendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                    age--
                }
                age
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}