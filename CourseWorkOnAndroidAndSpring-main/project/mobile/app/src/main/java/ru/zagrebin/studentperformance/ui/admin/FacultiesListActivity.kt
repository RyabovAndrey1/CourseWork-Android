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
import ru.ryabov.studentperformance.data.remote.dto.FacultyDto
import ru.ryabov.studentperformance.databinding.ActivityFacultiesListBinding

class FacultiesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultiesListBinding
    private lateinit var adapter: FacultyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultiesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val role = ru.ryabov.studentperformance.data.remote.SessionManager(applicationContext).userRole
        val canDelete = (role == "ADMIN") // На сервере deleteFaculty только hasRole('ADMIN')
        val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) loadFaculties()
        }
        adapter = FacultyAdapter(
            onItemClick = { formLauncher.launch(Intent(this, FacultyFormActivity::class.java).putExtra(FacultyFormActivity.EXTRA_ID, it.facultyId)) },
            onItemLongClick = if (canDelete) { { f -> deleteFaculty(f) } } else null
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener { formLauncher.launch(Intent(this, FacultyFormActivity::class.java)) }
        binding.emptyView.setOnClickListener { loadFaculties() }
        loadFaculties()
    }

    private fun loadFaculties() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getFaculties() }
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
                    Toast.makeText(this@FacultiesListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyWithError(e.message ?: "Ошибка сети")
                Toast.makeText(this@FacultiesListActivity, e.message ?: "Ошибка сети", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmptyWithError(reason: String) {
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(R.string.no_data) + "\n\n(нажмите для повтора)\n$reason"
    }

    private fun deleteFaculty(f: FacultyDto) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Удалить факультет «${f.name}»?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                        val resp = withContext(Dispatchers.IO) { api.deleteFaculty(f.facultyId) }
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            Toast.makeText(this@FacultiesListActivity, "Удалено", Toast.LENGTH_SHORT).show()
                            loadFaculties()
                        } else Toast.makeText(this@FacultiesListActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@FacultiesListActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadFaculties()
    }

    private class FacultyAdapter(
        private val onItemClick: (FacultyDto) -> Unit,
        private val onItemLongClick: ((FacultyDto) -> Unit)?
    ) : RecyclerView.Adapter<FacultyAdapter.VH>() {

        private var list = listOf<FacultyDto>()

        fun submitList(newList: List<FacultyDto>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faculty, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.deanName.text = item.deanName?.takeIf { it.isNotBlank() } ?: "—"
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.itemView.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                onItemLongClick != null
            }
        }

        override fun getItemCount() = list.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.name)
            val deanName: TextView = itemView.findViewById(R.id.deanName)
        }
    }
}
