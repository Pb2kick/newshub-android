package com.example.newshub.feature.elections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.ElectionRecord
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
            binding.textElectionName.text = item.name
            binding.textElectionMeta.text = listOf(item.status, item.startDate, item.endDate)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            binding.textElectionDescription.text = item.description
            binding.root.setOnClickListener { onClick(item) }
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

