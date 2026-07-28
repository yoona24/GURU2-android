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
        clearLegacyPlainTextCredentials()

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

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString()

            if (email.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "이메일 또는 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    PaydayDatabase.getInstance(this@MainActivity).userDao().getByEmail(email)
                }
                if (user == null || !PasswordHasher.verify(pw, user.passwordSalt, user.passwordHash)) {
                    Toast.makeText(
                        this@MainActivity,
                        "등록되지 않은 회원 이메일이거나 비밀번호가 틀렸습니다.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }
                UserSession(this@MainActivity).signIn(user.id)
                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                finish()
            }
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun clearLegacyPlainTextCredentials() {
        getSharedPreferences("UserAuthPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
