package com.example.fitnessapp.fragments

import android.graphics.Color
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.fitnessapp.R
import com.example.fitnessapp.base.BaseFragment
import com.example.fitnessapp.databinding.FragmentAnalyticsBinding
import com.example.fitnessapp.models.WeeklyProgress
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.BMICalculator
import com.example.fitnessapp.utils.ChartUtils
import com.example.fitnessapp.utils.DateUtils
import com.example.fitnessapp.viewmodels.AnalyticsViewModel
import java.text.DecimalFormat

class AnalyticsFragment : BaseFragment<FragmentAnalyticsBinding>(R.layout.fragment_analytics) {

    private lateinit var viewModel: AnalyticsViewModel

    override fun initViewModel() {
        val repository = MainRepository(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(repository) as T
            }
        }).get(AnalyticsViewModel::class.java)
    }

    override fun setupUI() {
        setupCharts()
    }

    private fun setupCharts() {
        val days = DateUtils.getWeekDays()

        // Weekly Bar Chart
        viewModel.weeklyProgress.observe(viewLifecycleOwner) { progress ->
            if (progress != null) {
                val values = progress.map { it.duration.toFloat() }
                ChartUtils.setupBarChart(binding.weeklyBarChart, days, values, Color.parseColor("#00FF00"))

                // Calories Line Chart
                val caloriesValues = progress.map { it.calories.toFloat() }
                ChartUtils.setupLineChart(binding.caloriesLineChart, days, caloriesValues, Color.parseColor("#FF6584"))

                // Add dynamically generated insights
                addInsights(progress)
            }
        }

        // Pie Chart
        viewModel.activityDistribution.observe(viewLifecycleOwner) { distribution ->
            if (distribution != null) {
                val colors = listOf(
                    Color.parseColor("#00FF00"),
                    Color.parseColor("#FF6584"),
                    Color.parseColor("#FFA500")
                )
                ChartUtils.setupPieChart(binding.pieChart, distribution, colors)
            }
        }
    }

    override fun setupObservers() {
        // Observe BMI value and dynamic health statuses
        viewModel.bmi.observe(viewLifecycleOwner) { bmi ->
            if (bmi != null) {
                val df = DecimalFormat("#.#")
                binding.bmiValue.text = df.format(bmi)

                val status = viewModel.bmiStatus.value ?: BMICalculator.getBMIStatus(bmi)
                binding.bmiStatus.text = status
                binding.healthStatus.text = status

                val statusColor = BMICalculator.getBMIColor(bmi)
                binding.bmiStatus.setTextColor(statusColor)
                binding.healthStatus.setTextColor(statusColor)
            }
        }

        // FIX: Fallback to calorie target progress if backend goals list is empty
        viewModel.goalProgress.observe(viewLifecycleOwner) { progressValue ->
            val explicitProgress = progressValue ?: 0

            if (explicitProgress > 0) {
                binding.goalProgressBar.progress = explicitProgress.coerceIn(0, 100)
                binding.goalProgressText.text = "$explicitProgress% completed"
            } else {
                // Calculate progress based on weekly burned calories divided by a baseline goal (e.g., 3500 kcal)
                val progressList = viewModel.weeklyProgress.value
                if (!progressList.isNullOrEmpty()) {
                    val totalCalories = progressList.sumOf { it.calories }
                    val calculatedProgress = if (totalCalories > 0) {
                        ((totalCalories.toFloat() / 3500) * 100).toInt()
                    } else 0

                    binding.goalProgressBar.progress = calculatedProgress.coerceIn(0, 100)
                    binding.goalProgressText.text = "$calculatedProgress% completed"
                } else {
                    binding.goalProgressBar.progress = 0
                    binding.goalProgressText.text = "0% completed"
                }
            }
        }
    }

    private fun addInsights(progress: List<WeeklyProgress>) {
        binding.insightsContainer.removeAllViews()
        if (progress.isEmpty()) return

        val bestDay = progress.maxByOrNull { it.calories }
        val totalCalories = progress.sumOf { it.calories }
        val avgCalories = totalCalories / progress.size
        val progressPercentage = viewModel.goalProgress.value ?: 0

        val insights = listOf(
            "💪 Your best day was ${bestDay?.day ?: "N/A"} with ${bestDay?.calories ?: 0} calories!",
            "📊 Average daily calories: $avgCalories kcal",
            "🎯 You're $progressPercentage% to your weekly goal!",
            "🔥 Keep up the great work!"
        )

        insights.forEach { insight ->
            val textView = TextView(requireContext()).apply {
                this.text = insight
                this.setTextColor(resources.getColor(R.color.text_secondary, null))
                this.textSize = 12f
                this.setPadding(0, 8, 0, 8)
            }
            binding.insightsContainer.addView(textView)
        }
    }

    override fun loadData() {
        viewModel.loadData()
    }
}