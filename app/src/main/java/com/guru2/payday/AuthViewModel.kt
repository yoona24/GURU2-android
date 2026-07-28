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
        _isSignUpEnabled.value = areSignUpInputsValid(email, pw, pwConfirm, nickname)
    }

    fun areSignUpInputsValid(
        email: String,
        pw: String,
        pwConfirm: String,
        nickname: String,
    ): Boolean {
        val normalizedEmail = email.trim()
        val normalizedNickname = nickname.trim()
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
        val pwRegex = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,}$".toRegex()
        val isPwValid = pw.matches(pwRegex)
        val isMatch = pw == pwConfirm
        val isNickValid = normalizedNickname.length in 2..10
        return isEmailValid && isPwValid && isMatch && isNickValid
    }

    fun validateLoginInputs(email: String, pw: String) {
        _isLoginEnabled.value = email.isNotBlank() && pw.isNotBlank()
    }
}
