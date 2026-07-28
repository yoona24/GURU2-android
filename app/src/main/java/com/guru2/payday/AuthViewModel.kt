package com.guru2.payday

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

    // 회원가입 버튼 활성화 상태를 관리하는 LiveData
    private val _isSignUpEnabled = MutableLiveData<Boolean>(false)
    val isSignEnabled: LiveData<Boolean> get() = _isSignUpEnabled

    // 로그인 버튼 활성화 상태를 관리하는 LiveData
    private val _isLoginEnabled = MutableLiveData<Boolean>(false)
    val isLoginEnabled: LiveData<Boolean> get() = _isLoginEnabled

    // 회원가입 입력값을 검증하고 그 결과를 _isSignUpEnabled에 반영하는 함수
    fun validateSignUpInputs(email: String, pw: String, pwConfirm: String, nickname: String) {
        _isSignUpEnabled.value = areSignUpInputsValid(email, pw, pwConfirm, nickname)
    }

    // 회원가입 각 입력값의 유효성(이메일 형식, 비밀번호 정규식, 비밀번호 일치, 닉네임 길이)을 검사하는 함수
    fun areSignUpInputsValid(
        email: String,
        pw: String,
        pwConfirm: String,
        nickname: String,
    ): Boolean {
        val normalizedEmail = email.trim()
        val normalizedNickname = nickname.trim()
        // 이메일 주소 형식 검증
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
        // 비밀번호 정규식 검증 (영문, 숫자가 포함된 8자 이상 공백 없는 문자열)
        val pwRegex = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,}$".toRegex()
        val isPwValid = pw.matches(pwRegex)
        // 비밀번호와 비밀번호 확인 일치 여부 확인
        val isMatch = pw == pwConfirm
        // 닉네임 길이 검증 (2~10자)
        val isNickValid = normalizedNickname.length in 2..10
        // 모든 조건이 충족될 때만 true 반환
        return isEmailValid && isPwValid && isMatch && isNickValid
    }

    // 로그인 입력값(이메일, 비밀번호)이 비어있지 않은지 검사하여 상태를 반영하는 함수
    fun validateLoginInputs(email: String, pw: String) {
        _isLoginEnabled.value = email.isNotBlank() && pw.isNotBlank()
    }
}
