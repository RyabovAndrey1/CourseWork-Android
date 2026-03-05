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
import ru.ryabov.studentperformance.data.remote.dto.StudentDto
import ru.ryabov.studentperformance.databinding.ActivityFacultiesListBinding
import ru.ryabov.studentperformance.ui.students.StudentDetailActivity

class StudentsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultiesListBinding
    private lateinit var adapter: StudentListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultiesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.students_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val role = SessionManager(applicationContext).userRole
        val canDelete = (role == "ADMIN")
        val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) loadStudents()
        }
        adapter = StudentListAdapter(
            onItemClick = { s ->
                formLauncher.launch(Intent(this, StudentFormActivity::class.java).putExtra(StudentFormActivity.EXTRA_ID, s.studentId))
            },
            onItemLongClick = if (canDelete) { { s -> deleteStudent(s) } } else null
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener { formLauncher.launch(Intent(this, StudentFormActivity::class.java)) }
        binding.emptyView.setOnClickListener { loadStudents() }
        loadStudents()
    }

    private fun loadStudents() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getStudents() }
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
                    val errMsg = resp.body()?.message
                        ?: resp.errorBody()?.string()?.take(200)
                        ?: "Код ${resp.code()}"
                    showEmptyWithError(errMsg)
                    Toast.makeText(this@StudentsListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                val errMsg = e.message ?: "Ошибка сети"
                showEmptyWithError(errMsg)
                Toast.makeText(this@StudentsListActivity, errMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmptyWithError(reason: String) {
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(R.string.no_data) + "\n\n(нажмите для повтора)\n$reason"
    }

    private fun deleteStudent(s: StudentDto) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Удалить студента «${s.fullName ?: "—"}»?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                        val resp = withContext(Dispatchers.IO) { api.deleteStudent(s.studentId) }
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            Toast.makeText(this@StudentsListActivity, "Удалено", Toast.LENGTH_SHORT).show()
                            loadStudents()
                        } else Toast.makeText(this@StudentsListActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@StudentsListActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadStudents()
    }

    private class StudentListAdapter(
        private val onItemClick: (StudentDto) -> Unit,
        private val onItemLongClick: ((StudentDto) -> Unit)?
    ) : RecyclerView.Adapter<StudentListAdapter.VH>() {
        private var list = listOf<StudentDto>()
        fun submitList(newList: List<StudentDto>) { list = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_student_admin, parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.fullName ?: "—"
            holder.info.text = buildString {
                item.groupName?.let { append("$it · ") }
                item.recordBookNumber?.let { append(it) }
            }.ifBlank { "—" }
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.itemView.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                onItemLongClick != null
            }
        }
        override fun getItemCount() = list.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.name)
            val info: TextView = itemView.findViewById(R.id.info)
        }
    }
}
