package ru.ryabov.studentperformance.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.viewmodel.SubjectGradeSummary
import java.util.Locale

/**
 * Адаптер для списка «По дисциплинам»: дисциплина, средний балл, количество оценок.
 */
class SubjectGradeSummaryAdapter : ListAdapter<SubjectGradeSummary, SubjectGradeSummaryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject_grade_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubjectName: TextView = itemView.findViewById(R.id.tvSubjectName)
        private val tvAverage: TextView = itemView.findViewById(R.id.tvAverage)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCount)

        fun bind(item: SubjectGradeSummary) {
            tvSubjectName.text = item.subjectName
            tvAverage.text = String.format(Locale.getDefault(), "%.2f", item.average)
            tvCount.text = "оценок: ${item.count}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SubjectGradeSummary>() {
        override fun areItemsTheSame(a: SubjectGradeSummary, b: SubjectGradeSummary) = a.subjectId == b.subjectId
        override fun areContentsTheSame(a: SubjectGradeSummary, b: SubjectGradeSummary) = a == b
    }
}
