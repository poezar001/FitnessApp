package com.example.fitnessapp.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivityForgotPasswordBinding
import com.example.fitnessapp.repository.AuthRepository
import com.example.fitnessapp.utils.PasswordStrength
import com.example.fitnessapp.utils.ValidationUtils
import com.example.fitnessapp.viewmodels.AuthViewModel

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var authRepository: AuthRepository
    private var generatedOTP: String = ""
    private var verifiedEmail: String = ""
    private var timerCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private val authViewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                authRepository = AuthRepository(this@ForgotPasswordActivity)
                return AuthViewModel(authRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        setupOTPTimer()
        setupPasswordStrengthChecker()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnResetPassword.isEnabled = !isLoading
        }

        authViewModel.resetPasswordResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.btnResetPassword.isEnabled = true

            if (result.success) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                // Navigate back to login
                finish()
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOTPTimer() {
        timerRunnable = Runnable {
            if (timerCount > 0) {
                timerCount--
                binding.tvTimer.text = "Resend OTP in ${timerCount}s"
                handler.postDelayed(timerRunnable, 1000)
            } else {
                binding.tvTimer.text = "Resend OTP"
                binding.btnResendOTP.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSendOTP.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (!ValidationUtils.isValidEmail(email)) {
                binding.etEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }
            verifiedEmail = email
            sendOTP(email)
        }

        binding.btnVerifyOTP.setOnClickListener {
            val otp = binding.etOTP.text.toString().trim()
            if (otp.isEmpty()) {
                binding.etOTP.error = "OTP is required"
                return@setOnClickListener
            }
            verifyOTP(otp)
        }

        binding.btnResetPassword.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (newPassword.isEmpty()) {
                binding.etNewPassword.error = "Password is required"
                return@setOnClickListener
            }

            val strength = ValidationUtils.checkPasswordStrength(newPassword)
            if (strength == PasswordStrength.WEAK) {
                binding.etNewPassword.error = "Password is too weak. Use 8+ characters with uppercase, lowercase, numbers, and special characters."
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                binding.etConfirmPassword.error = "Please confirm your password"
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            resetPassword(newPassword)
        }

        binding.btnResendOTP.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendOTP(email)
            }
        }
    }

    private fun sendOTP(email: String) {
        generatedOTP = String.format("%06d", (0..999999).random())

        binding.tvOTPDisplay.text = "Your OTP: $generatedOTP"
        binding.tvOTPDisplay.visibility = View.VISIBLE

        binding.layoutEmail.visibility = View.GONE
        binding.layoutOTP.visibility = View.VISIBLE

        timerCount = 60
        binding.btnResendOTP.visibility = View.GONE
        handler.postDelayed(timerRunnable, 0)

        Toast.makeText(this, "OTP sent to $email", Toast.LENGTH_SHORT).show()
    }

    private fun verifyOTP(otp: String) {
        if (otp == generatedOTP) {
            binding.layoutOTP.visibility = View.GONE
            binding.layoutResetPassword.visibility = View.VISIBLE
            Toast.makeText(this, "OTP verified successfully!", Toast.LENGTH_SHORT).show()
        } else {
            binding.etOTP.error = "Invalid OTP. Please try again."
            Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetPassword(newPassword: String) {
        // Call the API to reset password
        authViewModel.resetPassword(verifiedEmail, newPassword)
    }

    private fun setupPasswordStrengthChecker() {
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}