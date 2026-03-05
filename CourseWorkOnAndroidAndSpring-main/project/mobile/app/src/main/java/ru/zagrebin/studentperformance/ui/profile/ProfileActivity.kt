package ru.ryabov.studentperformance.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.repository.AuthRepository
import ru.ryabov.studentperformance.databinding.ActivityProfileStudentBinding
import ru.ryabov.studentperformance.ui.AuthActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(getString(ru.ryabov.studentperformance.R.string.profile_title))
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.userId
        val role = sessionManager.userRole ?: ""

        // Данные из сессии (с последнего входа) — сразу отображаем
        binding.tvUserName.text = sessionManager.userFullName?.takeIf { it.isNotBlank() }
            ?: "Пользователь #$userId"
        binding.tvUserEmail.text = sessionManager.userEmail?.takeIf { it.isNotBlank() } ?: ""
        binding.tvUserRole.text = role.ifBlank { "—" }

        // Загрузка актуальных данных пользователя с сервера (из БД)
        if (userId != null) {
            lifecycleScope.launch {
                try {
                    val api = RetrofitProvider.createApi(sessionManager)
                    val resp = withContext(Dispatchers.IO) { api.getUserById(userId) }
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        val user = resp.body()!!.data!!
                        val fullName = listOf(user.lastName, user.firstName, user.middleName)
                            .filterNotNull().joinToString(" ").trim().takeIf { it.isNotBlank() }
                        binding.tvUserName.text = fullName ?: user.login ?: "Пользователь #$userId"
                        binding.tvUserEmail.text = user.email?.takeIf { it.isNotBlank() } ?: ""
                        sessionManager.userEmail = user.email
                        sessionManager.userFullName = fullName
                    }
                } catch (_: Exception) { /* оставляем данные из сессии */ }
            }
        }

        // Админ-панель: ADMIN, DEANERY, TEACHER
        val showAdmin = role == "ADMIN" || role == "DEANERY" || role == "TEACHER"
        binding.btnAdminPanel.visibility = if (showAdmin) View.VISIBLE else View.GONE
        binding.btnAdminPanel.setOnClickListener {
            startActivity(Intent(this, ru.ryabov.studentperformance.ui.admin.AdminPanelActivity::class.java))
        }

        binding.btnChangePassword.setOnClickListener { showChangePasswordByEmailDialog(sessionManager) }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            finishAffinity()
            startActivity(Intent(this, AuthActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfileFromServer()
    }

    private fun refreshProfileFromServer() {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.userId ?: return
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(sessionManager)
                val resp = withContext(Dispatchers.IO) { api.getUserById(userId) }
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    val user = resp.body()!!.data!!
                    val fullName = listOf(user.lastName, user.firstName, user.middleName).filterNotNull().joinToString(" ").trim().takeIf { it.isNotBlank() }
                    binding.tvUserName.text = fullName ?: user.login ?: "Пользователь #$userId"
                    binding.tvUserEmail.text = user.email?.takeIf { it.isNotBlank() } ?: ""
                    sessionManager.userEmail = user.email
                    sessionManager.userFullName = fullName
                }
            } catch (_: Exception) { }
        }
    }

    /** Смена пароля через email: диалог с полем email → отправка ссылки на почту (forgot-password). */
    private fun showChangePasswordByEmailDialog(sessionManager: SessionManager) {
        val email = sessionManager.userEmail?.takeIf { it.isNotBlank() } ?: ""
        val input = EditText(this).apply {
            setText(email)
            hint = getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_hint_email)
            setPadding(48, 24, 48, 24)
            minWidth = 280
        }
        AlertDialog.Builder(this)
            .setTitle(getString(ru.ryabov.studentperformance.R.string.profile_change_password_btn))
            .setMessage(getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_message))
            .setView(input)
            .setPositiveButton(getString(ru.ryabov.studentperformance.R.string.auth_forgot_password_send)) { _, _ ->
                val e = input.text?.toString()?.trim().orEmpty()
                if (e.isBlank()) {
                    Toast.makeText(this, getString(ru.ryabov.studentperformance.R.string.auth_enter_email), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val api = RetrofitProvider.createApi(sessionManager)
                        val repo = AuthRepository(api, sessionManager)
                        withContext(Dispatchers.IO) { repo.forgotPassword(e).getOrThrow() }
                        Toast.makeText(this@ProfileActivity, getString(ru.ryabov.studentperformance.R.string.auth_email_sent_reset), Toast.LENGTH_LONG).show()
                    } catch (ex: Exception) {
                        Toast.makeText(this@ProfileActivity, ex.message ?: getString(ru.ryabov.studentperformance.R.string.profile_password_error), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(getString(ru.ryabov.studentperformance.R.string.cancel), null)
            .show()
    }
}
