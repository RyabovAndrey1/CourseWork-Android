package ru.ryabov.studentperformance.ui.groups

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.ActivityGroupDetailBinding
import ru.ryabov.studentperformance.ui.adapter.StudentAdapter
import ru.ryabov.studentperformance.ui.students.StudentDetailActivity
import ru.ryabov.studentperformance.viewmodel.GroupDetailViewModel

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var viewModel: GroupDetailViewModel
    private lateinit var studentAdapter: StudentAdapter

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(ru.ryabov.studentperformance.R.string.group_details)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1)
        if (groupId == -1L) {
            finish()
            return
        }

        val repository = (application as StudentPerformanceApp).database.let { db ->
            StudentRepository(
                db.userDao(), db.studentDao(), db.gradeDao(),
                db.subjectDao(), db.groupDao(), db.facultyDao(), db.gradeTypeDao()
            )
        }
        viewModel = ViewModelProvider(this, GroupDetailViewModel.Factory(repository))[GroupDetailViewModel::class.java]

        studentAdapter = StudentAdapter(
            onStudentClick = { student ->
                startActivity(Intent(this, StudentDetailActivity::class.java).apply {
                    putExtra(StudentDetailActivity.EXTRA_STUDENT_ID, student.studentId)
                })
            }
        )
        binding.rvStudents.layoutManager = LinearLayoutManager(this)
        binding.rvStudents.adapter = studentAdapter

        viewModel.loadGroupAndStudents(groupId)

        viewModel.group.observe(this) { group ->
            if (group != null) {
                binding.tvGroupName.text = group.name
                binding.tvGroupInfo.text = buildString {
                    group.specialization?.let { append("$it. ") }
                    group.admissionYear?.let { append("Год набора: $it") }
                }.ifBlank { null } ?: ""
                studentAdapter.groupName = group.name
                studentAdapter.notifyDataSetChanged()
            }
        }
        viewModel.students.observe(this) { students ->
            studentAdapter.submitList(students)
            binding.tvStudentsSection.text = getString(ru.ryabov.studentperformance.R.string.students_count, students.size)
            binding.emptyStudents.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
            binding.rvStudents.visibility = if (students.isEmpty()) View.GONE else View.VISIBLE
        }
        val role = ru.ryabov.studentperformance.data.remote.SessionManager(applicationContext).userRole
        binding.btnJournal.visibility = if (role == "TEACHER" || role == "ADMIN" || role == "DEANERY") View.VISIBLE else View.GONE
        binding.btnJournal.setOnClickListener {
            startActivity(android.content.Intent(this, ru.ryabov.studentperformance.ui.admin.JournalActivity::class.java))
        }
    }
}