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
import ru.ryabov.studentperformance.data.remote.dto.SubjectDto
import ru.ryabov.studentperformance.databinding.ActivitySubjectFormBinding

class SubjectFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectFormBinding
    private var subjectId: Long? = null

    companion object { const val EXTRA_ID = "extra_subject_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val types = listOf("— Не выбрано —", "EXAM", "CREDIT", "DIFF_CREDIT")
        binding.spinnerControlType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        subjectId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        if (subjectId != null) loadSubject()

        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadSubject() {
        val id = subjectId ?: return
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getSubjectById(id) }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val dto = resp.body()?.data
                    if (dto != null) {
                        binding.name.setText(dto.name)
                        binding.code.setText(dto.code ?: "")
                        binding.totalHours.setText(dto.totalHours?.toString() ?: "")
                        binding.description.setText(dto.description ?: "")
                        val idx = when (dto.controlType?.uppercase()) {
                            "EXAM" -> 1
                            "CREDIT" -> 2
                            "DIFF_CREDIT" -> 3
                            else -> 0
                        }
                        binding.spinnerControlType.setSelection(idx)
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
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }
        val code = binding.code.text?.toString()?.trim()?.ifBlank { null }
        val totalHours = binding.totalHours.text?.toString()?.trim()?.toIntOrNull()
        val controlIdx = binding.spinnerControlType.selectedItemPosition
        val controlType = when (controlIdx) {
            1 -> "EXAM"
            2 -> "CREDIT"
            3 -> "DIFF_CREDIT"
            else -> null
        }
        val description = binding.description.text?.toString()?.trim()?.ifBlank { null }

        val dto = SubjectDto(
            subjectId = subjectId ?: 0L,
            name = name,
            code = code,
            totalHours = totalHours,
            controlType = controlType,
            description = description
        )

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = if (subjectId != null) {
                    withContext(Dispatchers.IO) { api.updateSubject(subjectId!!, dto) }
                } else {
                    withContext(Dispatchers.IO) { api.createSubject(dto) }
                }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    Toast.makeText(this@SubjectFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    setResult(android.app.Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@SubjectFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SubjectFormActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }
}
