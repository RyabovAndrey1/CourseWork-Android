package ru.ryabov.studentperformance.ui.admin

import android.app.Activity
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
import ru.ryabov.studentperformance.data.remote.dto.CreateStudentRequest
import ru.ryabov.studentperformance.data.remote.dto.GroupDto
import ru.ryabov.studentperformance.data.remote.dto.StudentDto
import ru.ryabov.studentperformance.databinding.ActivityStudentFormBinding

class StudentFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentFormBinding
    private var studentId: Long? = null
    private var groups = listOf<GroupDto>()

    companion object { const val EXTRA_ID = "extra_student_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Разрешаем любой текст (в т.ч. кириллицу): тип «текст» и снимаем любые фильтры символов
        listOf(binding.lastName, binding.firstName, binding.middleName).forEach { edit ->
            edit.inputType = InputType.TYPE_CLASS_TEXT
            edit.setRawInputType(InputType.TYPE_CLASS_TEXT)
            edit.filters = emptyArray()
        }

        studentId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        if (studentId != null) binding.login.isEnabled = false
        loadGroupsAndStudent()

        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadGroupsAndStudent() {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val gResp = withContext(Dispatchers.IO) { api.getGroups() }
                if (gResp.isSuccessful && gResp.body()?.success == true) {
                    groups = gResp.body()?.data ?: emptyList()
                    val names = listOf("— Не выбрано —") + groups.map { it.name }
                    binding.spinnerGroup.adapter = ArrayAdapter(this@StudentFormActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                val id = studentId
                if (id != null) {
                    val sResp = withContext(Dispatchers.IO) { api.getStudentByIdRest(id) }
                    if (sResp.isSuccessful && sResp.body()?.success == true) {
                        val dto = sResp.body()?.data
                        if (dto != null) fillForm(dto)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun fillForm(dto: StudentDto) {
        binding.login.setText(dto.login ?: "")
        binding.email.setText(dto.email ?: "")
        val parts = (dto.fullName ?: "").trim().split("\\s+".toRegex())
        binding.lastName.setText(parts.getOrNull(0) ?: "")
        binding.firstName.setText(parts.getOrNull(1) ?: "")
        binding.middleName.setText(parts.getOrNull(2) ?: "")
        binding.recordBookNumber.setText(dto.recordBookNumber ?: "")
        binding.admissionYear.setText(dto.admissionYear?.toString() ?: "")
        val idx = groups.indexOfFirst { it.groupId == dto.groupId }
        binding.spinnerGroup.setSelection(if (idx >= 0) idx + 1 else 0)
    }

    private var isSaving = false

    private fun save() {
        if (isSaving) return
        val login = binding.login.text?.toString()?.trim() ?: ""
        val email = binding.email.text?.toString()?.trim() ?: ""
        val lastName = binding.lastName.text?.toString()?.trim() ?: ""
        val firstName = binding.firstName.text?.toString()?.trim() ?: ""
        if (studentId == null && login.isBlank()) {
            Toast.makeText(this, "Введите логин", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isBlank()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastName.isBlank() || firstName.isBlank()) {
            Toast.makeText(this, "Введите фамилию и имя", Toast.LENGTH_SHORT).show()
            return
        }
        val middleName = binding.middleName.text?.toString()?.trim()?.ifBlank { null }
        val password = binding.password.text?.toString()?.trim()?.ifBlank { null }
        if (studentId == null && !password.isNullOrBlank() && password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не короче 6 символов", Toast.LENGTH_SHORT).show()
            return
        }
        val pos = binding.spinnerGroup.selectedItemPosition
        val groupId = if (pos > 0 && pos <= groups.size) groups[pos - 1].groupId else null
        val recordBookNumber = binding.recordBookNumber.text?.toString()?.trim()?.ifBlank { null }
        val admissionYear = binding.admissionYear.text?.toString()?.trim()?.toIntOrNull()

        val request = CreateStudentRequest(
            login = login.ifBlank { "student${studentId ?: System.currentTimeMillis()}" },
            password = password,
            email = email,
            lastName = lastName,
            firstName = firstName,
            middleName = middleName,
            groupId = groupId,
            recordBookNumber = recordBookNumber,
            admissionYear = admissionYear
        )

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = if (studentId != null) {
                    withContext(Dispatchers.IO) { api.updateStudent(studentId!!, request) }
                } else {
                    withContext(Dispatchers.IO) { api.createStudent(request) }
                }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    Toast.makeText(this@StudentFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@StudentFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StudentFormActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }
}
