package com.example.fitnessapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivityPersonalizeBinding
import com.example.fitnessapp.models.ActivityLevel
import com.example.fitnessapp.repository.AuthRepository
import com.example.fitnessapp.viewmodels.ProfileViewModel
import java.util.Calendar

class PersonalizeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalizeBinding
    private lateinit var authRepository: AuthRepository

    private var selectedGender: String = "Male"
    private var currentAge: Int = 25
    private var currentHeight: Int = 170
    private var currentWeight: Int = 70
    private var selectedActivityLevel: ActivityLevel = ActivityLevel.BEGINNER

    private val profileViewModel: ProfileViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                authRepository = AuthRepository(this@PersonalizeActivity)
                return ProfileViewModel(authRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGenderSelection()
        setupValueAdjusters()
        setupActivityLevelSelection()
        setupObservers()
        setupClickListeners()

        // Initialize UI with default values
        updateGenderUI()
        updateActivityLevelUI()
        updateValueDisplay()
    }

    private fun setupGenderSelection() {
        binding.cardMale.setOnClickListener {
            selectedGender = "Male"
            updateGenderUI()
        }
        binding.cardFemale.setOnClickListener {
            selectedGender = "Female"
            updateGenderUI()
        }
    }

    private fun updateGenderUI() {
        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.surface)

        binding.cardMale.setCardBackgroundColor(if (selectedGender == "Male") activeColor else inactiveColor)
        binding.cardFemale.setCardBackgroundColor(if (selectedGender == "Female") activeColor else inactiveColor)
    }

    private fun setupValueAdjusters() {
        // Age adjustment
        binding.btnPlusAge.setOnClickListener {
            if (currentAge < 100) {
                currentAge++
                updateValueDisplay()
            }
        }
        binding.btnMinusAge.setOnClickListener {
            if (currentAge > 10) {
                currentAge--
                updateValueDisplay()
            }
        }

        // Height adjustment
        binding.btnPlusHeight.setOnClickListener {
            if (currentHeight < 250) {
                currentHeight++
                updateValueDisplay()
            }
        }
        binding.btnMinusHeight.setOnClickListener {
            if (currentHeight > 100) {
                currentHeight--
                updateValueDisplay()
            }
        }

        // Weight adjustment
        binding.btnPlusWeight.setOnClickListener {
            if (currentWeight < 250) {
                currentWeight++
                updateValueDisplay()
            }
        }
        binding.btnMinusWeight.setOnClickListener {
            if (currentWeight > 30) {
                currentWeight--
                updateValueDisplay()
            }
        }
    }

    private fun updateValueDisplay() {
        binding.ageValue.text = currentAge.toString()
        binding.heightValue.text = currentHeight.toString()
        binding.weightValue.text = currentWeight.toString()
    }

    private fun setupActivityLevelSelection() {
        binding.cardBeginner.setOnClickListener {
            selectedActivityLevel = ActivityLevel.BEGINNER
            updateActivityLevelUI()
        }
        binding.cardIntermediate.setOnClickListener {
            selectedActivityLevel = ActivityLevel.INTERMEDIATE
            updateActivityLevelUI()
        }
        binding.cardAdvanced.setOnClickListener {
            selectedActivityLevel = ActivityLevel.ADVANCED
            updateActivityLevelUI()
        }
    }

    private fun updateActivityLevelUI() {
        binding.radioBeginner.isChecked = selectedActivityLevel == ActivityLevel.BEGINNER
        binding.radioIntermediate.isChecked = selectedActivityLevel == ActivityLevel.INTERMEDIATE
        binding.radioAdvanced.isChecked = selectedActivityLevel == ActivityLevel.ADVANCED
    }

    private fun setupObservers() {
        profileViewModel.isSaving.observe(this) { isSaving ->
            binding.progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !isSaving
        }

        profileViewModel.saveResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                // Navigate to MainDashboardActivity
                startActivity(Intent(this, MainDashboardActivity::class.java))
                finish()
            } else {
                binding.errorText.text = "Failed to save profile."
                binding.errorText.visibility = View.VISIBLE
            }
        }

        profileViewModel.error.observe(this) { error ->
            binding.errorText.text = error
            binding.errorText.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val userId = authRepository.getUserId()

            if (userId == -1) {
                binding.errorText.text = "Session expired. Please login again."
                binding.errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Calculate approximate birthday based on age for the data model
            val calendar = Calendar.getInstance()
            val birthYear = calendar.get(Calendar.YEAR) - currentAge
            val birthday = "$birthYear-01-01"

            profileViewModel.saveProfile(
                userId = userId,
                birthday = birthday,
                gender = selectedGender,
                height = currentHeight.toFloat(),
                weight = currentWeight.toFloat(),
                activityLevel = selectedActivityLevel
            )
        }
    }
}