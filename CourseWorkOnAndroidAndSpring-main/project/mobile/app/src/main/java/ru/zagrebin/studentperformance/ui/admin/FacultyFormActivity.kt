package ru.ryabov.studentperformance.ui.admin

import android.os.Bundle
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
import ru.ryabov.studentperformance.databinding.ActivityFacultyFormBinding

class FacultyFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultyFormBinding
    private var facultyId: Long? = null

    companion object {
        const val EXTRA_ID = "extra_faculty_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultyFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        facultyId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        if (facultyId != null) loadFaculty()

        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadFaculty() {
        val id = facultyId ?: return
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getFacultyById(id) }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val dto = resp.body()?.data
                    if (dto != null) {
                        binding.name.setText(dto.name)
                        binding.deanName.setText(dto.deanName ?: "")
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
        val deanName = binding.deanName.text?.toString()?.trim() ?: ""
        val dto = FacultyDto(facultyId = facultyId ?: 0L, name = name, deanName = deanName.ifBlank { null })

        isSaving = true
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = if (facultyId != null) {
                    withContext(Dispatchers.IO) { api.updateFaculty(facultyId!!, dto) }
                } else {
                    withContext(Dispatchers.IO) { api.createFaculty(dto) }
                }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    Toast.makeText(this@FacultyFormActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    setResult(android.app.Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@FacultyFormActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FacultyFormActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                binding.btnSave.isEnabled = true
            }
        }
    }
}
