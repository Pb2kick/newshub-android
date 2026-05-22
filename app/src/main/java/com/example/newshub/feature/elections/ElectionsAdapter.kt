package com.example.newshub.feature.elections

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.newshub.ElectionRecord
import com.example.newshub.R
import com.example.newshub.databinding.ItemElectionBinding

class ElectionsAdapter(
    private val onClick: (ElectionRecord) -> Unit
) : ListAdapter<ElectionRecord, ElectionsAdapter.ElectionViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElectionViewHolder {
        val binding = ItemElectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ElectionViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ElectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ElectionViewHolder(
        private val binding: ItemElectionBinding,
        private val onClick: (ElectionRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ElectionRecord) {
            binding.imageElection.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_home_feature_placeholder)
                error(R.drawable.bg_home_feature_placeholder)
            }

            val regionLabel = item.region.ifBlank { "NATIONAL" }.uppercase()
            binding.textElectionRegion.text = regionLabel
            binding.textElectionName.text = item.name

            val dateLabel = listOf(item.startDate, item.endDate)
                .filter { it.isNotBlank() }
                .joinToString(" – ")
            binding.textElectionDate.text = dateLabel.ifBlank { item.startDate }

            val count = item.candidateCount
            binding.textCandidateCount.text = if (count > 0) {
                "$count Candidates"
            } else {
                binding.root.context.getString(R.string.view_candidates)
            }

            val status = item.status.ifBlank { "OPEN" }
            binding.textElectionStatus.text = status.uppercase()
            applyStatusChipColor(status)

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun applyStatusChipColor(status: String) {
            val normalized = status.uppercase()
            val color = when {
                normalized.contains("ACTIVE") || normalized.contains("OPEN") ->
                    Color.parseColor("#16A34A")
                normalized.contains("UPCOMING") ->
                    Color.parseColor("#D97706")
                else -> Color.parseColor("#64748B")
            }
            binding.textElectionStatus.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    private object Diff : DiffUtil.ItemCallback<ElectionRecord>() {
        override fun areItemsTheSame(oldItem: ElectionRecord, newItem: ElectionRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ElectionRecord, newItem: ElectionRecord): Boolean {
            return oldItem == newItem
        }
    }
}
