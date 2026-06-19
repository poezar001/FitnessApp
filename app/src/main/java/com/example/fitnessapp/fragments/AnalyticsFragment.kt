package com.example.fitnessapp.fragments

import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import com.example.fitnessapp.R
import com.example.fitnessapp.base.BaseFragment
import com.example.fitnessapp.databinding.FragmentAnalyticsBinding
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
            val values = progress.map { it.duration.toFloat() }
            ChartUtils.setupBarChart(binding.weeklyBarChart, days, values, Color.parseColor("#00FF00"))

            // Calories Line Chart
            val caloriesValues = progress.map { it.calories.toFloat() }
            ChartUtils.setupLineChart(binding.caloriesLineChart, days, caloriesValues, Color.parseColor("#FF6584"))
        }

        // Pie Chart
        viewModel.activityDistribution.observe(viewLifecycleOwner) { distribution ->
            val colors = listOf(
                Color.parseColor("#00FF00"),
                Color.parseColor("#FF6584"),
                Color.parseColor("#FFA500")
            )
            ChartUtils.setupPieChart(binding.pieChart, distribution, colors)
        }
    }

    override fun setupObservers() {
        viewModel.bmi.observe(viewLifecycleOwner) { bmi ->
            val df = DecimalFormat("#.#")
            binding.bmiValue.text = df.format(bmi)
            binding.bmiStatus.text = viewModel.bmiStatus.value ?: ""
            binding.bmiStatus.setTextColor(BMICalculator.getBMIColor(bmi))
        }

        viewModel.weeklyProgress.observe(viewLifecycleOwner) { progress ->
            val totalCalories = progress.sumOf { it.calories }
            val goalProgress = (totalCalories.toFloat() / 3500 * 100).toInt()
            binding.goalProgressBar.progress = goalProgress
            binding.goalProgressText.text = "$goalProgress% completed"

            // Add insights
            addInsights(progress)
        }
    }

    private fun addInsights(progress: List<com.example.fitnessapp.models.WeeklyProgress>) {
        binding.insightsContainer.removeAllViews()

        val bestDay = progress.maxByOrNull { it.calories }
        val totalCalories = progress.sumOf { it.calories }
        val avgCalories = totalCalories / progress.size

        val insights = listOf(
            "💪 Your best day was ${bestDay?.day} with ${bestDay?.calories} calories!",
            "📊 Average daily calories: $avgCalories kcal",
            "🎯 You're ${(totalCalories.toFloat() / 3500 * 100).toInt()}% to your weekly goal!",
            "🔥 Keep up the great work!"
        )

        insights.forEach { insight ->
            val textView = android.widget.TextView(requireContext()).apply {
                this.text = insight
                this.setTextColor(resources.getColor(com.example.fitnessapp.R.color.text_secondary, null))
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