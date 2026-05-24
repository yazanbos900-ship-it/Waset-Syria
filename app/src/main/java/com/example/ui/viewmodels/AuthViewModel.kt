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
        if (username.isBlank() || phone.isBlank() || email.isBlank() || pass1.isBlank() || pass2.isBlank()) {
            _registerState.value = Resource.Error("الرجاء ملء جميع الحقول")
            return
        }
        
        if (username.contains(" ")) {
            _registerState.value = Resource.Error("اسم المستخدم لا يمكن أن يحتوي على مسافات")
            return
        }

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)\$")
        if (!emailRegex.matches(email.trim())) {
            _registerState.value = Resource.Error("الرجاء إدخال بريد إلكتروني صحيح")
            return
        }

        if (pass1 != pass2) {
            _registerState.value = Resource.Error("كلمات المرور غير متطابقة")
            return
        }

        if (pass1.length < 6) {
            _registerState.value = Resource.Error("يجب أن تكون كلمة المرور 6 أحرف على الأقل")
            return
        }

        val cleanPhone = phone.trim()
        if (cleanPhone.length < 9) {
            _registerState.value = Resource.Error("يرجى إدخال رقم هاتف صحيح")
            return
        }

        _registerState.value = Resource.Loading
        
        viewModelScope.launch {
            val result = repository.registerUser(username.trim(), cleanPhone, email.trim(), pass1)
            when (result) {
                is Resource.Success -> {
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
            _loginState.value = Resource.Error("الرجاء إدخال بيانات الدخول")
            return
        }
        
        _loginState.value = Resource.Loading
        
        viewModelScope.launch {
            val result = repository.loginUser(identifier.trim(), pass)
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
