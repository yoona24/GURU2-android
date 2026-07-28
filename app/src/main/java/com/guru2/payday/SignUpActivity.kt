package com.guru2.payday

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.guru2.payday.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.validateSignUpInputs(
                    binding.emailInput.text.toString(),
                    binding.passwordInput.text.toString(),
                    binding.passwordConfirmInput.text.toString(),
                    binding.nicknameInput.text.toString(),
                )
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }

        listOf(
            binding.emailInput,
            binding.passwordInput,
            binding.passwordConfirmInput,
            binding.nicknameInput,
        ).forEach { it.addTextChangedListener(watcher) }

        viewModel.isSignEnabled.observe(this) { enabled ->
            binding.signUpButton.isEnabled = enabled
            binding.signUpButton.alpha = if (enabled) 1f else 0.45f
        }

        binding.backButton.setOnClickListener { finish() }

        // 🔍 [수정됨] SharedPreferences를 이용해 직접 즉시 가입 처리
        binding.signUpButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString()
            val pwConfirm = binding.passwordConfirmInput.text.toString()
            val nickname = binding.nicknameInput.text.toString()

            val pwRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || !pw.matches(pwRegex) || pw != pwConfirm || nickname.length !in 2..10) {
                Toast.makeText(this, "입력 양식을 확인해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("UserAuthPrefs", Context.MODE_PRIVATE)
            val savedEmail = sharedPref.getString("KEY_EMAIL", null)

            // 이미 가입된 이메일인 경우 요청하신 에러 메시지 출력
            if (savedEmail != null && savedEmail == email) {
                Toast.makeText(this, "이미 계정이 있는 이메일 주소입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 가입 정보 저장
            sharedPref.edit().apply {
                putString("KEY_EMAIL", email)
                putString("KEY_PW", pw)
                apply()
            }

            Toast.makeText(this, R.string.sign_up_complete, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
