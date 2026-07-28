package com.guru2.payday

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

    private val _isSignUpEnabled = MutableLiveData<Boolean>(false)
    val isSignEnabled: LiveData<Boolean> get() = _isSignUpEnabled

    private val _isLoginEnabled = MutableLiveData<Boolean>(false)
    val isLoginEnabled: LiveData<Boolean> get() = _isLoginEnabled

    fun validateSignUpInputs(email: String, pw: String, pwConfirm: String, nickname: String) {
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val pwRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        val isPwValid = pw.matches(pwRegex)
        val isMatch = (pw == pwConfirm)
        val isNickValid = nickname.length in 2..10

        _isSignUpEnabled.value = isEmailValid && isPwValid && isMatch && isNickValid
    }

    fun validateLoginInputs(email: String, pw: String) {
        _isLoginEnabled.value = email.isNotBlank() && pw.isNotBlank()
    }
}
