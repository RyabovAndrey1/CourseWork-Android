package ru.ryabov.studentperformance.ui.subjects

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.FragmentSubjectsBinding
import ru.ryabov.studentperformance.viewmodel.GradeViewModel

class SubjectsFragment : Fragment() {

    private var _binding: FragmentSubjectsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GradeViewModel
    private lateinit var adapter: ru.ryabov.studentperformance.ui.adapter.SubjectAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupSwipeRefresh()
        observeData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.IO) {
                ru.ryabov.studentperformance.data.sync.SyncManager(
                    requireContext(),
                    (requireActivity().application as StudentPerformanceApp).database
                ).sync()
                requireActivity().runOnUiThread { binding.swipeRefresh.isRefreshing = false }
            }
        }
    }

    private fun setupViewModel() {
        val repository = (requireActivity().application as StudentPerformanceApp).database.let { database ->
            StudentRepository(
                database.userDao(), database.studentDao(), database.gradeDao(),
                database.subjectDao(), database.groupDao(), database.facultyDao(), database.gradeTypeDao()
            )
        }
        viewModel = ViewModelProvider(requireActivity(), GradeViewModel.Factory(repository))[GradeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ru.ryabov.studentperformance.ui.adapter.SubjectAdapter { subject ->
            startActivity(Intent(requireContext(), SubjectDetailActivity::class.java).apply {
                putExtra(SubjectDetailActivity.EXTRA_SUBJECT_ID, subject.subjectId)
            })
        }
        binding.rvSubjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSubjects.adapter = adapter
    }

    private fun observeData() {
        viewModel.subjects.observe(viewLifecycleOwner) { subjects ->
            adapter.submitList(subjects)
            binding.tvSubjectsCount.text = getString(ru.ryabov.studentperformance.R.string.subjects_count, subjects.size)
            binding.progressBar.visibility = if (subjects.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
