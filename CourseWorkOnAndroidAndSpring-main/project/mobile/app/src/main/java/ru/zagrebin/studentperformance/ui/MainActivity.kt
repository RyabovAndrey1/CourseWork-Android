package ru.ryabov.studentperformance.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.remote.dto.FcmTokenRequest
import ru.ryabov.studentperformance.data.sync.SyncManager
import ru.ryabov.studentperformance.databinding.ActivityMainBinding
import ru.ryabov.studentperformance.ui.admin.SystemInfoFragment
import ru.ryabov.studentperformance.ui.groups.GroupsFragment
import ru.ryabov.studentperformance.ui.profile.ProfileFragment
import ru.ryabov.studentperformance.ui.subjects.SubjectsFragment
import ru.ryabov.studentperformance.ui.grades.GradesListFragment

/**
 * Главная активность приложения «Контроль успеваемости».
 * Отображает нижнюю навигацию и переключает фрагменты: Группы, Дисциплины, Оценки, Профиль.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = SyncManager(this@MainActivity, (application as ru.ryabov.studentperformance.StudentPerformanceApp).database).sync()
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            if (result.isSuccess) getString(R.string.refresh_success) else getString(R.string.refresh_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return true
            }
            R.id.action_logout -> {
                SessionManager(applicationContext).clearSession()
                finishAffinity()
                startActivity(Intent(this, AuthActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /** Отправка FCM-токена на сервер для пуш-уведомлений (после входа). */
    private fun registerFcmTokenIfLoggedIn(sessionManager: SessionManager) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result.isNullOrBlank()) return@addOnCompleteListener
            val token = task.result
            lifecycleScope.launch {
                try {
                    val api = RetrofitProvider.createApi(sessionManager)
                    val resp = withContext(Dispatchers.IO) { api.registerFcmToken(FcmTokenRequest(token)) }
                    if (!resp.isSuccessful) { /* сервер может не иметь FCM — игнорируем */ }
                } catch (_: Exception) { }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = ru.ryabov.studentperformance.data.remote.SessionManager(applicationContext)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        registerFcmTokenIfLoggedIn(sessionManager)

        applyRoleBasedMenu(sessionManager.userRole)

        if (savedInstanceState == null) {
            val startFragment = when (sessionManager.userRole) {
                "STUDENT" -> GradesListFragment()
                "ADMIN" -> SystemInfoFragment()
                "TEACHER" -> ProfileFragment()
                else -> GroupsFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentFrame, startFragment)
                .commit()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            SyncManager(this@MainActivity, (application as ru.ryabov.studentperformance.StudentPerformanceApp).database).sync()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_more) {
                showMoreOptions(sessionManager.userRole)
                return@setOnItemSelectedListener true
            }
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_groups -> GroupsFragment()
                R.id.nav_subjects -> SubjectsFragment()
                R.id.nav_grades -> GradesListFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentFrame, fragment)
                .commit()
            true
        }
    }

    /**
     * По ролям (не более 5 пунктов в BottomNavigationView):
     * ADMIN — «Ещё», «Профиль».
     * DEANERY — Группы, Дисциплины, Оценки, Профиль.
     * TEACHER — «Ещё», Профиль (Журнал и Отчёты внутри «Ещё»).
     * STUDENT — Оценки, Профиль.
     */
    private fun applyRoleBasedMenu(role: String?) {
        val menu = binding.bottomNavigation.menu
        menu.findItem(R.id.nav_more)?.isVisible = (role == "ADMIN" || role == "TEACHER")
        menu.findItem(R.id.nav_groups)?.isVisible = (role == "DEANERY")
        menu.findItem(R.id.nav_subjects)?.isVisible = (role == "DEANERY")
        menu.findItem(R.id.nav_grades)?.isVisible = (role != "ADMIN" && role != "TEACHER")
        menu.findItem(R.id.nav_profile)?.isVisible = true
    }

    /** Диалог «Ещё»: для ADMIN — О системе, Админ-панель; для TEACHER — Журнал, Отчёты. */
    private fun showMoreOptions(role: String?) {
        val options = when (role) {
            "ADMIN" -> arrayOf(
                getString(R.string.nav_system_info_title),
                getString(R.string.admin_panel_button)
            )
            "TEACHER" -> arrayOf(
                getString(R.string.journal_title),
                getString(R.string.reports_title)
            )
            else -> return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.nav_more))
            .setItems(options) { _, which ->
                when (role) {
                    "ADMIN" -> when (which) {
                        0 -> supportFragmentManager.beginTransaction()
                            .replace(R.id.contentFrame, SystemInfoFragment())
                            .commit()
                        1 -> startActivity(Intent(this, ru.ryabov.studentperformance.ui.admin.AdminPanelActivity::class.java))
                    }
                    "TEACHER" -> when (which) {
                        0 -> startActivity(Intent(this, ru.ryabov.studentperformance.ui.admin.JournalListActivity::class.java))
                        1 -> startActivity(Intent(this, ru.ryabov.studentperformance.ui.admin.ReportsActivity::class.java))
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Переход в админ-панель (для пользователей с ролью ADMIN и DEANERY).
     */
    fun openAdminPanel() {
        startActivity(Intent(this, ru.ryabov.studentperformance.ui.admin.AdminPanelActivity::class.java))
    }
}
