package ru.ryabov.studentperformance.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.local.entity.GradeEntity
import ru.ryabov.studentperformance.viewmodel.GradeWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для отображения списка оценок
 */
class GradeAdapter(
    private val onGradeClick: (GradeWithDetails) -> Unit,
    private val onGradeLongClick: (GradeWithDetails) -> Boolean = { false }
) : ListAdapter<GradeWithDetails, GradeAdapter.GradeViewHolder>(GradeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubject: TextView = itemView.findViewById(R.id.tvSubjectName)
        private val tvGrade: TextView = itemView.findViewById(R.id.tvGradeValue)
        private val tvDate: TextView = itemView.findViewById(R.id.tvGradeDate)
        private val tvType: TextView = itemView.findViewById(R.id.tvGradeType)
        private val tvComment: TextView = itemView.findViewById(R.id.tvComment)

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(gradeWithDetails: GradeWithDetails) {
            val grade = gradeWithDetails.grade

            tvSubject.text = gradeWithDetails.subjectName
            tvType.text = grade.workType ?: "Оценка"
            tvDate.text = dateFormat.format(Date(grade.gradeDate))

            grade.gradeValue?.let { value ->
                tvGrade.text = String.format(Locale.getDefault(), "%.1f", value)
                tvGrade.setTextColor(getGradeColor(value))
            } ?: run {
                tvGrade.text = "-"
                tvGrade.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
            }

            tvComment.text = grade.comment ?: ""
            tvComment.visibility = if (grade.comment.isNullOrBlank()) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onGradeClick(gradeWithDetails) }
            itemView.setOnLongClickListener { onGradeLongClick(gradeWithDetails) }
        }

        private fun getGradeColor(grade: Double): Int {
            return when {
                grade >= 4.5 -> ContextCompat.getColor(itemView.context, R.color.grade_excellent)
                grade >= 3.5 -> ContextCompat.getColor(itemView.context, R.color.grade_good)
                grade >= 2.5 -> ContextCompat.getColor(itemView.context, R.color.grade_satisfactory)
                else -> ContextCompat.getColor(itemView.context, R.color.grade_fail)
            }
        }
    }

    class GradeDiffCallback : DiffUtil.ItemCallback<GradeWithDetails>() {
        override fun areItemsTheSame(oldItem: GradeWithDetails, newItem: GradeWithDetails): Boolean {
            return oldItem.grade.gradeId == newItem.grade.gradeId
        }

        override fun areContentsTheSame(oldItem: GradeWithDetails, newItem: GradeWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}