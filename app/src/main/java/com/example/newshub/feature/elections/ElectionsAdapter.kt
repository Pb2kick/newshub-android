package com.example.newshub.feature.elections

import android.graphics.Color
import androidx.core.content.ContextCompat
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

            val status = item.status.ifBlank { "Active" }
            val normalized = status.uppercase()
            binding.textElectionStatus.text = when {
                normalized.contains("UPCOMING") -> "Upcoming"
                normalized.contains("ACTIVE") || normalized.contains("OPEN") -> "Active"
                else -> status.replaceFirstChar { it.uppercase() }
            }
            applyStatusChipStyle(normalized)

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun applyStatusChipStyle(status: String) {
            val context = binding.root.context
            when {
                status.contains("UPCOMING") -> {
                    binding.textElectionStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                    binding.textElectionStatus.setTextColor(Color.parseColor("#92400E"))
                }
                status.contains("ACTIVE") || status.contains("OPEN") -> {
                    binding.textElectionStatus.setBackgroundResource(R.drawable.bg_status_active)
                    binding.textElectionStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.home_success_text)
                    )
                }
                else -> {
                    binding.textElectionStatus.setBackgroundResource(R.drawable.bg_home_chip_muted)
                    binding.textElectionStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.home_text_secondary)
                    )
                }
            }
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
