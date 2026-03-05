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
import ru.ryabov.studentperformance.data.remote.dto.FacultyDto
import ru.ryabov.studentperformance.data.remote.dto.GroupDto
import ru.ryabov.studentperformance.databinding.ActivityGroupFormBinding

class GroupFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupFormBinding
    private var groupId: Long? = null
    private var faculties = listOf<FacultyDto>()

    companion object { const val EXTRA_ID = "extra_group_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        groupId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        loadFacultiesAndGroup()

        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadFacultiesAndGroup() {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val fResp = withContext(Dispatchers.IO) { api.getFaculties() }
                if (fResp.isSuccessful && fResp.body()?.success == true) {
                    faculties = fResp.body()?.data ?: emptyList()
                    val names = listOf("— Не выбрано —") + faculties.map { it.name }
                    binding.spinnerFaculty.adapter = ArrayAdapter(this@GroupFormActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                val id = groupId
                if (id != null) {
                    val gResp = withContext(Dispatchers.IO) { api.getGroupById(id) }
                    if (gResp.isSuccessful && gResp.body()?.success == true) {
                        val dto = gResp.body()?.data
                        if (dto != null) {
                            binding.name.setText(dto.name)
                            binding.admissionYear.setText(dto.admissionYear?.toString() ?: "")
                            binding.specialization.setText(dto.specialization ?: "")
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
            Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
            return
        }
        val pos = binding.spinnerFaculty.selectedItemPosition
        val facultyId = if (pos > 0 && pos <= faculties.size) faculties[pos - 1].facultyId else null
        val yearStr = binding.admissionYear.text?.toString()?.trim()
        val admissionYear = yearStr?.toIntOrNull()
        val specialization = binding.specialization.text?.toString()?.trim()?.ifBlank { null }
        val dto = GroupDto(
            groupId = groupId ?: 0L,
            name = name,
            facultyName = null,
            facultyId = facultyId,
            admissionYear = admissionYear,
            specialization = specialization,
            studentCount = 0
        )

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = if (groupId != null) {
                    withContext(Dispatchers.IO) { api.updateGroup(groupId!!, dto) }
                } else {
                    withContext(Dispatchers.IO) { api.createGroup(dto) }
                }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    Toast.makeText(this@GroupFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    setResult(android.app.Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@GroupFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupFormActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }
}
