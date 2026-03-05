package ru.ryabov.studentperformance.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.databinding.FragmentProfileBinding

/**
 * Фрагмент «Профиль» в главном экране: переход в полный профиль или админ-панель, выход.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sessionManager = ru.ryabov.studentperformance.data.remote.SessionManager(requireContext().applicationContext)
        val role = sessionManager.userRole ?: ""
        val displayName = sessionManager.userFullName?.takeIf { it.isNotBlank() }
            ?: sessionManager.userId?.let { "Пользователь (ID: $it)" }
            ?: getString(R.string.profile_email_stub)
        val displayEmail = sessionManager.userEmail?.takeIf { it.isNotBlank() } ?: ""
        binding.tvProfileEmail.text = if (displayEmail.isNotBlank()) displayEmail else displayName
        binding.tvProfileRole.text = getString(R.string.profile_role, role.ifBlank { "—" })
        // Админ-панель: только для ADMIN и DEANERY (у TEACHER журнал и отчёты в нижнем меню)
        binding.btnAdminPanel.visibility = if (role == "ADMIN" || role == "DEANERY") View.VISIBLE else View.GONE
        // Журнал и отчёты в профиле — для ADMIN и DEANERY; TEACHER переходит по нижнему меню
        binding.btnJournal.visibility = if (role == "ADMIN" || role == "DEANERY") View.VISIBLE else View.GONE
        binding.btnReports.visibility = if (role == "ADMIN" || role == "DEANERY" || role == "STUDENT") View.VISIBLE else View.GONE

        binding.btnSync.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val result = ru.ryabov.studentperformance.data.sync.SyncManager(
                    requireContext(),
                    (requireActivity().application as ru.ryabov.studentperformance.StudentPerformanceApp).database
                ).sync()
                requireActivity().runOnUiThread {
                    android.widget.Toast.makeText(
                        requireContext(),
                        if (result.isSuccess) getString(R.string.refresh_success) else getString(R.string.refresh_error),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        binding.btnOpenProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
        binding.btnAdminPanel.setOnClickListener {
            (activity as? ru.ryabov.studentperformance.ui.MainActivity)?.openAdminPanel()
        }
        binding.btnJournal.setOnClickListener {
            startActivity(Intent(requireContext(), ru.ryabov.studentperformance.ui.admin.JournalActivity::class.java))
        }
        binding.btnReports.setOnClickListener {
            startActivity(Intent(requireContext(), ru.ryabov.studentperformance.ui.admin.ReportsActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            ru.ryabov.studentperformance.data.remote.SessionManager(requireContext().applicationContext).clearSession()
            requireActivity().finishAffinity()
            startActivity(Intent(requireContext(), ru.ryabov.studentperformance.ui.AuthActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
