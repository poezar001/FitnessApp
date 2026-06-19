package com.example.fitnessapp.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.databinding.ActivityRegisterBinding
import com.example.fitnessapp.repository.AuthRepository
import com.example.fitnessapp.utils.PasswordStrength
import com.example.fitnessapp.utils.ValidationUtils
import com.example.fitnessapp.viewmodels.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository
    private val authViewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                authRepository = AuthRepository(this@RegisterActivity)
                return AuthViewModel(authRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPasswordStrengthChecker()
        setupObservers()
        setupClickListeners()
    }

    private fun setupPasswordStrengthChecker() {
        binding.passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val strength = ValidationUtils.checkPasswordStrength(password)

                binding.passwordStrengthBar.progress = ValidationUtils.getPasswordStrengthProgress(strength)
                binding.passwordStrengthBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    ValidationUtils.getPasswordStrengthColor(strength)
                )
                binding.passwordStrengthText.text = ValidationUtils.getPasswordStrengthMessage(strength)
                binding.passwordStrengthText.setTextColor(ValidationUtils.getPasswordStrengthColor(strength))
            }
        })
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.registerButton.isEnabled = !isLoading
        }

        authViewModel.registerResult.observe(this) { result ->
            if (result.success) {
                Toast.makeText(this, "Registration successful! Please complete your profile.", Toast.LENGTH_LONG).show()
                // New users always go to Personalize page
                startActivity(Intent(this, PersonalizeActivity::class.java))
                finish()
            } else {
                binding.errorText.text = result.message
                binding.errorText.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            binding.errorText.visibility = View.GONE
            binding.usernameLayout.error = null
            binding.emailLayout.error = null
            binding.passwordLayout.error = null
            binding.confirmPasswordLayout.error = null

            var hasError = false

            if (username.isEmpty()) {
                binding.usernameLayout.error = "Username is required"
                hasError = true
            } else if (username.length < 3) {
                binding.usernameLayout.error = "Username must be at least 3 characters"
                hasError = true
            }

            if (email.isEmpty()) {
                binding.emailLayout.error = "Email is required"
                hasError = true
            } else if (!ValidationUtils.isValidEmail(email)) {
                binding.emailLayout.error = "Please enter a valid email address"
                hasError = true
            }

            if (password.isEmpty()) {
                binding.passwordLayout.error = "Password is required"
                hasError = true
            } else {
                val strength = ValidationUtils.checkPasswordStrength(password)
                if (strength == PasswordStrength.WEAK) {
                    binding.passwordLayout.error = "Password is too weak. Please use 8+ characters with uppercase, lowercase, numbers, and special characters."
                    hasError = true
                }
            }

            if (confirmPassword.isEmpty()) {
                binding.confirmPasswordLayout.error = "Please confirm your password"
                hasError = true
            } else if (password != confirmPassword) {
                binding.confirmPasswordLayout.error = "Passwords do not match"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            authViewModel.register(username, email, password)
        }

        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}