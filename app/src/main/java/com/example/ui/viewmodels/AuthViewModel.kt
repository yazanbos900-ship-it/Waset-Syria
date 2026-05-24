package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.utils.PasswordHasher
import com.example.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: AuthRepository

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        repository = AuthRepository(userDao, application.applicationContext)
    }

    private val _registerState = MutableStateFlow<Resource<Unit>?>(null)
    val registerState: StateFlow<Resource<Unit>?> = _registerState.asStateFlow()

    private val _loginState = MutableStateFlow<Resource<Unit>?>(null)
    val loginState: StateFlow<Resource<Unit>?> = _loginState.asStateFlow()

    private val _usernameAvailability = MutableStateFlow<Resource<Boolean>?>(null)
    val usernameAvailability: StateFlow<Resource<Boolean>?> = _usernameAvailability.asStateFlow()

    private var usernameCheckJob: kotlinx.coroutines.Job? = null

    fun checkUsername(username: String) {
        if (username.isBlank()) {
            _usernameAvailability.value = null
            return
        }
        
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Debounce
            _usernameAvailability.value = Resource.Loading
            val isAvailable = repository.checkUsernameAvailability(username.trim())
            _usernameAvailability.value = isAvailable
        }
    }

    fun register(username: String, phone: String, email: String, pass1: String, pass2: String) {
        // Validation Layer
        if (username.isBlank() || phone.isBlank() || pass1.isBlank() || pass2.isBlank()) {
            _registerState.value = Resource.Error("Please fill in all required fields")
            return
        }
        
        if (username.contains(" ")) {
            _registerState.value = Resource.Error("Username cannot contain spaces")
            return
        }

        if (pass1 != pass2) {
            _registerState.value = Resource.Error("Passwords do not match")
            return
        }

        if (pass1.length < 6) {
            _registerState.value = Resource.Error("Password must be at least 6 characters")
            return
        }

        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.length < 10 || (!cleanPhone.startsWith("+9639") && !cleanPhone.startsWith("09") && !cleanPhone.startsWith("9639"))) {
            _registerState.value = Resource.Error("Please enter a valid Syrian phone number (e.g. 09... or +9639...)")
            return
        }

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)\$")
        if (email.isNotBlank() && !emailRegex.matches(email)) {
             _registerState.value = Resource.Error("Invalid email format")
            return
        }

        _registerState.value = Resource.Loading
        
        viewModelScope.launch {
            val result = repository.registerUser(username, cleanPhone, email, pass1)
            when (result) {
                is Resource.Success -> {
                    // Successful registration
                    _registerState.value = Resource.Success(Unit)
                }
                is Resource.Error -> {
                    _registerState.value = Resource.Error(result.message)
                }
                else -> {}
            }
        }
    }
    
    fun login(identifier: String, pass: String) {
        if (identifier.isBlank() || pass.isBlank()) {
            _loginState.value = Resource.Error("Please enter your credentials")
            return
        }
        
        _loginState.value = Resource.Loading
        
        viewModelScope.launch {
            val result = repository.loginUser(identifier, pass)
            when (result) {
                is Resource.Success -> _loginState.value = Resource.Success(Unit)
                is Resource.Error -> _loginState.value = Resource.Error(result.message)
                else -> {}
            }
        }
    }
    
    fun resetStates() {
        _registerState.value = null
        _loginState.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
