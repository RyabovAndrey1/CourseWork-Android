package ru.ryabov.studentperformance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.repository.StudentRepository
import ru.ryabov.studentperformance.databinding.FragmentSystemInfoBinding

/**
 * Экран «Информация о системе» для роли ADMIN (главный экран после входа).
 */
class SystemInfoFragment : Fragment() {

    private var _binding: FragmentSystemInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSystemInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()
    }

    private fun loadStats() {
        val db = (requireContext().applicationContext as StudentPerformanceApp).database
        val repo = StudentRepository(
            db.userDao(), db.studentDao(), db.gradeDao(),
            db.subjectDao(), db.groupDao(), db.facultyDao(), db.gradeTypeDao()
        )
        viewLifecycleOwner.lifecycleScope.launch {
            val (users, students, groups, faculties, subjects) = withContext(Dispatchers.IO) {
                Tuple5(
                    repo.getUserCount(),
                    repo.getStudentCount(),
                    repo.getGroupCount(),
                    repo.getFacultyCount(),
                    repo.getSubjectCount()
                )
            }
            binding.tvUsersCount.text = users.toString()
            binding.tvStudentsCount.text = students.toString()
            binding.tvGroupsCount.text = groups.toString()
            binding.tvFacultiesCount.text = faculties.toString()
            binding.tvSubjectsCount.text = subjects.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
