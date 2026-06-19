package com.example.fitnessapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.models.Achievement
import com.example.fitnessapp.models.ActivityLevel
import com.example.fitnessapp.models.UserProfile
import com.example.fitnessapp.repository.AuthRepository
import com.example.fitnessapp.repository.MainRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: Any) : ViewModel() {

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _achievements = MutableLiveData<List<Achievement>>()
    val achievements: LiveData<List<Achievement>> = _achievements

    fun loadUserData() {
        if (repository is MainRepository) {
            _username.value = repository.getUsername()
            _email.value = repository.getUserEmail()
            _userProfile.value = repository.getUserProfile()
            _achievements.value = repository.getAchievements()
        }
    }

    fun saveProfile(
        userId: Int,
        birthday: String,
        gender: String,
        height: Float,
        weight: Float,
        activityLevel: ActivityLevel
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val profile = UserProfile(userId, birthday, gender, height, weight, activityLevel)
            val success = when (repository) {
                is AuthRepository -> repository.saveProfile(profile)
                is MainRepository -> false // MainRepository doesn't have saveProfile in this implementation
                else -> false
            }
            _saveResult.value = success
            if (!success) {
                _error.value = "Failed to save profile"
            }
            _isSaving.value = false
        }
    }

    fun logout() {
        when (repository) {
            is AuthRepository -> repository.logout()
            is MainRepository -> repository.logout()
        }
    }
}
