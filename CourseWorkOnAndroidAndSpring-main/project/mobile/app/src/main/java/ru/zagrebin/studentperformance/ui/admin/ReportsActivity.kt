package ru.ryabov.studentperformance.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.BuildConfig
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.remote.dto.GroupDto
import ru.ryabov.studentperformance.data.remote.dto.ReportRecordDto
import ru.ryabov.studentperformance.data.remote.dto.StudentDto
import ru.ryabov.studentperformance.data.remote.dto.SubjectDto
import ru.ryabov.studentperformance.databinding.ActivityReportsBinding
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Отчёты: тип отчёта → период (2 даты, DatePicker) → группа + предмет (у преподавателя только свои предметы) → PDF/Excel.
 * Для студента — только «Мой отчёт».
 */
class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val session get() = SessionManager(applicationContext)
    private val role get() = session.userRole

    private var groups = listOf<GroupDto>()
    private var subjects = listOf<SubjectDto>()
    private var students = listOf<StudentDto>()
    /** Студенты выбранной группы (для типа «По студенту»). */
    private var studentsInSelectedGroup = listOf<StudentDto>()
    private var dateFrom: LocalDate? = null
    private var dateTo: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarReports)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.reports_title)
        binding.toolbarReports.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (role == "STUDENT") {
            setupStudentOnly()
        } else {
            setupReportTypeAndFilters()
        }
    }

    private fun setupStudentOnly() {
        binding.spinnerReportType.visibility = View.GONE
        // Для студента показываем выбор периода отчёта (дата с — дата по)
        binding.layoutDateFrom.visibility = View.VISIBLE
        binding.layoutDateTo.visibility = View.VISIBLE
        binding.dateFrom.setOnClickListener { showDatePicker(true) }
        binding.layoutDateFrom.setEndIconOnClickListener { showDatePicker(true) }
        binding.dateTo.setOnClickListener { showDatePicker(false) }
        binding.layoutDateTo.setEndIconOnClickListener { showDatePicker(false) }
        val today = LocalDate.now()
        val startOfYear = if (today.monthValue <= 6) LocalDate.of(today.year, 1, 1) else LocalDate.of(today.year, 9, 1)
        dateFrom = startOfYear
        dateTo = today
        binding.dateFrom.setText(startOfYear.format(DateTimeFormatter.ISO_LOCAL_DATE))
        binding.dateTo.setText(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
        binding.labelGroup.visibility = View.GONE
        binding.spinnerGroup.visibility = View.GONE
        binding.labelSubject.visibility = View.GONE
        binding.spinnerSubject.visibility = View.GONE
        binding.sectionStudentsTitle.visibility = View.GONE
        binding.spinnerStudent.visibility = View.GONE
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) { api.getCurrentStudent() }
                binding.progressBar.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    val me = resp.body()!!.data!!
                    students = listOf(me)
                    binding.sectionStudentsTitle.visibility = View.VISIBLE
                    binding.sectionStudentsTitle.text = "Мой отчёт"
                    binding.spinnerStudent.visibility = View.VISIBLE
                    binding.spinnerStudent.adapter = ArrayAdapter(
                        this@ReportsActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf(me.fullName ?: "Мой отчёт")
                    )
                } else {
                    binding.emptyView.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
        }
        binding.btnDownloadExcel.setOnClickListener { downloadStudentReport(excel = true) }
        binding.btnDownloadPdf.setOnClickListener { downloadStudentReport(excel = false) }
        loadReportRecords()
    }

    private fun setupReportTypeAndFilters() {
        val reportTypes = listOf("По предмету", "По группе", "По студенту")
        binding.spinnerReportType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reportTypes)

        val today = LocalDate.now()
        val startOfYear = if (today.monthValue <= 6) LocalDate.of(today.year, 1, 1) else LocalDate.of(today.year, 9, 1)
        dateFrom = startOfYear
        dateTo = today
        binding.dateFrom.setText(startOfYear.format(DateTimeFormatter.ISO_LOCAL_DATE))
        binding.dateTo.setText(today.format(DateTimeFormatter.ISO_LOCAL_DATE))

        binding.dateFrom.setOnClickListener { showDatePicker(true) }
        binding.layoutDateFrom.setEndIconOnClickListener { showDatePicker(true) }
        binding.dateTo.setOnClickListener { showDatePicker(false) }
        binding.layoutDateTo.setEndIconOnClickListener { showDatePicker(false) }

        binding.spinnerReportType.setSelection(0)
        updateVisibilityByReportType()

        binding.spinnerReportType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                updateVisibilityByReportType()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.spinnerGroup.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (binding.spinnerReportType.selectedItemPosition == 2 && pos > 0 && pos <= groups.size) {
                    loadStudentsByGroup(groups[pos - 1].groupId)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        loadGroupsAndSubjectsAndStudents()
        binding.btnDownloadExcel.setOnClickListener { downloadReport(excel = true) }
        binding.btnDownloadPdf.setOnClickListener { downloadReport(excel = false) }
        loadReportRecords()
    }

    private fun updateVisibilityByReportType() {
        val typePos = binding.spinnerReportType.selectedItemPosition
        val isSubject = typePos == 0
        val isGroup = typePos == 1
        val isStudent = typePos == 2
        // По предмету: дисциплина + даты
        binding.labelSubject.visibility = if (isSubject) View.VISIBLE else View.GONE
        binding.spinnerSubject.visibility = if (isSubject) View.VISIBLE else View.GONE
        // Датапикеры показываем для всех типов (для «по группе» период не отправляется на сервер, но интерфейс не сбрасывается)
        binding.layoutDateFrom.visibility = View.VISIBLE
        binding.layoutDateTo.visibility = View.VISIBLE
        // По группе: только группа
        binding.labelGroup.visibility = if (isGroup || isStudent) View.VISIBLE else View.GONE
        binding.spinnerGroup.visibility = if (isGroup || isStudent) View.VISIBLE else View.GONE
        // По студенту: группа уже показана, плюс выбор студента из группы
        binding.sectionStudentsTitle.visibility = if (isStudent) View.VISIBLE else View.GONE
        binding.spinnerStudent.visibility = if (isStudent) View.VISIBLE else View.GONE
        if (isStudent) {
            binding.sectionStudentsTitle.text = "Студент (из выбранной группы)"
            if (binding.spinnerGroup.selectedItemPosition > 0 && binding.spinnerGroup.selectedItemPosition <= groups.size) {
                loadStudentsByGroup(groups[binding.spinnerGroup.selectedItemPosition - 1].groupId)
            } else {
                studentsInSelectedGroup = emptyList()
                binding.spinnerStudent.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("— Сначала выберите группу —"))
            }
        }
    }

    private fun showDatePicker(isFrom: Boolean) {
        val c = Calendar.getInstance(Locale("ru"))
        val current = if (isFrom) dateFrom else dateTo
        current?.let { d -> c.set(d.year, d.monthValue - 1, d.dayOfMonth) }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val d = LocalDate.of(year, month + 1, day)
                if (isFrom) {
                    dateFrom = d
                    binding.dateFrom.setText(d.format(DateTimeFormatter.ISO_LOCAL_DATE))
                } else {
                    dateTo = d
                    binding.dateTo.setText(d.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadGroupsAndSubjectsAndStudents() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val isTeacher = role == "TEACHER"
                val gResp = withContext(Dispatchers.IO) { api.getGroups() }
                val sResp = withContext(Dispatchers.IO) { if (isTeacher) api.getSubjectsForTeacher() else api.getSubjects() }
                val stResp = withContext(Dispatchers.IO) { api.getStudents() }
                binding.progressBar.visibility = View.GONE

                if (gResp.isSuccessful && gResp.body()?.success == true) {
                    groups = gResp.body()?.data ?: emptyList()
                    val names = listOf("— Выберите группу —") + groups.map { it.name }
                    binding.spinnerGroup.adapter = ArrayAdapter(this@ReportsActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                if (sResp.isSuccessful && sResp.body()?.success == true) {
                    subjects = sResp.body()?.data ?: emptyList()
                    val names = listOf("— Выберите дисциплину —") + subjects.map { it.name }
                    binding.spinnerSubject.adapter = ArrayAdapter(this@ReportsActivity, android.R.layout.simple_spinner_dropdown_item, names)
                }
                if (stResp.isSuccessful && stResp.body()?.success == true) {
                    students = stResp.body()?.data ?: emptyList()
                    if (binding.spinnerReportType.selectedItemPosition != 2) {
                        val names = listOf("— Выберите студента —") + students.map { it.fullName ?: "Студент #${it.studentId}" }
                        binding.spinnerStudent.adapter = ArrayAdapter(this@ReportsActivity, android.R.layout.simple_spinner_dropdown_item, names)
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ReportsActivity, e.message ?: "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Загрузить студентов выбранной группы (для типа «По студенту»). */
    private fun loadStudentsByGroup(groupId: Long) {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) { api.getStudentsByGroup(groupId) }
                if (resp.isSuccessful && resp.body()?.success == true) {
                    studentsInSelectedGroup = resp.body()?.data ?: emptyList()
                    val names = listOf("— Выберите студента —") + studentsInSelectedGroup.map { it.fullName ?: "Студент #${it.studentId}" }
                    binding.spinnerStudent.adapter = ArrayAdapter(this@ReportsActivity, android.R.layout.simple_spinner_dropdown_item, names)
                } else {
                    studentsInSelectedGroup = emptyList()
                    binding.spinnerStudent.adapter = ArrayAdapter(this@ReportsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("— Нет студентов в группе —"))
                }
            } catch (_: Exception) {
                studentsInSelectedGroup = emptyList()
                binding.spinnerStudent.adapter = ArrayAdapter(this@ReportsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("— Ошибка загрузки —"))
            }
        }
    }

    private fun downloadReport(excel: Boolean) {
        val typePos = binding.spinnerReportType.selectedItemPosition
        when (typePos) {
            0 -> { // По предмету
                val subjPos = binding.spinnerSubject.selectedItemPosition
                if (subjPos <= 0 || subjPos > subjects.size) {
                    Toast.makeText(this, "Выберите дисциплину", Toast.LENGTH_SHORT).show()
                    return
                }
                sendReportToEmail(subjectId = subjects[subjPos - 1].subjectId, groupId = null, studentId = null, excel = excel)
            }
            1 -> { // По группе
                val groupPos = binding.spinnerGroup.selectedItemPosition
                if (groupPos <= 0 || groupPos > groups.size) {
                    Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
                    return
                }
                sendReportToEmail(subjectId = null, groupId = groups[groupPos - 1].groupId, studentId = null, excel = excel)
            }
            2 -> { // По студенту
                val groupPos = binding.spinnerGroup.selectedItemPosition
                if (groupPos <= 0 || groupPos > groups.size) {
                    Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
                    return
                }
                val studentPos = binding.spinnerStudent.selectedItemPosition
                if (studentPos <= 0 || studentPos > studentsInSelectedGroup.size) {
                    Toast.makeText(this, "Выберите студента из группы", Toast.LENGTH_SHORT).show()
                    return
                }
                val from = dateFrom?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val to = dateTo?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                sendReportToEmail(subjectId = null, groupId = null, studentId = studentsInSelectedGroup[studentPos - 1].studentId, excel = excel, dateFrom = from, dateTo = to)
            }
        }
    }

    private fun downloadStudentReport(excel: Boolean) {
        if (students.isEmpty()) {
            Toast.makeText(this, "Нет данных студента", Toast.LENGTH_SHORT).show()
            return
        }
        val from = dateFrom?.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val to = dateTo?.format(DateTimeFormatter.ISO_LOCAL_DATE)
        sendReportToEmail(subjectId = null, groupId = null, studentId = students[0].studentId, excel = excel, dateFrom = from, dateTo = to)
    }

    /**
     * Отправить отчёт на почту текущего пользователя.
     * Ровно один из subjectId, groupId, studentId задан.
     */
    private fun sendReportToEmail(
        subjectId: Long?,
        groupId: Long?,
        studentId: Long?,
        excel: Boolean,
        dateFrom: String? = null,
        dateTo: String? = null
    ) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) {
                    when {
                        subjectId != null -> if (excel) api.sendSubjectReportExcelToEmail(subjectId) else api.sendSubjectReportPdfToEmail(subjectId)
                        groupId != null -> if (excel) api.sendGroupReportExcelToEmail(groupId) else api.sendGroupReportPdfToEmail(groupId)
                        studentId != null -> if (excel) api.sendStudentReportExcelToEmail(studentId, dateFrom, dateTo) else api.sendStudentReportPdfToEmail(studentId, dateFrom, dateTo)
                        else -> throw IllegalArgumentException("Укажите предмет, группу или студента")
                    }
                }
                binding.progressBar.visibility = View.GONE
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val msg = resp.body()?.message
                    val email = SessionManager(applicationContext).userEmail?.takeIf { it.isNotBlank() }
                    val toastMsg = when {
                        !msg.isNullOrBlank() -> msg
                        !email.isNullOrBlank() -> getString(R.string.report_sent_to_email_with_address, email)
                        else -> getString(R.string.report_sent_to_email)
                    }
                    Toast.makeText(this@ReportsActivity, toastMsg, Toast.LENGTH_LONG).show()
                } else {
                    val msg = resp.body()?.message
                        ?: resp.errorBody()?.string()?.take(300)
                        ?: "Ошибка загрузки (код ${resp.code()}). Проверьте подключение к серверу и логи сервера."
                    Toast.makeText(this@ReportsActivity, msg, Toast.LENGTH_LONG).show()
                    if (msg.contains("не удалось отправить", ignoreCase = true) || msg.contains("не настроена", ignoreCase = true)) {
                        downloadReportToDevice(subjectId, groupId, studentId, excel, dateFrom, dateTo)
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ReportsActivity, e.message ?: "Ошибка сети", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Скачать отчёт на устройство (если отправка на почту не удалась). */
    private fun downloadReportToDevice(
        subjectId: Long?,
        groupId: Long?,
        studentId: Long?,
        excel: Boolean,
        dateFrom: String? = null,
        dateTo: String? = null
    ) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) {
                    when {
                        subjectId != null -> if (excel) api.downloadSubjectReportExcel(subjectId) else api.downloadSubjectReportPdf(subjectId)
                        groupId != null -> if (excel) api.downloadGroupReportExcel(groupId) else api.downloadGroupReportPdf(groupId)
                        studentId != null -> if (excel) api.downloadStudentReportExcel(studentId, dateFrom, dateTo) else api.downloadStudentReportPdf(studentId, dateFrom, dateTo)
                        else -> throw IllegalArgumentException("Укажите предмет, группу или студента")
                    }
                }
                binding.progressBar.visibility = View.GONE
                if (resp.isSuccessful && resp.body() != null) {
                    val ext = if (excel) "xlsx" else "pdf"
                    val prefix = when {
                        subjectId != null -> "report-subject-$subjectId"
                        groupId != null -> "report-group-$groupId"
                        else -> "report-student-$studentId"
                    }
                    val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: filesDir
                    val file = File(dir, "$prefix.$ext")
                    resp.body()!!.byteStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    openFile(file, ext)
                    Toast.makeText(this@ReportsActivity, "Отчёт сохранён: ${file.name}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ReportsActivity, "Не удалось скачать отчёт (код ${resp.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ReportsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReportRecords() {
        lifecycleScope.launch {
            try {
                val api = RetrofitProvider.createApi(session)
                val resp = withContext(Dispatchers.IO) { api.getReportRecords() }
                if (!resp.isSuccessful || resp.body()?.success != true) return@launch
                val list = resp.body()?.data ?: emptyList()
                val container = binding.reportRecordsContainer
                container.removeAllViews()
                for (r in list) {
                    val desc = when (r.reportType) {
                        "SUBJECT" -> "По дисциплине ${r.subjectId}, ${r.format ?: "?"}, ${r.createdAt?.take(10) ?: ""}"
                        "GROUP" -> "По группе ${r.groupId}, ${r.format ?: "?"}, ${r.createdAt?.take(10) ?: ""}"
                        "STUDENT" -> "По студенту ${r.studentId}, ${r.format ?: "?"}, ${r.createdAt?.take(10) ?: ""}"
                        else -> "Отчёт ${r.reportType}, ${r.format ?: "?"}, ${r.createdAt?.take(10) ?: ""}"
                    }
                    val row = LinearLayout(this@ReportsActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 8, 0, 8)
                    }
                    val tv = TextView(this@ReportsActivity).apply {
                        text = desc
                        setPadding(0, 0, 24, 0)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val btn = android.widget.Button(this@ReportsActivity).apply {
                        text = "Скачать"
                        setOnClickListener { downloadRecord(r) }
                    }
                    row.addView(tv)
                    row.addView(btn)
                    container.addView(row)
                }
            } catch (_: Exception) { /* скрытый список при ошибке */ }
        }
    }

    private fun downloadRecord(r: ReportRecordDto) {
        val excel = r.format.equals("EXCEL", ignoreCase = true)
        val from = r.periodFrom?.toString()
        val to = r.periodTo?.toString()
        when {
            r.reportType == "SUBJECT" && r.subjectId != null -> downloadReportToDevice(r.subjectId, null, null, excel)
            r.reportType == "GROUP" && r.groupId != null -> downloadReportToDevice(null, r.groupId, null, excel)
            r.reportType == "STUDENT" && r.studentId != null -> downloadReportToDevice(null, null, r.studentId, excel, from, to)
            else -> Toast.makeText(this, "Не удалось скачать этот отчёт", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: File, ext: String) {
        val mime = when (ext) {
            "pdf" -> "application/pdf"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "*/*"
        }
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            Toast.makeText(this, "Файл сохранён", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Файл: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}
