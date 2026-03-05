package ru.ryabov.studentperformance.ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.remote.dto.TeacherDto
import ru.ryabov.studentperformance.databinding.ActivityFacultiesListBinding

class TeachersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultiesListBinding
    private lateinit var adapter: TeacherListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultiesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.teachers_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) loadTeachers()
        }
        adapter = TeacherListAdapter(
            onItemClick = { t -> formLauncher.launch(Intent(this, TeacherFormActivity::class.java).putExtra(TeacherFormActivity.EXTRA_ID, t.teacherId)) },
            onItemLongClick = { t -> deleteTeacher(t) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener { formLauncher.launch(Intent(this, TeacherFormActivity::class.java)) }
        binding.emptyView.setOnClickListener { loadTeachers() }
        loadTeachers()
    }

    private fun loadTeachers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getTeachers() }
                binding.progressBar.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val list = resp.body()?.data ?: emptyList()
                    adapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.emptyView.text = getString(R.string.no_data)
                    } else {
                        binding.emptyView.visibility = View.GONE
                    }
                } else {
                    val errMsg = resp.body()?.message ?: resp.errorBody()?.string()?.take(200) ?: "Код ${resp.code()}"
                    showEmptyWithError(errMsg)
                    Toast.makeText(this@TeachersListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyWithError(e.message ?: "Ошибка сети")
                Toast.makeText(this@TeachersListActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmptyWithError(reason: String) {
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(R.string.no_data) + "\n\n(нажмите для повтора)\n$reason"
    }

    private fun deleteTeacher(t: TeacherDto) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Удалить запись преподавателя «${t.fullName ?: t.login}»?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                        val resp = withContext(Dispatchers.IO) { api.deleteTeacher(t.teacherId) }
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            Toast.makeText(this@TeachersListActivity, "Удалено", Toast.LENGTH_SHORT).show()
                            loadTeachers()
                        } else Toast.makeText(this@TeachersListActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@TeachersListActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadTeachers()
    }

    private class TeacherListAdapter(
        private val onItemClick: (TeacherDto) -> Unit,
        private val onItemLongClick: (TeacherDto) -> Unit
    ) : RecyclerView.Adapter<TeacherListAdapter.VH>() {
        private var list = listOf<TeacherDto>()
        fun submitList(newList: List<TeacherDto>) { list = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_department, parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.fullName ?: item.login ?: "—"
            holder.info.text = item.departmentName ?: item.position ?: "—"
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.itemView.setOnLongClickListener { onItemLongClick(item); true }
        }
        override fun getItemCount() = list.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.name)
            val info: TextView = itemView.findViewById(R.id.facultyName)
        }
    }
}
