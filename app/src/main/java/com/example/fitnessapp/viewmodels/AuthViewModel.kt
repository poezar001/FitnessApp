package com.example.fitnessapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.models.AuthResponse
import com.example.fitnessapp.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<AuthResponse>()
    val loginResult: LiveData<AuthResponse> = _loginResult

    private val _registerResult = MutableLiveData<AuthResponse>()
    val registerResult: LiveData<AuthResponse> = _registerResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.login(email, password)
            _loginResult.value = result
            _isLoading.value = false
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.register(username, email, password)
            _registerResult.value = result
            _isLoading.value = false
        }
    }
}