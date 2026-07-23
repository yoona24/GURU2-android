package com.guru2.payday

import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns

class AuthViewModel : ViewModel(){

    //화면(UI)으로 전달할 인증 성공 여부 상태 LiveData
    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> get() = _authResult

    //화면(UI)으로 전달할 에러 메세지 상태 LiveData
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    // 회원가입 '가입하기' 버튼 활성화 상태 LiveData
    private val _isSignUpEnabled = MutableLiveData<Boolean>(false)
    val isSignEnabled: LiveData<Boolean> get() = _isSignUpEnabled

    // 로그인 '로그인' 버튼 활성화 상태 LiveData
    private val _isLoginEnabled = MutableLiveData<Boolean>(false)
    val isLoginEnabled: LiveData<Boolean> get() = _isLoginEnabled

    // 회원가입 입력값이 바뀔 때마다 호출하여 버튼 활성화 여부를 결정하는 함수
    fun validateSignUpInputs(email: String, pw: String, pwConfirm: String, nickname: String){
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val pwRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        val isPwValid = pw.matches(pwRegex)
        val isMatch = (pw == pwConfirm)
        val isNickValid = nickname.length in 2..10

        // 모든 조건이 참일 때만 버튼 활성화(true)
        _isSignUpEnabled.value = isEmailValid && isPwValid && isMatch && isNickValid
    }

    // 로그인 입력값이 바뀔 때마다 호출하여 버튼 활성화 여부를 결정하는 함수
    fun validateLoginInputs(email: String, pw: String){
        _isLoginEnabled.value = email.isNotBlank() && pw.isNotBlank()
    }
    /**
     * 회원가입 유효성 검사 및 처리 함수
 * -이메일 형식, 비밀번호 8자 이상 영문 + 숫자, 비밀번호 확인 일치, 닉네임 2~10자 검사
     */
    fun signUp(email: String, pw: String, pwConfirm: String, nickname: String){
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val pwRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        val isPwValid = pw.matches(pwRegex)
        val isMatch = (pw == pwConfirm)
        val isNickValid = nickname.length in 2..10

        //유효성 검사 실패 시 에러 메세지 전달 후 종료
        if(!isEmailValid || !isPwValid || !isMatch || !isNickValid){
            _errorMessage.value = "입력 양식을 확인해주세요. "
            _authResult.value = false
            return
        }

        // 이미 가입된 이메일 예외 처리
        if (email == "exist@email.com"){
            _errorMessage.value = "이미 가입된 이메일입니다."
            _authResult.value = false
            return
        }

        //유효성 통과 시 가입 성공 처리 (추후 Firebase 연동부)
        _authResult.value = true
    }

    /**
     * 로그인 입력 값 검증 함수
     * - 이메일과 비밀번호 공백 여부 확인
     */
    fun login(email: String, pw: String){
        if(email.isBlank() || pw.isBlank()){
            _errorMessage.value = "이메일 또는 비밀번호를 확인해주세요. "
            _authResult.value = false
            return
        }

        // 예시용 로그인 실패 분기
        if(email != "correct@email.com" || pw != "password123"){
            _errorMessage.value = "이메일 또는 비밀번호를 확인해주세요."
            _authResult.value = false
            return
        }

        // 로그인 성공 처리
        _authResult.value = true
    }

}