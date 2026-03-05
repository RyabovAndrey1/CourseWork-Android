package ru.ryabov.studentperformance.ui.admin

import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.remote.dto.CreateUserRequest
import ru.ryabov.studentperformance.data.remote.dto.UpdateUserRequest
import ru.ryabov.studentperformance.data.remote.dto.UserDto
import ru.ryabov.studentperformance.databinding.ActivityUserFormBinding

class UserFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserFormBinding
    private var userId: Long? = null
    private val roles = listOf("ADMIN", "DEANERY", "TEACHER", "STUDENT")

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.spinnerRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        // Разрешаем любой текст (в т.ч. кириллицу): тип «текст» и снимаем любые фильтры символов
        listOf(binding.lastName, binding.firstName, binding.middleName).forEach { edit ->
            edit.inputType = InputType.TYPE_CLASS_TEXT
            edit.setRawInputType(InputType.TYPE_CLASS_TEXT)
            edit.filters = emptyArray()
        }

        userId = intent.getLongExtra(EXTRA_USER_ID, -1L).takeIf { it > 0 }
        if (userId != null) {
            supportActionBar?.title = "Редактирование пользователя"
            binding.login.isEnabled = false
            binding.layoutPassword.hint = "Пароль (оставьте пустым, чтобы не менять)"
            loadUser()
        } else {
            supportActionBar?.title = "Новый пользователь"
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private var isSaving = false

    private fun save() {
        if (isSaving) return
        val login = binding.login.text?.toString()?.trim() ?: ""
        val password = binding.password.text?.toString()?.trim()?.ifBlank { null }
        val email = binding.email.text?.toString()?.trim()?.ifBlank { null }
        val lastName = binding.lastName.text?.toString()?.trim() ?: ""
        val firstName = binding.firstName.text?.toString()?.trim() ?: ""
        val middleName = binding.middleName.text?.toString()?.trim()?.ifBlank { null }
        val role = roles.getOrNull(binding.spinnerRole.selectedItemPosition) ?: "STUDENT"

        if (login.isBlank()) {
            Toast.makeText(this, "Введите логин", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null && password.isNullOrBlank()) {
            Toast.makeText(this, "Введите пароль для нового пользователя", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastName.isBlank() || firstName.isBlank()) {
            Toast.makeText(this, "Введите фамилию и имя", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null && !password.isNullOrBlank() && password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не короче 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                performSave(login, password, email, lastName, firstName, middleName, role)
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }

    private suspend fun performSave(
        login: String,
        password: String?,
        email: String?,
        lastName: String,
        firstName: String,
        middleName: String?,
        role: String
    ) {
        try {
            val api = RetrofitProvider.createApi(SessionManager(applicationContext))
            val resp = if (userId != null) {
                val updateReq = UpdateUserRequest(
                    email = email,
                    lastName = lastName,
                    firstName = firstName,
                    middleName = middleName,
                    role = role,
                    password = password
                )
                withContext(Dispatchers.IO) { api.updateUser(userId!!, updateReq) }
            } else {
                val request = CreateUserRequest(
                    login = login,
                    password = password,
                    email = email,
                    lastName = lastName,
                    firstName = firstName,
                    middleName = middleName,
                    role = role
                )
                withContext(Dispatchers.IO) { api.createUser(request) }
            }
            if (resp.isSuccessful && resp.body()?.success == true) {
                val sessionManager = SessionManager(applicationContext)
                if (userId != null && userId == sessionManager.userId) {
                    val d = resp.body()?.data
                    if (d != null) {
                        sessionManager.userEmail = d.email
                        val fullName = listOf(d.lastName, d.firstName, d.middleName).filterNotNull().joinToString(" ").trim().takeIf { it.isNotBlank() }
                        sessionManager.userFullName = fullName
                    }
                }
                Toast.makeText(this@UserFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                setResult(android.app.Activity.RESULT_OK)
                finish()
            } else {
                val msg = resp.body()?.message
                    ?: resp.errorBody()?.string()?.take(200)
                    ?: "Ошибка (код ${resp.code()})"
                Toast.makeText(this@UserFormActivity, msg, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this@UserFormActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadUser() {
        val id = userId ?: return
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getUserById(id) }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val dto = resp.body()?.data ?: return@launch
                    binding.login.setText(dto.login ?: "")
                    binding.email.setText(dto.email ?: "")
                    binding.lastName.setText(dto.lastName ?: "")
                    binding.firstName.setText(dto.firstName ?: "")
                    binding.middleName.setText(dto.middleName ?: "")
                    val roleIndex = roles.indexOf(dto.role ?: "STUDENT").coerceAtLeast(0)
                    binding.spinnerRole.setSelection(roleIndex)
                }
            } catch (_: Exception) {}
        }
    }
}
