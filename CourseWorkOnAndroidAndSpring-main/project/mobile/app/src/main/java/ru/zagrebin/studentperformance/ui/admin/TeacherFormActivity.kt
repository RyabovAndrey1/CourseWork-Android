package ru.ryabov.studentperformance.ui.admin

import android.os.Bundle
import android.view.View
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
import ru.ryabov.studentperformance.data.remote.dto.DepartmentDto
import ru.ryabov.studentperformance.data.remote.dto.TeacherDto
import ru.ryabov.studentperformance.data.remote.dto.UserDto
import ru.ryabov.studentperformance.databinding.ActivityTeacherFormBinding

class TeacherFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherFormBinding
    private var teacherId: Long? = null
    private var departments = listOf<DepartmentDto>()
    private var usersForCreate = listOf<UserDto>()

    companion object { const val EXTRA_ID = "extra_teacher_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        teacherId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        binding.tvUserLabel.visibility = if (teacherId == null) View.VISIBLE else View.GONE
        binding.spinnerUser.visibility = if (teacherId == null) View.VISIBLE else View.GONE

        loadData()
        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val dResp = withContext(Dispatchers.IO) { api.getDepartments(null) }
                if (dResp.isSuccessful && dResp.body()?.success == true) {
                    departments = dResp.body()?.data ?: emptyList()
                    val names = listOf("— Не выбрано —") + departments.map { it.name }
                    binding.spinnerDepartment.adapter = ArrayAdapter(this@TeacherFormActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                if (teacherId == null) {
                    val (uResp, tResp) = withContext(Dispatchers.IO) { api.getUsers() to api.getTeachers() }
                    val teacherUserIds = (tResp.body()?.data ?: emptyList()).mapNotNull { it.userId }.toSet()
                    if (uResp.isSuccessful && uResp.body()?.success == true) {
                        usersForCreate = (uResp.body()?.data ?: emptyList()).filter { it.userId !in teacherUserIds }
                        val userNames = listOf("— Выберите пользователя —") + usersForCreate.map { "${it.login} (${it.lastName ?: ""} ${it.firstName ?: ""})".trim() }
                        binding.spinnerUser.adapter = ArrayAdapter(this@TeacherFormActivity, android.R.layout.simple_spinner_dropdown_item, userNames)
                    }
                } else {
                    val tResp = withContext(Dispatchers.IO) { api.getTeacherById(teacherId!!) }
                    if (tResp.isSuccessful && tResp.body()?.success == true) {
                        val t = tResp.body()?.data
                        if (t != null) {
                            binding.academicDegree.setText(t.academicDegree ?: "")
                            binding.position.setText(t.position ?: "")
                            val idx = departments.indexOfFirst { it.departmentId == t.departmentId }
                            binding.spinnerDepartment.setSelection(if (idx >= 0) idx + 1 else 0)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private var isSaving = false

    private fun save() {
        if (isSaving) return
        val pos = binding.spinnerDepartment.selectedItemPosition
        val departmentId = if (pos > 0 && pos <= departments.size) departments[pos - 1].departmentId else null
        val academicDegree = binding.academicDegree.text?.toString()?.trim()?.ifBlank { null }
        val position = binding.position.text?.toString()?.trim()?.ifBlank { null }

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                if (teacherId != null) {
                    val dto = TeacherDto(
                        teacherId = teacherId!!,
                        userId = null,
                        fullName = null,
                        login = null,
                        email = null,
                        academicDegree = academicDegree,
                        position = position,
                        departmentName = null,
                        facultyName = null,
                        departmentId = departmentId
                    )
                    val resp = withContext(Dispatchers.IO) { api.updateTeacher(teacherId!!, dto) }
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        Toast.makeText(this@TeacherFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                        setResult(android.app.Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@TeacherFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val userPos = binding.spinnerUser.selectedItemPosition
                    if (userPos <= 0 || userPos > usersForCreate.size) {
                        Toast.makeText(this@TeacherFormActivity, "Выберите пользователя", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val userId = usersForCreate[userPos - 1].userId
                    val dto = TeacherDto(
                        teacherId = 0L,
                        userId = userId,
                        fullName = null,
                        login = null,
                        email = null,
                        academicDegree = academicDegree,
                        position = position,
                        departmentName = null,
                        facultyName = null,
                        departmentId = departmentId
                    )
                    val resp = withContext(Dispatchers.IO) { api.createTeacher(dto) }
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        Toast.makeText(this@TeacherFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                        setResult(android.app.Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@TeacherFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@TeacherFormActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }
}
