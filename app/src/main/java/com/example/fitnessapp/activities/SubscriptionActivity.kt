package com.example.fitnessapp.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivitySubscriptionBinding

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private var selectedPlan: String = "annual"  // Default selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPlanSelection()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupPlanSelection() {
        // Select annual plan by default
        selectPlan("annual")
    }

    private fun selectPlan(plan: String) {
        selectedPlan = plan

        // Reset both cards to default
        resetCard(binding.cardAnnual)
        resetCard(binding.cardMonthly)

        // Apply selection to chosen plan
        if (plan == "annual") {
            selectCard(binding.cardAnnual)
        } else {
            selectCard(binding.cardMonthly)
        }
    }

    private fun resetCard(card: CardView) {
        card.setCardBackgroundColor(resources.getColor(R.color.surface, null))
        card.cardElevation = 2f
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(resources.getColor(R.color.surface, null))
        drawable.cornerRadius = 16f
        card.background = drawable
        // Uncheck radio buttons
        binding.radioAnnual.isChecked = false
        binding.radioMonthly.isChecked = false
    }

    private fun selectCard(card: CardView) {
        card.setCardBackgroundColor(resources.getColor(R.color.primary_dark, null))
        card.cardElevation = 4f
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(resources.getColor(R.color.primary_dark, null))
        drawable.setStroke(3, resources.getColor(R.color.primary, null))
        drawable.cornerRadius = 16f
        card.background = drawable
        // Check corresponding radio button
        if (card.id == R.id.cardAnnual) {
            binding.radioAnnual.isChecked = true
            binding.radioMonthly.isChecked = false
        } else {
            binding.radioMonthly.isChecked = true
            binding.radioAnnual.isChecked = false
        }
    }

    private fun setupClickListeners() {
        // Click on Annual card
        binding.cardAnnual.setOnClickListener {
            selectPlan("annual")
        }

        // Click on Monthly card
        binding.cardMonthly.setOnClickListener {
            selectPlan("monthly")
        }

        // Click on radio buttons
        binding.radioAnnual.setOnClickListener {
            selectPlan("annual")
        }
        binding.radioMonthly.setOnClickListener {
            selectPlan("monthly")
        }

        // Try Free Button
        binding.btnTryFree.setOnClickListener {
            val planName = if (selectedPlan == "annual") "Annual" else "Monthly"
            Toast.makeText(
                this,
                "✅ You've started your 1 Month Free Trial for $planName plan!",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}