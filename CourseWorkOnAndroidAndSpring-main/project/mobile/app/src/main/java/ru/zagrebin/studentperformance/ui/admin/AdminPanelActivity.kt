package ru.ryabov.studentperformance.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.ActivityAdminPanelBinding
import ru.ryabov.studentperformance.viewmodel.AdminViewModel
import ru.ryabov.studentperformance.viewmodel.SystemStatistics

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var viewModel: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.admin_panel)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val role = ru.ryabov.studentperformance.data.remote.SessionManager(applicationContext).userRole
        if (role != "ADMIN" && role != "DEANERY" && role != "TEACHER") {
            finish()
            return
        }
        binding.statsScrollView.visibility = when (role) {
            "ADMIN" -> android.view.View.VISIBLE
            else -> android.view.View.GONE
        }

        setupSections(role)
        setupViewModel()
        observeData()
    }

    private fun setupSections(role: String?) {
        val container = binding.adminSectionsContainer
        val sections = mutableListOf<Pair<String, () -> Unit>>()
        if (role == "ADMIN") {
            sections.add(getString(R.string.admin_section_users) to { startActivity(Intent(this, UsersListActivity::class.java)) })
        }
        if (role != "TEACHER") {
            sections.add(getString(R.string.admin_section_students) to { startActivity(Intent(this, StudentsListActivity::class.java)) })
            sections.add(getString(R.string.admin_section_groups) to { startActivity(Intent(this, GroupsListActivity::class.java)) })
            if (role == "ADMIN") {
                sections.add(getString(R.string.admin_section_faculties) to { startActivity(Intent(this, FacultiesListActivity::class.java)) })
            }
            sections.add(getString(R.string.admin_section_departments) to { startActivity(Intent(this, DepartmentsListActivity::class.java)) })
            sections.add(getString(R.string.admin_section_subjects) to { startActivity(Intent(this, SubjectsListActivity::class.java)) })
            sections.add(getString(R.string.admin_section_teachers) to { startActivity(Intent(this, TeachersListActivity::class.java)) })
        } else {
            sections.add(getString(R.string.admin_section_students) to { startActivity(Intent(this, StudentsListActivity::class.java)) })
        }
        sections.add(getString(R.string.admin_section_journal) to { startActivity(Intent(this, JournalActivity::class.java)) })
        sections.add(getString(R.string.admin_section_reports) to { startActivity(Intent(this, ReportsActivity::class.java)) })

        for ((title, action) in sections) {
            val btn = MaterialButton(this).apply {
                text = title
                setOnClickListener { action() }
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            container.addView(btn)
        }
    }

    private fun setupViewModel() {
        val repository = application.let { app ->
            (app as StudentPerformanceApp).database.let { database ->
                StudentRepository(database.userDao(), database.studentDao(), database.gradeDao(),
                    database.subjectDao(), database.groupDao(), database.facultyDao(), database.gradeTypeDao())
            }
        }
        viewModel = ViewModelProvider(this, AdminViewModel.Factory(repository))[AdminViewModel::class.java]
    }

    private fun observeData() {
        viewModel.statistics.observe(this) { stats ->
            updateStatisticsCard(stats)
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ru.ryabov.studentperformance.viewmodel.AdminUiState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }
                is ru.ryabov.studentperformance.viewmodel.AdminUiState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                }
                is ru.ryabov.studentperformance.viewmodel.AdminUiState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun updateStatisticsCard(stats: SystemStatistics) {
        binding.tvTotalUsers.text = stats.totalUsers.toString()
        binding.tvTotalStudents.text = stats.totalStudents.toString()
        binding.tvTotalGroups.text = stats.totalGroups.toString()
        binding.tvTotalFaculties.text = stats.totalFaculties.toString()
        binding.tvTotalSubjects.text = stats.totalSubjects.toString()
    }
}