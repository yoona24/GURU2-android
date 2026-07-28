package com.guru2.payday

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.auth.PasswordHasher
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.data.local.UserEntity
import com.guru2.payday.databinding.ActivitySignUpBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // AuthViewModel 초기화
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // 회원가입 입력 필드들의 변경을 감지하는 텍스트 워처 정의
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                // 텍스트가 바뀔 때마다 회원가입 입력값 전체 유효성 검사 수행
                viewModel.validateSignUpInputs(
                    binding.emailInput.text.toString(),
                    binding.passwordInput.text.toString(),
                    binding.passwordConfirmInput.text.toString(),
                    binding.nicknameInput.text.toString(),
                )
            }
        }

        // 모든 입력 필드에 텍스트 워처 일괄 등록
        listOf(
            binding.emailInput,
            binding.passwordInput,
            binding.passwordConfirmInput,
            binding.nicknameInput,
        ).forEach { it.addTextChangedListener(watcher) }

        // 회원가입 버튼 활성화 상태 변화를 관찰하여 버튼 활성화 여부 및 투명도 조절
        viewModel.isSignEnabled.observe(this) { enabled ->
            binding.signUpButton.isEnabled = enabled
            binding.signUpButton.alpha = if (enabled) 1f else 0.45f
        }

        // 뒤로 가기 버튼 클릭 시 현재 액티비티 종료
        binding.backButton.setOnClickListener { finish() }

        // 회원가입 버튼 클릭 시 실행되는 리스너
        binding.signUpButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString()
            val pwConfirm = binding.passwordConfirmInput.text.toString()
            val nickname = binding.nicknameInput.text.toString().trim()

            // 입력 양식이 유효하지 않은 경우 토스트 메시지 출력 후 리턴
            if (!viewModel.areSignUpInputsValid(email, pw, pwConfirm, nickname)) {
                Toast.makeText(this, "입력 양식을 확인해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비동기로 데이터베이스에서 중복 이메일 체크 및 신규 사용자 정보 저장 수행
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val userDao = PaydayDatabase.getInstance(this@SignUpActivity).userDao()
                    // 이미 존재하는 이메일인 경우 회원가입 실패(false) 반환
                    if (userDao.getByEmail(email) != null) {
                        return@withContext false
                    }
                    // 비밀번호 암호화를 위한 솔트(Salt) 생성 후 해시화하여 사용자 정보 저장
                    val salt = PasswordHasher.createSalt()
                    userDao.insert(
                        UserEntity(
                            email = email,
                            passwordHash = PasswordHasher.hash(pw, salt),
                            passwordSalt = salt,
                            nickname = nickname,
                        ),
                    )
                    true
                }

                // 중복 이메일로 인해 가입에 실패한 경우 에러 토스트 표시
                if (!result) {
                    Toast.makeText(
                        this@SignUpActivity,
                        "이미 계정이 있는 이메일 주소입니다.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }

                // 회원가입 완료 토스트 메시지 출력 후 화면 종료
                Toast.makeText(
                    this@SignUpActivity,
                    R.string.sign_up_complete,
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
        }
    }
}
