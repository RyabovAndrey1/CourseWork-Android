package ru.ryabov.studentperformance.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.databinding.ActivityAuthStudentBinding
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.repository.AuthRepository
import ru.ryabov.studentperformance.viewmodel.AuthUiState
import ru.ryabov.studentperformance.viewmodel.AuthViewModel

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthStudentBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionManager = SessionManager(applicationContext)
        val api = RetrofitProvider.createApi(sessionManager)
        val authRepository = AuthRepository(api, sessionManager)
        authViewModel = ViewModelProvider(this, AuthViewModel.Factory(authRepository))[AuthViewModel::class.java]

        setupClickListeners()
        observeLoginState()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            authViewModel.login(
                binding.etLogin.text.toString(),
                binding.etPassword.text.toString()
            )
        }
        binding.btnForgotPassword.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(this).apply {
            hint = getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_hint_email)
            setPadding(48, 24, 48, 24)
            minWidth = 280
        }
        AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_Dialog_Alert)
            .setTitle(getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_title))
            .setMessage(getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_message))
            .setView(input)
            .setPositiveButton(getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_send)) { _, _ ->
                val email = input.text?.toString()?.trim().orEmpty()
                if (email.isBlank()) {
                    Toast.makeText(this, getString(ru.ryabov.studentperformance.R.string.auth_enter_email), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val repo = AuthRepository(RetrofitProvider.createApi(SessionManager(applicationContext)), SessionManager(applicationContext))
                        val msg = withContext(Dispatchers.IO) { repo.forgotPassword(email).getOrThrow() }
                        Toast.makeText(this@AuthActivity, getString(ru.ryabov.studentperformance.R.string.auth_email_sent_reset), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AuthActivity, e.message ?: getString(ru.ryabov.studentperformance.R.string.error), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(getString(ru.ryabov.studentperformance.R.string.cancel), null)
            .show()
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginState.collectLatest { state ->
                    when (state) {
                        is AuthUiState.Idle -> {
                            binding.progressBar.isVisible = false
                            binding.tvError.isVisible = false
                        }
                        is AuthUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.tvError.isVisible = false
                            binding.btnLogin.isEnabled = false
                        }
                        is AuthUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.btnLogin.isEnabled = true
                            Toast.makeText(this@AuthActivity, getString(ru.ryabov.studentperformance.R.string.auth_login_success), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                            finish()
                        }
                        is AuthUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.btnLogin.isEnabled = true
                            binding.tvError.isVisible = true
                            binding.tvError.text = state.message
                        }
                    }
                }
            }
        }
    }
}
