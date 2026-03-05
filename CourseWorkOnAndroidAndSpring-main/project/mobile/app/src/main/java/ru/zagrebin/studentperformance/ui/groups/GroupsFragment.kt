package ru.ryabov.studentperformance.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.FragmentGroupsBinding
import ru.ryabov.studentperformance.viewmodel.AdminViewModel

class GroupsFragment : Fragment() {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AdminViewModel
    private lateinit var adapter: ru.ryabov.studentperformance.ui.adapter.GroupAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
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
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
            StudentRepository(database.userDao(), database.studentDao(), database.gradeDao(),
                database.subjectDao(), database.groupDao(), database.facultyDao(), database.gradeTypeDao())
        }
        viewModel = ViewModelProvider(requireActivity(), AdminViewModel.Factory(repository))[AdminViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ru.ryabov.studentperformance.ui.adapter.GroupAdapter { group ->
            startActivity(android.content.Intent(requireContext(), GroupDetailActivity::class.java).apply {
                putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.groupId)
            })
        }
        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGroups.adapter = adapter
    }

    private fun observeData() {
        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            adapter.submitList(groups)
            binding.tvGroupsCount.text = getString(ru.ryabov.studentperformance.R.string.groups_count, groups.size)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
