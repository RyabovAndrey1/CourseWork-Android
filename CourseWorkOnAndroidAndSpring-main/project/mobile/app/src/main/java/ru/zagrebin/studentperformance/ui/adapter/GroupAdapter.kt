package ru.ryabov.studentperformance.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ryabov.studentperformance.R
import ru.ryabov.studentperformance.data.local.entity.GroupEntity

/**
 * Адаптер для отображения списка учебных групп
 */
class GroupAdapter(
    private val onGroupClick: (GroupEntity) -> Unit
) : ListAdapter<GroupEntity, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvSpecialization: TextView = itemView.findViewById(R.id.tvSpecialization)
        private val tvYear: TextView = itemView.findViewById(R.id.tvAdmissionYear)

        fun bind(group: GroupEntity) {
            tvName.text = group.name
            tvSpecialization.text = group.specialization ?: ""
            tvSpecialization.visibility = if (group.specialization.isNullOrBlank()) View.GONE else View.VISIBLE
            tvYear.text = "Поступление: ${group.admissionYear ?: "N/A"}"

            itemView.setOnClickListener { onGroupClick(group) }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<GroupEntity>() {
        override fun areItemsTheSame(oldItem: GroupEntity, newItem: GroupEntity): Boolean {
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItem: GroupEntity, newItem: GroupEntity): Boolean {
            return oldItem == newItem
        }
    }
}