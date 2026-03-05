package ru.ryabov.studentperformance.ui.students

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.ActivityStudentDetailBinding
import ru.ryabov.studentperformance.ui.adapter.GradeAdapter
import ru.ryabov.studentperformance.ui.grades.AddGradeActivity
import ru.ryabov.studentperformance.ui.grades.GradeDetailActivity
import ru.ryabov.studentperformance.viewmodel.GradeWithDetails

class StudentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDetailBinding

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(ru.ryabov.studentperformance.R.string.student_details)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val studentId = intent.getLongExtra(EXTRA_STUDENT_ID, -1)
        if (studentId == -1L) {
            finish()
            return
        }
        // Кнопка «Добавить оценку» только для TEACHER, ADMIN, DEANERY (как в веб-интерфейсе по ролям)
        val sessionManager = SessionManager(applicationContext)
        val role = sessionManager.userRole ?: ""
        val canAddGrade = role == "TEACHER" || role == "ADMIN" || role == "DEANERY"
        binding.btnAddGrade.visibility = if (canAddGrade) View.VISIBLE else View.GONE
        binding.btnAddGrade.setOnClickListener {
            startActivity(Intent(this, AddGradeActivity::class.java).apply {
                putExtra(AddGradeActivity.EXTRA_STUDENT_ID, studentId)
            })
        }
        loadStudentDetails(studentId)
    }

    private fun loadStudentDetails(studentId: Long) {
        val repository = (application as StudentPerformanceApp).database.let { db ->
            StudentRepository(
                db.userDao(), db.studentDao(), db.gradeDao(),
                db.subjectDao(), db.groupDao(), db.facultyDao(), db.gradeTypeDao()
            )
        }
        val adapter = GradeAdapter(
            onGradeClick = { g -> startActivity(Intent(this, GradeDetailActivity::class.java).putExtra(GradeDetailActivity.EXTRA_GRADE_ID, g.grade.gradeId)) }
        )
        binding.rvGrades.layoutManager = LinearLayoutManager(this)
        binding.rvGrades.adapter = adapter

        lifecycleScope.launch {
            val student = repository.getStudentById(studentId)
            val userName = student?.userId?.let { repository.getUserByIdFlow(it).first()?.fullName }
            binding.tvStudentName.text = userName ?: "Студент #$studentId"
            binding.tvRecordBook.text = student?.recordBookNumber?.let { "Зачётная книжка: $it" } ?: ""
            binding.tvRecordBook.visibility = if (student?.recordBookNumber.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            val avg = repository.getAverageGradeByStudent(studentId)
            binding.tvAverageGrade.text = avg?.let { "%.2f".format(it) } ?: "—"
            val grades = repository.getGradesByStudentSync(studentId)
            binding.tvGradesCount.text = grades.size.toString()
            val withDetails = grades.map { g -> GradeWithDetails(g, repository.getSubjectById(g.subjectId)?.name ?: "—") }
            adapter.submitList(withDetails)
        }
    }
}
