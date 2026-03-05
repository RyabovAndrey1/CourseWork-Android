package ru.ryabov.studentperformance.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import ru.ryabov.studentperformance.data.remote.dto.GradeTypeDto
import ru.ryabov.studentperformance.data.remote.dto.GroupDto
import ru.ryabov.studentperformance.data.remote.dto.LessonRecordDto
import ru.ryabov.studentperformance.data.remote.dto.SubjectDto
import ru.ryabov.studentperformance.databinding.ActivityJournalListBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Список занятий журнала с фильтрами (дата с/по, предмет, группа, тип занятия).
 * Для TEACHER/DEANERY — FAB «Добавить»; для ADMIN — только просмотр.
 * По нажатию на занятие открывается просмотр/редактирование (JournalActivity с параметрами).
 */
class JournalListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalListBinding
    private val session get() = SessionManager(applicationContext)
    private val role get() = session.userRole
    private val lessons = mutableListOf<LessonRecordDto>()
    private lateinit var adapter: LessonAdapter

    private var filterSubjects = listOf<SubjectDto>()
    private var filterGroups = listOf<GroupDto>()
    private var filterGradeTypes = listOf<GradeTypeDto>()

    private var filterDateFrom = LocalDate.now().minusMonths(3)
    private var filterDateTo = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (role != "TEACHER" && role != "ADMIN" && role != "DEANERY") {
            finish()
            return
        }
        binding = ActivityJournalListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.journal_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.fabAdd.visibility = if (role == "ADMIN") View.GONE else View.VISIBLE
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }

        adapter = LessonAdapter(lessons) { lesson ->
            val date = lesson.date ?: return@LessonAdapter
            val groupId = lesson.groupId ?: return@LessonAdapter
            val subjectId = lesson.subjectId ?: return@LessonAdapter
            val gradeTypeId = lesson.gradeTypeId ?: return@LessonAdapter
            val intent = Intent(this, JournalActivity::class.java).apply {
                putExtra(JournalActivity.EXTRA_LESSON_DATE, date)
                putExtra(JournalActivity.EXTRA_GROUP_ID, groupId)
                putExtra(JournalActivity.EXTRA_SUBJECT_ID, subjectId)
                putExtra(JournalActivity.EXTRA_GRADE_TYPE_ID, gradeTypeId)
                putExtra(JournalActivity.EXTRA_READ_ONLY, role == "ADMIN")
            }
            startActivity(intent)
        }
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.filterDateFrom.setText(filterDateFrom.format(DateTimeFormatter.ISO_LOCAL_DATE))
        binding.filterDateTo.setText(filterDateTo.format(DateTimeFormatter.ISO_LOCAL_DATE))
        binding.layoutDateFrom.setEndIconOnClickListener { showDatePicker(true) }
        binding.layoutDateTo.setEndIconOnClickListener { showDatePicker(false) }
        binding.filterDateFrom.setOnClickListener { showDatePicker(true) }
        binding.filterDateTo.setOnClickListener { showDatePicker(false) }
        binding.btnApplyFilters.setOnClickListener { loadLessons() }

        loadFilterOptionsThenLessons()
    }

    override fun onResume() {
        super.onResume()
        if (filterSubjects.isNotEmpty()) loadLessons()
    }

    private fun showDatePicker(isFrom: Boolean) {
        val initial = if (isFrom) filterDateFrom else filterDateTo
        val c = Calendar.getInstance(Locale("ru")).apply {
            set(initial.year, initial.monthValue - 1, initial.dayOfMonth)
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = LocalDate.of(year, month + 1, day)
                if (isFrom) {
                    filterDateFrom = date
                    binding.filterDateFrom.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    if (filterDateTo.isBefore(date)) {
                        filterDateTo = date
                        binding.filterDateTo.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                } else {
                    filterDateTo = date
                    binding.filterDateTo.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    if (filterDateFrom.isAfter(date)) {
                        filterDateFrom = date
                        binding.filterDateFrom.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadFilterOptionsThenLessons() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val isTeacher = role == "TEACHER"
                val subjResp = withContext(Dispatchers.IO) {
                    if (isTeacher) api.getSubjectsForTeacher() else api.getSubjects()
                }
                val groupsResp = withContext(Dispatchers.IO) { api.getGroups() }
                val gtResp = withContext(Dispatchers.IO) { api.getGradeTypes() }
                if (subjResp.isSuccessful && subjResp.body()?.success == true) {
                    filterSubjects = subjResp.body()?.data ?: emptyList()
                }
                if (groupsResp.isSuccessful && groupsResp.body()?.success == true) {
                    filterGroups = groupsResp.body()?.data ?: emptyList()
                }
                filterGradeTypes = if (gtResp.isSuccessful && gtResp.body()?.success == true) {
                    (gtResp.body()?.data ?: emptyList()).distinctBy { it.typeId }
                } else emptyList()
                val subjectNames = listOf("— Все —") + filterSubjects.map { it.name }
                val groupNames = listOf("— Все —") + filterGroups.map { it.name }
                val typeNames = listOf("— Все —") + filterGradeTypes.map { it.name ?: "" }
                binding.filterSubject.adapter = ArrayAdapter(
                    this@JournalListActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    subjectNames
                )
                binding.filterGroup.adapter = ArrayAdapter(
                    this@JournalListActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    groupNames
                )
                binding.filterGradeType.adapter = ArrayAdapter(
                    this@JournalListActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    typeNames
                )
                loadLessons()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@JournalListActivity, e.message ?: "Ошибка загрузки фильтров", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLessons() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        val dateFrom = binding.filterDateFrom.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: filterDateFrom.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = binding.filterDateTo.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: filterDateTo.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val subjectPos = binding.filterSubject.selectedItemPosition
        val groupPos = binding.filterGroup.selectedItemPosition
        val typePos = binding.filterGradeType.selectedItemPosition
        val subjectId = if (subjectPos > 0 && subjectPos <= filterSubjects.size) filterSubjects[subjectPos - 1].subjectId else null
        val groupId = if (groupPos > 0 && groupPos <= filterGroups.size) filterGroups[groupPos - 1].groupId else null
        val gradeTypeId = if (typePos > 0 && typePos <= filterGradeTypes.size) filterGradeTypes[typePos - 1].typeId else null

        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) {
                    api.getLessonRecords(
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        groupId = groupId,
                        subjectId = subjectId,
                        gradeTypeId = gradeTypeId,
                        page = 0,
                        size = 200
                    )
                }
                binding.progressBar.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val content = resp.body()?.data?.content.orEmpty()
                    lessons.clear()
                    lessons.addAll(content)
                    adapter.notifyDataSetChanged()
                    binding.emptyView.visibility = if (content.isEmpty()) View.VISIBLE else View.GONE
                    binding.emptyView.text = "Занятий за период нет. Измените фильтры или нажмите «+», чтобы добавить запись."
                } else {
                    binding.emptyView.visibility = View.VISIBLE
                    val errMsg = resp.body()?.message
                        ?: resp.errorBody()?.string()?.take(400)
                        ?: "Ошибка загрузки (код ${resp.code()})."
                    binding.emptyView.text = errMsg
                    Toast.makeText(this@JournalListActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
                val msg = e.message ?: "Ошибка сети."
                binding.emptyView.text = msg
                Toast.makeText(this@JournalListActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private class LessonAdapter(
        private val items: List<LessonRecordDto>,
        private val onItemClick: (LessonRecordDto) -> Unit
    ) : RecyclerView.Adapter<LessonAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson_record, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val lesson = items[position]
            holder.tvDate.text = lesson.date ?: ""
            holder.tvSubjectGroup.text = "${lesson.subjectName ?: ""} · ${lesson.groupName ?: ""}"
            holder.tvType.text = lesson.gradeTypeName ?: ""
            holder.itemView.setOnClickListener { onItemClick(lesson) }
        }

        override fun getItemCount() = items.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvSubjectGroup: TextView = itemView.findViewById(R.id.tvSubjectGroup)
            val tvType: TextView = itemView.findViewById(R.id.tvType)
        }
    }
}
