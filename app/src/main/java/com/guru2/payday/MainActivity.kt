package com.guru2.payday

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
        viewModel.authResult.observe(this) { success ->
            if (success) {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotBlank()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        binding.loginButton.setOnClickListener {
            viewModel.login(
                binding.emailInput.text.toString(),
                binding.passwordInput.text.toString(),
            )
        }
        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
