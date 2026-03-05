package ru.ryabov.studentperformance.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
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
import ru.ryabov.studentperformance.data.remote.dto.AssignedCourseItemDto
import ru.ryabov.studentperformance.data.remote.dto.GradeTypeDto
import ru.ryabov.studentperformance.data.remote.dto.GroupDto
import ru.ryabov.studentperformance.data.remote.dto.StudentDto
import ru.ryabov.studentperformance.data.remote.dto.SubjectDto
import ru.ryabov.studentperformance.databinding.ActivityJournalBinding
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Журнал: добавление записи или просмотр/редактирование выбранного занятия (из списка).
 * Для TEACHER: предметы и группы только из назначений (for-teacher, assigned-courses/me).
 * Для ADMIN при открытии с extras — только просмотр (read-only).
 */
class JournalActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LESSON_DATE = "lesson_date"
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_SUBJECT_ID = "subject_id"
        const val EXTRA_GRADE_TYPE_ID = "grade_type_id"
        const val EXTRA_READ_ONLY = "read_only"
    }

    private lateinit var binding: ActivityJournalBinding
    private val session get() = SessionManager(applicationContext)
    private val role get() = session.userRole
    private var isReadOnly = false

    private var subjects = listOf<SubjectDto>()
    private var gradeTypes = listOf<GradeTypeDto>()
    private var groups = listOf<GroupDto>()
    private var assignedCourses = listOf<AssignedCourseItemDto>()

    private var selectedLessonDate: LocalDate? = null
    private var selectedAssignment: AssignedCourseItemDto? = null
    private var students = mutableListOf<JournalStudentRow>()
    private lateinit var adapter: JournalStudentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (role != "TEACHER" && role != "ADMIN" && role != "DEANERY") {
            finish()
            return
        }
        binding = ActivityJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarJournal)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.journal_title)
        binding.toolbarJournal.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.lessonDate.setOnClickListener { showDatePicker() }
        binding.layoutLessonDate.setEndIconOnClickListener { showDatePicker() }

        // Спиннер «Тип занятия» — сразу ставим адаптер с плейсхолдером, чтобы выпадающий список открывался
        val gradeTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf("— Выберите тип занятия —"))
        gradeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGradeType.adapter = gradeTypeAdapter

        loadInitialData()
        binding.spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                onSubjectOrGroupChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                onSubjectOrGroupChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.btnLoadStudents.setOnClickListener { loadStudentsByGroup() }
        binding.btnSave.setOnClickListener { saveJournalEntry() }

        val lessonDate = intent.getStringExtra(EXTRA_LESSON_DATE)
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
        val gradeTypeId = intent.getLongExtra(EXTRA_GRADE_TYPE_ID, -1L)
        isReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)
        adapter = JournalStudentsAdapter(students, isReadOnly = isReadOnly)
        binding.recyclerView.adapter = adapter
        if (lessonDate != null && groupId > 0 && subjectId > 0 && gradeTypeId > 0) {
            selectedLessonDate = try { LocalDate.parse(lessonDate) } catch (_: Exception) { null }
            binding.lessonDate.setText(lessonDate)
            if (isReadOnly) {
                binding.btnSave.visibility = View.GONE
                binding.btnLoadStudents.isEnabled = false
            }
            loadInitialDataThenSelectLesson(groupId, subjectId, gradeTypeId)
        }
    }

    private fun loadInitialDataThenSelectLesson(groupId: Long, subjectId: Long, gradeTypeId: Long) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val isTeacher = role == "TEACHER"
                val subjResp = withContext(Dispatchers.IO) { if (isTeacher) api.getSubjectsForTeacher() else api.getSubjects() }
                val gtResp = withContext(Dispatchers.IO) { api.getGradeTypes() }
                val groupsResp = withContext(Dispatchers.IO) { api.getGroups() }
                val assignedResp = if (isTeacher) withContext(Dispatchers.IO) { api.getAssignedCoursesMe() } else null
                if (subjResp.isSuccessful && subjResp.body()?.success == true) {
                    subjects = subjResp.body()?.data ?: emptyList()
                    val names = listOf("— Выберите предмет —") + subjects.map { it.name }
                    binding.spinnerSubject.adapter = ArrayAdapter(this@JournalActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                gradeTypes = if (gtResp.isSuccessful && (gtResp.body()?.success == true)) {
                    (gtResp.body()?.data ?: emptyList()).distinctBy { it.typeId }.ifEmpty { defaultGradeTypes() }
                } else defaultGradeTypes()
                val gradeTypeNames = listOf("— Выберите тип занятия —") + gradeTypes.map { it.name ?: "" }
                val gtAdapter = ArrayAdapter(this@JournalActivity, android.R.layout.simple_spinner_item, gradeTypeNames)
                gtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGradeType.adapter = gtAdapter
                if (isTeacher && assignedResp != null && assignedResp.isSuccessful && assignedResp.body()?.success == true) {
                    assignedCourses = assignedResp.body()?.data ?: emptyList()
                    updateGroupSpinnerForSubject(subjectId)
                } else if (groupsResp.isSuccessful && groupsResp.body()?.success == true) {
                    groups = groupsResp.body()?.data ?: emptyList()
                    updateGroupSpinnerForSubject(subjectId)
                }
                binding.progressBar.visibility = View.GONE
                val subjIdx = subjects.indexOfFirst { it.subjectId == subjectId }.takeIf { it >= 0 }?.plus(1) ?: 0
                val typeIdx = gradeTypes.indexOfFirst { it.typeId == gradeTypeId }.takeIf { it >= 0 }?.plus(1) ?: 0
                val groupIdx = if (role == "TEACHER") {
                    val forSubj = assignedCourses.filter { it.subjectId == subjectId }
                    selectedAssignment = forSubj.find { it.groupId == groupId }
                    forSubj.indexOfFirst { it.groupId == groupId }.takeIf { it >= 0 }?.plus(1) ?: 0
                } else {
                    groups.indexOfFirst { it.groupId == groupId }.takeIf { it >= 0 }?.plus(1) ?: 0
                }
                binding.spinnerSubject.setSelection(subjIdx)
                binding.spinnerGradeType.setSelection(typeIdx)
                binding.spinnerGroup.setSelection(groupIdx)
                if (groupIdx > 0) loadStudentsByGroup()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@JournalActivity, e.message ?: "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance(Locale("ru"))
        selectedLessonDate?.let { date ->
            c.set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedLessonDate = LocalDate.of(year, month + 1, day)
                binding.lessonDate.setText(selectedLessonDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE))
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadInitialData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val isTeacher = role == "TEACHER"
                val subjResp = withContext(Dispatchers.IO) { if (isTeacher) api.getSubjectsForTeacher() else api.getSubjects() }
                val gtResp = withContext(Dispatchers.IO) { api.getGradeTypes() }
                val groupsResp = withContext(Dispatchers.IO) { api.getGroups() }
                val assignedResp = if (isTeacher) withContext(Dispatchers.IO) { api.getAssignedCoursesMe() } else null
                binding.progressBar.visibility = View.GONE

                if (subjResp.isSuccessful && subjResp.body()?.success == true) {
                    subjects = subjResp.body()?.data ?: emptyList()
                    val names = listOf("— Выберите предмет —") + subjects.map { it.name }
                    binding.spinnerSubject.adapter = ArrayAdapter(this@JournalActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                gradeTypes = if (gtResp.isSuccessful && (gtResp.body()?.success == true)) {
                    (gtResp.body()?.data ?: emptyList()).distinctBy { it.typeId }.ifEmpty { defaultGradeTypes() }
                } else {
                    if (!gtResp.isSuccessful) {
                        val errMsg = gtResp.errorBody()?.string()?.take(150) ?: "код ${gtResp.code()}"
                        Toast.makeText(this@JournalActivity, "Типы занятий с сервера: $errMsg. Используется список по умолчанию.", Toast.LENGTH_LONG).show()
                    }
                    defaultGradeTypes()
                }
                val gradeTypeNames = listOf("— Выберите тип занятия —") + gradeTypes.map { it.name ?: "" }
                val gtAdapter = ArrayAdapter(this@JournalActivity, android.R.layout.simple_spinner_item, gradeTypeNames)
                gtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGradeType.adapter = gtAdapter
                if (isTeacher && assignedResp != null && assignedResp.isSuccessful && assignedResp.body()?.success == true) {
                    assignedCourses = assignedResp.body()?.data ?: emptyList()
                    updateGroupSpinnerFromAssignments()
                } else if (groupsResp.isSuccessful && groupsResp.body()?.success == true) {
                    groups = groupsResp.body()?.data ?: emptyList()
                    val names = listOf("— Выберите группу —") + groups.map { it.name }
                    binding.spinnerGroup.adapter = ArrayAdapter(this@JournalActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@JournalActivity, e.message ?: "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Типы занятий по умолчанию (совпадают с сервером H2), если API не вернул данные. */
    private fun defaultGradeTypes(): List<GradeTypeDto> = listOf(
        GradeTypeDto(1, "Лекция", null, null, null, null),
        GradeTypeDto(2, "Практика", null, null, null, null),
        GradeTypeDto(3, "Лабораторная работа", null, null, null, null),
        GradeTypeDto(4, "Контрольная работа", null, null, null, null),
        GradeTypeDto(5, "Экзамен", null, null, null, null),
        GradeTypeDto(6, "Зачёт", null, null, null, null)
    )

    private fun onSubjectOrGroupChanged() {
        if (role != "TEACHER") return
        updateGroupSpinnerFromAssignments()
    }

    /** Обновляет список групп по выбранному предмету (из спиннера). */
    private fun updateGroupSpinnerFromAssignments() {
        val subjectPos = binding.spinnerSubject.selectedItemPosition
        val subjectId = if (subjectPos > 0 && subjectPos <= subjects.size) subjects[subjectPos - 1].subjectId else null
        updateGroupSpinnerForSubject(subjectId)
    }

    /** Заполняет спиннер групп по переданному subjectId (для TEACHER — из назначений, для ADMIN/DEANERY — все группы). */
    private fun updateGroupSpinnerForSubject(subjectId: Long?) {
        val names: List<String>
        if (role == "TEACHER") {
            val forSubject = if (subjectId != null) assignedCourses.filter { it.subjectId == subjectId } else emptyList()
            names = listOf("— Выберите группу —") + forSubject.map { it.groupName ?: "" }
        } else {
            names = listOf("— Выберите группу —") + groups.map { it.name }
        }
        binding.spinnerGroup.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
    }

    private fun loadStudentsByGroup() {
        val date = selectedLessonDate
        if (date == null) {
            Toast.makeText(this, "Выберите дату занятия", Toast.LENGTH_SHORT).show()
            return
        }
        val subjectPos = binding.spinnerSubject.selectedItemPosition
        val gradeTypePos = binding.spinnerGradeType.selectedItemPosition
        if (subjectPos <= 0 || gradeTypePos <= 0) {
            Toast.makeText(this, "Выберите предмет и тип занятия", Toast.LENGTH_SHORT).show()
            return
        }
        val groupId: Long?
        if (role == "TEACHER") {
            val assignPos = binding.spinnerGroup.selectedItemPosition
            val assignmentsForSubject = assignedCourses.filter { it.subjectId == subjects[subjectPos - 1].subjectId }
            if (assignPos <= 0 || assignPos > assignmentsForSubject.size) {
                Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
                return
            }
            selectedAssignment = assignmentsForSubject[assignPos - 1]
            groupId = selectedAssignment?.groupId
        } else {
            val groupPos = binding.spinnerGroup.selectedItemPosition
            if (groupPos <= 0 || groupPos > groups.size) {
                Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
                return
            }
            groupId = groups[groupPos - 1].groupId
            selectedAssignment = null
        }
        if (groupId == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) { api.getStudentsByGroup(groupId) }
                binding.progressBar.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val list = resp.body()?.data ?: emptyList()
                    val subjectPos = binding.spinnerSubject.selectedItemPosition
                    val subjectId = if (subjectPos > 0 && subjectPos <= subjects.size) subjects[subjectPos - 1].subjectId else null
                    val dateStr = selectedLessonDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val journalResp = if (subjectId != null) withContext(Dispatchers.IO) {
                        api.getJournalEntries(dateFrom = dateStr, dateTo = dateStr, groupId = groupId, subjectId = subjectId, searchStudent = null)
                    } else null
                    val journalEntries = if (journalResp?.isSuccessful == true && journalResp.body()?.success == true) {
                        journalResp.body()?.data ?: emptyList()
                    } else emptyList()
                    students.clear()
                    students.addAll(list.map { student ->
                        val name = student.fullName ?: ""
                        val gradeEntry = journalEntries.find { it.entryType == "Оценка" && it.studentFullName == name }
                        val attEntry = journalEntries.find { it.entryType == "Посещаемость" && it.studentFullName == name }
                        val score = gradeEntry?.valueDisplay?.takeIf { it.isNotBlank() } ?: ""
                        val present = attEntry?.valueDisplay?.lowercase() != "нет"
                        JournalStudentRow(student, score, present)
                    })
                    adapter.notifyDataSetChanged()
                    binding.studentsSectionTitle.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                    binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.emptyView.text = "Выберите дату, предмет, тип занятия и группу, затем нажмите «Загрузить список группы»"
                } else {
                    val errMsg = resp.body()?.message
                        ?: resp.errorBody()?.string()?.take(400)
                        ?: "Ошибка (код ${resp.code()}). Проверьте, что сервер запущен из ru.ryabov.studentperformance.ServerApplication."
                    binding.emptyView.visibility = View.VISIBLE
                    binding.emptyView.text = errMsg
                    Toast.makeText(this@JournalActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                val errMsg = e.message ?: "Ошибка сети"
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = errMsg
                Toast.makeText(this@JournalActivity, errMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveJournalEntry() {
        val date = selectedLessonDate
        if (date == null) {
            Toast.makeText(this, "Выберите дату занятия", Toast.LENGTH_SHORT).show()
            return
        }
        val subjectPos = binding.spinnerSubject.selectedItemPosition
        val gradeTypePos = binding.spinnerGradeType.selectedItemPosition
        if (subjectPos <= 0 || subjectPos > subjects.size || gradeTypePos <= 0 || gradeTypePos > gradeTypes.size) {
            Toast.makeText(this, "Выберите предмет и тип занятия", Toast.LENGTH_SHORT).show()
            return
        }
        val subjectId = subjects[subjectPos - 1].subjectId
        val gradeTypeId = gradeTypes[gradeTypePos - 1].typeId
        val assignmentId = selectedAssignment?.assignmentId
        val semester = selectedAssignment?.semester ?: if (date.monthValue in 1..6) 1 else 2
        val academicYear = selectedAssignment?.academicYear ?: date.year
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (students.isEmpty()) {
            Toast.makeText(this, "Сначала загрузите список группы", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                var gradesOk = 0
                var attendanceOk = 0
                var firstError: String? = null
                for (row in students) {
                    if (firstError != null) break
                    val scoreVal = row.score.trim().toDoubleOrNull()
                    if (scoreVal != null) {
                        val req = ru.ryabov.studentperformance.data.remote.dto.CreateGradeRequest(
                            studentId = row.student.studentId,
                            subjectId = subjectId,
                            assignmentId = assignmentId,
                            gradeTypeId = gradeTypeId,
                            gradeValue = BigDecimal.valueOf(scoreVal),
                            gradeDate = dateStr,
                            semester = semester,
                            academicYear = academicYear,
                            comment = null,
                            workType = null
                        )
                        val r = withContext(Dispatchers.IO) { api.createGrade(req) }
                        when {
                            r.isSuccessful && r.body()?.success == true -> gradesOk++
                            else -> firstError = r.body()?.message
                                ?: r.errorBody()?.string()?.take(300)
                                ?: "Оценка: код ${r.code()}"
                        }
                    }
                    if (firstError != null) break
                    val markResp = withContext(Dispatchers.IO) {
                        api.markAttendance(
                            studentId = row.student.studentId,
                            subjectId = subjectId,
                            assignmentId = assignmentId,
                            lessonDate = dateStr,
                            present = row.present,
                            semester = semester,
                            academicYear = academicYear,
                            comment = null
                        )
                    }
                    when {
                        markResp.isSuccessful && markResp.body()?.success == true -> attendanceOk++
                        else -> firstError = markResp.body()?.message
                            ?: markResp.errorBody()?.string()?.take(300)
                            ?: "Посещаемость: код ${markResp.code()}"
                    }
                }
                binding.progressBar.visibility = View.GONE
                if (firstError != null) {
                    Toast.makeText(this@JournalActivity, firstError!!, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@JournalActivity, "Сохранено: оценок $gradesOk, посещаемость $attendanceOk", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@JournalActivity, e.message ?: "Ошибка сохранения", Toast.LENGTH_LONG).show()
            }
        }
    }

    private data class JournalStudentRow(val student: StudentDto, var score: String, var present: Boolean)

    private class JournalStudentsAdapter(private val rows: MutableList<JournalStudentRow>, private val isReadOnly: Boolean = false) : RecyclerView.Adapter<JournalStudentsAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_journal_student_row, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            holder.studentName.text = row.student.fullName ?: "Студент #${row.student.studentId}"
            holder.scoreInput.setText(row.score)
            holder.scoreInput.isEnabled = !isReadOnly
            holder.checkPresent.isChecked = row.present
            holder.checkPresent.isEnabled = !isReadOnly
            holder.scoreInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { row.score = s?.toString()?.trim() ?: "" }
                override fun afterTextChanged(s: Editable?) {}
            })
            holder.checkPresent.setOnCheckedChangeListener { _, checked -> row.present = checked }
        }

        override fun getItemCount() = rows.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val studentName: TextView = itemView.findViewById(R.id.studentName)
            val scoreInput: android.widget.EditText = itemView.findViewById(R.id.scoreInput)
            val checkPresent: CheckBox = itemView.findViewById(R.id.checkPresent)
        }
    }
}
