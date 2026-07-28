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
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.validateSignUpInputs(
                    binding.emailInput.text.toString(),
                    binding.passwordInput.text.toString(),
                    binding.passwordConfirmInput.text.toString(),
                    binding.nicknameInput.text.toString(),
                )
            }
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

        binding.signUpButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString()
            val pwConfirm = binding.passwordConfirmInput.text.toString()
            val nickname = binding.nicknameInput.text.toString().trim()

            if (!viewModel.areSignUpInputsValid(email, pw, pwConfirm, nickname)) {
                Toast.makeText(this, "입력 양식을 확인해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val userDao = PaydayDatabase.getInstance(this@SignUpActivity).userDao()
                    if (userDao.getByEmail(email) != null) {
                        return@withContext false
                    }
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
                if (!result) {
                    Toast.makeText(
                        this@SignUpActivity,
                        "이미 계정이 있는 이메일 주소입니다.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }
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
