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
import androidx.appcompat.app.AlertDialog
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
import ru.ryabov.studentperformance.data.remote.dto.UserDto
import ru.ryabov.studentperformance.databinding.ActivityFacultiesListBinding

class UsersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultiesListBinding
    private lateinit var adapter: UserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(applicationContext).userRole != "ADMIN") {
            finish()
            return
        }
        binding = ActivityFacultiesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.users_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.fab.visibility = View.VISIBLE
        val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) loadUsers()
        }
        binding.fab.setOnClickListener { formLauncher.launch(Intent(this, UserFormActivity::class.java)) }

        adapter = UserListAdapter(
            onItemClick = { user -> formLauncher.launch(Intent(this, UserFormActivity::class.java).putExtra(UserFormActivity.EXTRA_USER_ID, user.userId)) },
            onItemLongClick = { user -> showDeleteDialog(user) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.emptyView.setOnClickListener { loadUsers() }
        loadUsers()
    }

    private fun showDeleteDialog(user: UserDto) {
        AlertDialog.Builder(this)
            .setTitle("Удалить пользователя?")
            .setMessage("${user.login} (${user.lastName} ${user.firstName})")
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteUser(user.userId) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteUser(userId: Long) {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.deleteUser(userId) }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    Toast.makeText(this@UsersListActivity, "Удалено", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@UsersListActivity, resp.body()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UsersListActivity, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(SessionManager(applicationContext))
                val resp = withContext(Dispatchers.IO) { api.getUsers() }
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
                    Toast.makeText(this@UsersListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyWithError(e.message ?: "Ошибка сети")
                Toast.makeText(this@UsersListActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadUsers()
    }

    private fun showEmptyWithError(reason: String) {
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(R.string.no_data) + "\n\n(нажмите для повтора)\n$reason"
    }

    private class UserListAdapter(
        private val onItemClick: (UserDto) -> Unit,
        private val onItemLongClick: (UserDto) -> Unit
    ) : RecyclerView.Adapter<UserListAdapter.VH>() {
        private var list = listOf<UserDto>()
        fun submitList(newList: List<UserDto>) { list = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_department, parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = "${item.login ?: ""} · ${item.lastName ?: ""} ${item.firstName ?: ""}".trim()
            holder.info.text = item.role ?: "—"
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
