package ru.ryabov.studentperformance.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.local.entity.StudentEntity

/**
 * Адаптер для отображения списка студентов
 */
class StudentAdapter(
    private val onStudentClick: (StudentEntity) -> Unit,
    var groupName: String? = null
) : ListAdapter<StudentEntity, StudentAdapter.StudentViewHolder>(StudentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvStudentName)
        private val tvGroup: TextView = itemView.findViewById(R.id.tvStudentGroup)
        private val tvRecordBook: TextView = itemView.findViewById(R.id.tvRecordBookNumber)

        fun bind(student: StudentEntity) {
            tvName.text = "Студент #${student.studentId}" // ФИО — при наличии User по userId
            tvGroup.text = groupName?.let { "Группа: $it" } ?: "Группа: ${student.groupId ?: "—"}"
            tvRecordBook.text = student.recordBookNumber ?: ""
            tvRecordBook.visibility = if (student.recordBookNumber.isNullOrBlank()) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onStudentClick(student) }
        }
    }

    class StudentDiffCallback : DiffUtil.ItemCallback<StudentEntity>() {
        override fun areItemsTheSame(oldItem: StudentEntity, newItem: StudentEntity): Boolean {
            return oldItem.studentId == newItem.studentId
        }

        override fun areContentsTheSame(oldItem: StudentEntity, newItem: StudentEntity): Boolean {
            return oldItem == newItem
        }
    }
}