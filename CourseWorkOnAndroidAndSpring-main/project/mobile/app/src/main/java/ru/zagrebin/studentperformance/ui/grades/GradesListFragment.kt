package ru.ryabov.studentperformance.ui.grades

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.FragmentGradesListBinding
import ru.ryabov.studentperformance.viewmodel.GradeViewModel

/**
 * Фрагмент со списком оценок (сводка по текущему пользователю или общий список для преподавателя/админа).
 */
class GradesListFragment : Fragment() {

    private var _binding: FragmentGradesListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GradeViewModel
    private lateinit var adapter: ru.ryabov.studentperformance.ui.adapter.GradeAdapter
    private lateinit var bySubjectAdapter: ru.ryabov.studentperformance.ui.adapter.SubjectGradeSummaryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGradesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupTabs()
        setupRecyclerView()
        setupSwipeRefresh()
        observeData()
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(ru.ryabov.studentperformance.R.string.grades_tab_summary)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(ru.ryabov.studentperformance.R.string.grades_tab_by_subject)))
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                val isSummary = tab.position == 0
                binding.gradesSummaryContent.visibility = if (isSummary) View.VISIBLE else View.GONE
                binding.rvBySubject.visibility = if (isSummary) View.GONE else View.VISIBLE
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadAllGrades(
                ru.ryabov.studentperformance.data.remote.SessionManager(requireContext().applicationContext).userId,
                ru.ryabov.studentperformance.data.remote.SessionManager(requireContext().applicationContext).userRole
            )
            binding.swipeRefresh.postDelayed({ binding.swipeRefresh.isRefreshing = false }, 800)
        }
    }

    private fun setupViewModel() {
        val app = requireActivity().application as StudentPerformanceApp
        val repository = app.database.let { db ->
            StudentRepository(
                db.userDao(), db.studentDao(), db.gradeDao(),
                db.subjectDao(), db.groupDao(), db.facultyDao(), db.gradeTypeDao()
            )
        }
        val sessionManager = SessionManager(requireContext().applicationContext)
        val api = RetrofitProvider.createApi(sessionManager)
        viewModel = ViewModelProvider(this, GradeViewModel.Factory(repository, api))[GradeViewModel::class.java]
        viewModel.loadAllGrades(sessionManager.userId, sessionManager.userRole)
    }

    private fun setupRecyclerView() {
        adapter = ru.ryabov.studentperformance.ui.adapter.GradeAdapter(
            onGradeClick = { gradeWithDetails ->
                startActivity(Intent(requireContext(), GradeDetailActivity::class.java).apply {
                    putExtra(GradeDetailActivity.EXTRA_GRADE_ID, gradeWithDetails.grade.gradeId)
                })
            }
        )
        binding.rvGrades.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGrades.adapter = adapter
        bySubjectAdapter = ru.ryabov.studentperformance.ui.adapter.SubjectGradeSummaryAdapter()
        binding.rvBySubject.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBySubject.adapter = bySubjectAdapter
    }

    private fun observeData() {
        viewModel.grades.observe(viewLifecycleOwner) { grades ->
            adapter.submitList(grades)
            binding.emptyState.visibility = if (grades.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.averageGrade.observe(viewLifecycleOwner) { avg ->
            binding.tvAverageGrade.text = avg?.let { "%.2f".format(it) } ?: "—"
        }
        viewModel.totalGradesCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalGrades.text = count.toString()
        }
        viewModel.subjectSummaries.observe(viewLifecycleOwner) { list ->
            bySubjectAdapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
