package com.guru2.payday

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.auth.PasswordHasher
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 구버전의 평문 인증 정보가 남아있는 경우 SharedPreferences 초기화
        clearLegacyPlainTextCredentials()

        // AuthViewModel 초기화
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // 이메일과 비밀번호 입력란의 변경을 감지하는 텍스트 워처 정의
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 입력값이 바뀔 때마다 로그인 입력 유효성 검사 수행
                viewModel.validateLoginInputs(
                    binding.emailInput.text.toString(),
                    binding.passwordInput.text.toString(),
                )
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }

        // 입력 필드에 텍스트 워처 등록
        binding.emailInput.addTextChangedListener(watcher)
        binding.passwordInput.addTextChangedListener(watcher)

        // 로그인 버튼 활성화 상태 변화를 관찰하여 버튼 활성화 여부 및 투명도 조절
        viewModel.isLoginEnabled.observe(this) { enabled ->
            binding.loginButton.isEnabled = enabled
            binding.loginButton.alpha = if (enabled) 1f else 0.45f
        }

        // 로그인 버튼 클릭 시 실행되는 리스너
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString()

            // 이메일이나 비밀번호가 비어있는 경우 토스트 메시지 출력 후 리턴
            if (email.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "이메일 또는 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비동기로 데이터베이스에서 사용자 정보를 조회하여 로그인 검증 수행
            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    PaydayDatabase.getInstance(this@MainActivity).userDao().getByEmail(email)
                }

                // 사용자가 존재하지 않거나 비밀번호가 일치하지 않는 경우 에러 토스트 표시
                if (user == null || !PasswordHasher.verify(pw, user.passwordSalt, user.passwordHash)) {
                    Toast.makeText(
                        this@MainActivity,
                        "등록되지 않은 회원 이메일이거나 비밀번호가 틀렸습니다.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }

                // 로그인 성공 시 세션에 유저 ID를 저장하고 대시보드 화면으로 이동 후 현재 액티비티 종료
                UserSession(this@MainActivity).signIn(user.id)
                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                finish()
            }
        }

        // 회원가입 버튼 클릭 시 회원가입 화면으로 이동
        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    // SharedPreferences에 저장되어 있던 레거시 평문 인증 정보를 삭제하는 함수
    private fun clearLegacyPlainTextCredentials() {
        getSharedPreferences("UserAuthPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
