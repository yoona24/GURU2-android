package com.guru2.payday

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
        viewModel.authResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, R.string.sign_up_complete, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        viewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        binding.backButton.setOnClickListener { finish() }
        binding.signUpButton.setOnClickListener {
            viewModel.signUp(
                binding.emailInput.text.toString(),
                binding.passwordInput.text.toString(),
                binding.passwordConfirmInput.text.toString(),
                binding.nicknameInput.text.toString(),
            )
        }
    }
}
