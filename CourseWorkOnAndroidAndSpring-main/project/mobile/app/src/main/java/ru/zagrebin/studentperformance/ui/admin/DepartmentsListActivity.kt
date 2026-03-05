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
import ru.ryabov.studentperformance.data.remote.dto.DepartmentDto
import ru.ryabov.studentperformance.databinding.ActivityDeptListBinding

class DepartmentsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeptListBinding
    private lateinit var adapter: DeptAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeptListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) loadDepartments()
        }
        adapter = DeptAdapter(
            onItemClick = { formLauncher.launch(Intent(this, DepartmentFormActivity::class.java).putExtra(DepartmentFormActivity.EXTRA_ID, it.departmentId)) },
            onItemLongClick = { deleteDepartment(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener { formLauncher.launch(Intent(this, DepartmentFormActivity::class.java)) }
        binding.emptyView.setOnClickListener { loadDepartments() }
        loadDepartments()
    }

    private fun loadDepartments() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getDepartments(null) }
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
                    Toast.makeText(this@DepartmentsListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyWithError(e.message ?: "Ошибка сети")
                Toast.makeText(this@DepartmentsListActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmptyWithError(reason: String) {
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(R.string.no_data) + "\n\n(нажмите для повтора)\n$reason"
    }

    private fun deleteDepartment(d: DepartmentDto) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Удалить кафедру «${d.name}»?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                        val resp = withContext(Dispatchers.IO) { api.deleteDepartment(d.departmentId) }
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            Toast.makeText(this@DepartmentsListActivity, "Удалено", Toast.LENGTH_SHORT).show()
                            loadDepartments()
                        } else Toast.makeText(this@DepartmentsListActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@DepartmentsListActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadDepartments()
    }

    private class DeptAdapter(
        private val onItemClick: (DepartmentDto) -> Unit,
        private val onItemLongClick: (DepartmentDto) -> Unit
    ) : RecyclerView.Adapter<DeptAdapter.VH>() {
        private var list = listOf<DepartmentDto>()
        fun submitList(newList: List<DepartmentDto>) { list = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_department, parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.facultyName.text = item.facultyName ?: "—"
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.itemView.setOnLongClickListener { onItemLongClick(item); true }
        }
        override fun getItemCount() = list.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.name)
            val facultyName: TextView = itemView.findViewById(R.id.facultyName)
        }
    }
}
