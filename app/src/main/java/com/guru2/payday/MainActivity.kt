package com.guru2.payday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.guru2.payday.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.validateLoginInputs(
                    binding.emailInput.text.toString(),
                    binding.passwordInput.text.toString(),
                )
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        binding.emailInput.addTextChangedListener(watcher)
        binding.passwordInput.addTextChangedListener(watcher)

        viewModel.isLoginEnabled.observe(this) { enabled ->
            binding.loginButton.isEnabled = enabled
            binding.loginButton.alpha = if (enabled) 1f else 0.45f
        }

        //SharedPreferences에서 직접 계정 정보를 불러와 비교 후 로그인 처리
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString()

            if (email.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "이메일 또는 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("UserAuthPrefs", Context.MODE_PRIVATE)
            val savedEmail = sharedPref.getString("KEY_EMAIL", null)
            val savedPw = sharedPref.getString("KEY_PW", null)

            if (savedEmail == null || savedPw == null || email != savedEmail || pw != savedPw) {
                Toast.makeText(this, "등록되지 않은 회원 이메일이거나 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 로그인 성공 시 대시보드로 이동
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
