package ru.ryabov.studentperformance.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.local.entity.SubjectEntity

/**
 * Адаптер для отображения списка дисциплин
 */
class SubjectAdapter(
    private val onSubjectClick: (SubjectEntity) -> Unit
) : ListAdapter<SubjectEntity, SubjectAdapter.SubjectViewHolder>(SubjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvSubjectName)
        private val tvCode: TextView = itemView.findViewById(R.id.tvSubjectCode)
        private val tvCredits: TextView = itemView.findViewById(R.id.tvCredits)
        private val tvControlType: TextView = itemView.findViewById(R.id.tvControlType)

        fun bind(subject: SubjectEntity) {
            tvName.text = subject.name
            tvCode.text = subject.code ?: ""
            tvCode.visibility = if (subject.code.isNullOrBlank()) View.GONE else View.VISIBLE
            tvCredits.text = "Кредиты: ${subject.credits}"
            tvControlType.text = getControlTypeText(subject.controlType)
        }

        private fun getControlTypeText(controlType: String?): String {
            return when (controlType) {
                "EXAM" -> "Экзамен"
                "CREDIT" -> "Зачет"
                "DIFF_CREDIT" -> "Диф. зачет"
                else -> ""
            }
        }

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSubjectClick(getItem(position))
                }
            }
        }
    }

    class SubjectDiffCallback : DiffUtil.ItemCallback<SubjectEntity>() {
        override fun areItemsTheSame(oldItem: SubjectEntity, newItem: SubjectEntity): Boolean {
            return oldItem.subjectId == newItem.subjectId
        }

        override fun areContentsTheSame(oldItem: SubjectEntity, newItem: SubjectEntity): Boolean {
            return oldItem == newItem
        }
    }
}