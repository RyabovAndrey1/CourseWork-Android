package ru.ryabov.studentperformance.ui.subjects

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.ActivitySubjectDetailBinding
import ru.ryabov.studentperformance.ui.adapter.GradeAdapter
import ru.ryabov.studentperformance.viewmodel.GradeWithDetails

class SubjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectDetailBinding
    private lateinit var gradeAdapter: GradeAdapter

    companion object {
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(ru.ryabov.studentperformance.R.string.subject_details)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1)
        if (subjectId != -1L) {
            loadSubjectDetails(subjectId)
        } else {
            finish()
        }
    }

    private fun loadSubjectDetails(subjectId: Long) {
        val repository = (application as StudentPerformanceApp).database.let { db ->
            StudentRepository(
                db.userDao(), db.studentDao(), db.gradeDao(),
                db.subjectDao(), db.groupDao(), db.facultyDao(), db.gradeTypeDao()
            )
        }
        lifecycleScope.launch {
            val subject = repository.getSubjectById(subjectId)
            if (subject == null) {
                binding.tvSubjectName.text = "Дисциплина не найдена"
                binding.tvSubjectHours.text = ""
                binding.tvControlType.text = ""
                return@launch
            }
            binding.tvSubjectName.text = subject.name
            val hours = subject.totalHours?.let { "Часов: $it" }
                ?: "Зачётных единиц: ${subject.credits}"
            binding.tvSubjectHours.text = hours
            binding.tvControlType.text = subject.controlType?.let { "Тип контроля: $it" } ?: ""
            binding.tvControlType.visibility = if (subject.controlType.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            binding.tvDescription.text = subject.description?.takeIf { it.isNotBlank() }
            binding.tvDescription.visibility = if (!subject.description.isNullOrBlank()) android.view.View.VISIBLE else android.view.View.GONE

            gradeAdapter = GradeAdapter(onGradeClick = { /* optional: open grade detail */ })
            binding.rvSubjectGrades.layoutManager = LinearLayoutManager(this@SubjectDetailActivity)
            binding.rvSubjectGrades.adapter = gradeAdapter
            val sessionManager = SessionManager(applicationContext)
            val studentId = sessionManager.userId?.let { repository.getStudentByUserId(it)?.studentId }
            if (studentId != null) {
                repository.getGradesByStudentAndSubject(studentId, subjectId).first().let { grades ->
                    val withDetails = grades.map { g ->
                        GradeWithDetails(g, subject.name)
                    }
                    gradeAdapter.submitList(withDetails)
                }
            }
        }
    }
}
