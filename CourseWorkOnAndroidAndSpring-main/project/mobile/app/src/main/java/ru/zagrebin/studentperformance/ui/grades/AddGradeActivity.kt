package ru.ryabov.studentperformance.ui.grades

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.ActivityAddGradeBinding
import ru.ryabov.studentperformance.viewmodel.GradeViewModel

class AddGradeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"
    }

    private lateinit var binding: ActivityAddGradeBinding
    private lateinit var gradeViewModel: GradeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGradeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_grade)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupViewModel()
        setupUI()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val app = application as StudentPerformanceApp
        val database = app.database
        val repository = StudentRepository(
            database.userDao(),
            database.studentDao(),
            database.gradeDao(),
            database.subjectDao(),
            database.groupDao(),
            database.facultyDao(),
            database.gradeTypeDao()
        )
        val sessionManager = SessionManager(applicationContext)
        val api = RetrofitProvider.createApi(sessionManager)
        gradeViewModel = ViewModelProvider(this, GradeViewModel.Factory(repository, api))[GradeViewModel::class.java]
    }

    private fun setupUI() {
        gradeViewModel.subjects.observe(this) { subjects ->
            val names = subjects?.map { s -> s.name } ?: emptyList()
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerSubject.adapter = adapter
        }

        val gradeTypes = listOf("Лекция", "Практика", "Лабораторная", "Контрольная", "Экзамен")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gradeTypes)
        binding.spinnerGradeType.adapter = adapter
    }

    private fun setupClickListeners() {
        gradeViewModel.addGradeResult.observe(this) { result ->
            if (result == null) return@observe
            binding.btnSave.isEnabled = true
            if (result) {
                Toast.makeText(this, getString(R.string.grade_add_success), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSave.setOnClickListener {
            val gradeValue = binding.etGradeValue.text.toString().toDoubleOrNull()
            val comment = binding.etComment.text.toString().takeIf { it.isNotBlank() }

            if (gradeValue == null || gradeValue < 1.0 || gradeValue > 5.0) {
                Toast.makeText(this, "Введите корректную оценку от 1 до 5", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val studentId = intent.getLongExtra(EXTRA_STUDENT_ID, -1L)
            if (studentId == -1L) {
                Toast.makeText(this, "Не указан студент", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val subjectPosition = binding.spinnerSubject.selectedItemPosition
            val subject = gradeViewModel.subjects.value?.getOrNull(subjectPosition)
            if (subject == null) {
                Toast.makeText(this, "Выберите дисциплину", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnSave.isEnabled = false
            gradeViewModel.addGrade(
                studentId = studentId,
                subjectId = subject.subjectId,
                gradeValue = gradeValue,
                gradeTypeId = binding.spinnerGradeType.selectedItemPosition + 1L,
                comment = comment
            )
        }
    }
}
