package ru.ryabov.studentperformance.ui.grades

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.ActivityGradeDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GradeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGradeDetailBinding

    companion object {
        const val EXTRA_GRADE_ID = "extra_grade_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val gradeId = intent.getLongExtra(EXTRA_GRADE_ID, -1)
        if (gradeId != -1L) {
            loadGradeDetails(gradeId)
        } else {
            finish()
        }
    }

    private fun loadGradeDetails(gradeId: Long) {
        val repository = (application as StudentPerformanceApp).database.let { db ->
            StudentRepository(
                db.userDao(), db.studentDao(), db.gradeDao(),
                db.subjectDao(), db.groupDao(), db.facultyDao(), db.gradeTypeDao()
            )
        }
        lifecycleScope.launch {
            val grade = repository.getGradeById(gradeId)
            if (grade == null) {
                binding.tvSubjectName.text = "Оценка не найдена"
                binding.tvGradeValue.text = ""
                binding.tvGradeDate.text = ""
                binding.tvComment.text = ""
                return@launch
            }
            val subject = repository.getSubjectById(grade.subjectId)
            binding.tvSubjectName.text = subject?.name ?: "Дисциплина #${grade.subjectId}"
            binding.tvGradeValue.text = "Балл: ${grade.gradeValue?.toString() ?: "—"}"
            val dateStr = try {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(grade.gradeDate))
            } catch (_: Exception) { grade.gradeDate.toString() }
            binding.tvGradeDate.text = "Дата: $dateStr"
            binding.tvComment.text = grade.comment?.takeIf { it.isNotBlank() } ?: ""
            binding.tvComment.visibility = if (grade.comment.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }
}
