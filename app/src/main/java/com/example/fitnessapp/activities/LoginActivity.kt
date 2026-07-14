package com.example.fitnessapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.databinding.ActivityLoginBinding
import com.example.fitnessapp.repository.AuthRepository
import com.example.fitnessapp.utils.ValidationUtils
import com.example.fitnessapp.viewmodels.AuthViewModel

// CRITICAL FIX: Explicitly import your target destination activities
// if they are inside a subfolder or different directory
import com.example.fitnessapp.activities.MainDashboardActivity
import com.example.fitnessapp.activities.PersonalizeActivity
import com.example.fitnessapp.activities.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository

    // Clean up ViewModel instantiation to avoid lazy race conditions
    private val authViewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                // Ensure repository context exists before passing it to ViewModel
                if (!::authRepository.isInitialized) {
                    authRepository = AuthRepository(this@LoginActivity)
                }
                return AuthViewModel(authRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIX: Guarantee authRepository is fully initialized as soon as the screen loads
        authRepository = AuthRepository(this)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = !isLoading
        }

        authViewModel.loginResult.observe(this) { result ->
            if (result.success) {
                Toast.makeText(this, "Login successful! Welcome back!", Toast.LENGTH_LONG).show()

                // Safe execution now that repository initialization is guaranteed
                val hasProfile = authRepository.hasUserProfile()

                if (hasProfile) {
                    // User has a completed profile -> Send directly to MainDashboard
                    startActivity(Intent(this, MainDashboardActivity::class.java))
                } else {
                    // User profile is missing or incomplete -> Complete profile setup
                    startActivity(Intent(this, PersonalizeActivity::class.java))
                }
                finish() // Destroy login interface from active stack frame
            } else {
                binding.errorText.text = result.message
                binding.errorText.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            binding.errorText.visibility = View.GONE
            binding.emailLayout.error = null
            binding.passwordLayout.error = null

            var hasError = false

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
            }

            if (hasError) return@setOnClickListener

            authViewModel.login(email, password)
        }

        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.registerLinkBold.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}