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
import ru.ryabov.studentperformance.data.remote.dto.GroupDto
import ru.ryabov.studentperformance.databinding.ActivityFacultiesListBinding
import ru.ryabov.studentperformance.ui.groups.GroupDetailActivity

class GroupsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultiesListBinding
    private lateinit var adapter: GroupListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultiesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.groups_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val role = SessionManager(applicationContext).userRole
        val canDelete = (role == "ADMIN")
        val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) loadGroups()
        }
        adapter = GroupListAdapter(
            onItemClick = { g ->
                formLauncher.launch(Intent(this, GroupFormActivity::class.java).putExtra(GroupFormActivity.EXTRA_ID, g.groupId))
            },
            onItemLongClick = if (canDelete) { { g -> deleteGroup(g) } } else null
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener { formLauncher.launch(Intent(this, GroupFormActivity::class.java)) }
        binding.emptyView.setOnClickListener { loadGroups() }
        loadGroups()
    }

    private fun loadGroups() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getGroups() }
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
                    Toast.makeText(this@GroupsListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyWithError(e.message ?: "Ошибка сети")
                Toast.makeText(this@GroupsListActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmptyWithError(reason: String) {
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(R.string.no_data) + "\n\n(нажмите для повтора)\n$reason"
    }

    private fun deleteGroup(g: GroupDto) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Удалить группу «${g.name}»?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                        val resp = withContext(Dispatchers.IO) { api.deleteGroup(g.groupId) }
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            Toast.makeText(this@GroupsListActivity, "Удалено", Toast.LENGTH_SHORT).show()
                            loadGroups()
                        } else Toast.makeText(this@GroupsListActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@GroupsListActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadGroups()
    }

    private class GroupListAdapter(
        private val onItemClick: (GroupDto) -> Unit,
        private val onItemLongClick: ((GroupDto) -> Unit)?
    ) : RecyclerView.Adapter<GroupListAdapter.VH>() {
        private var list = listOf<GroupDto>()
        fun submitList(newList: List<GroupDto>) { list = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_group_admin, parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.info.text = buildString {
                item.facultyName?.let { append("$it · ") }
                item.admissionYear?.let { append("Год $it") }
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
