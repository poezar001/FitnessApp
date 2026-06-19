package com.example.fitnessapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.models.WeeklyProgress
import com.example.fitnessapp.repository.MainRepository
import com.example.fitnessapp.utils.ApiGoal
import com.example.fitnessapp.utils.BMICalculator
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: MainRepository) : ViewModel() {

    private val _weeklyProgress = MutableLiveData<List<WeeklyProgress>>()
    val weeklyProgress: LiveData<List<WeeklyProgress>> = _weeklyProgress

    private val _activityDistribution = MutableLiveData<Map<String, Float>>()
    val activityDistribution: LiveData<Map<String, Float>> = _activityDistribution

    private val _goals = MutableLiveData<List<ApiGoal>>()
    val goals: LiveData<List<ApiGoal>> = _goals

    private val _bmi = MutableLiveData<Double>()
    val bmi: LiveData<Double> = _bmi

    private val _bmiStatus = MutableLiveData<String>()
    val bmiStatus: LiveData<String> = _bmiStatus

    private val _goalProgress = MutableLiveData<Int>()
    val goalProgress: LiveData<Int> = _goalProgress

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val analytics = repository.getAnalytics()
                _weeklyProgress.value = analytics.weeklyProgress
                _activityDistribution.value = analytics.activityDistribution
                _goals.value = analytics.goals
                _bmi.value = analytics.bmi
                _bmiStatus.value = BMICalculator.getBMIStatus(analytics.bmi)

                // Calculate overall goal progress
                if (analytics.goals.isNotEmpty()) {
                    val avgProgress = analytics.goals.map { it.progress }.average().toInt()
                    _goalProgress.value = avgProgress
                } else {
                    _goalProgress.value = 0
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
