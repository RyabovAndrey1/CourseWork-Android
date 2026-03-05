package ru.ryabov.studentperformance.ui.admin

import android.os.Bundle
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
import ru.ryabov.studentperformance.data.remote.dto.FacultyDto
import ru.ryabov.studentperformance.databinding.ActivityDepartmentFormBinding

class DepartmentFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDepartmentFormBinding
    private var departmentId: Long? = null
    private var faculties = listOf<FacultyDto>()

    companion object { const val EXTRA_ID = "extra_department_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepartmentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        departmentId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        loadFacultiesAndDepartment()

        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadFacultiesAndDepartment() {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val fResp = withContext(Dispatchers.IO) { api.getFaculties() }
                if (fResp.isSuccessful && fResp.body()?.success == true) {
                    faculties = fResp.body()?.data ?: emptyList()
                    val names = listOf("— Не выбрано —") + faculties.map { it.name }
                    binding.spinnerFaculty.adapter = ArrayAdapter(this@DepartmentFormActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                val id = departmentId
                if (id != null) {
                    val dResp = withContext(Dispatchers.IO) { api.getDepartmentById(id) }
                    if (dResp.isSuccessful && dResp.body()?.success == true) {
                        val dto = dResp.body()?.data
                        if (dto != null) {
                            binding.name.setText(dto.name)
                            binding.headName.setText(dto.headName ?: "")
                            val idx = faculties.indexOfFirst { it.facultyId == dto.facultyId }
                            binding.spinnerFaculty.setSelection(if (idx >= 0) idx + 1 else 0)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private var isSaving = false

    private fun save() {
        if (isSaving) return
        val name = binding.name.text?.toString()?.trim() ?: ""
        if (name.isBlank()) {
            Toast.makeText(this, "Введите название кафедры", Toast.LENGTH_SHORT).show()
            return
        }
        val pos = binding.spinnerFaculty.selectedItemPosition
        val facultyId = if (pos > 0 && pos <= faculties.size) faculties[pos - 1].facultyId else null
        val headName = binding.headName.text?.toString()?.trim()?.ifBlank { null }
        val dto = DepartmentDto(departmentId = departmentId ?: 0L, name = name, facultyId = facultyId, facultyName = null, headName = headName)

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = if (departmentId != null) {
                    withContext(Dispatchers.IO) { api.updateDepartment(departmentId!!, dto) }
                } else {
                    withContext(Dispatchers.IO) { api.createDepartment(dto) }
                }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    Toast.makeText(this@DepartmentFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    setResult(android.app.Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@DepartmentFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DepartmentFormActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }
}
