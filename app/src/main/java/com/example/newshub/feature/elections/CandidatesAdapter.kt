package com.example.newshub.feature.elections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.CandidateRecord
import com.example.newshub.databinding.ItemCandidateBinding

class CandidatesAdapter(
    private val onVoteClicked: (CandidateRecord) -> Unit
) : ListAdapter<CandidateRecord, CandidatesAdapter.CandidateViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        val binding = ItemCandidateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CandidateViewHolder(binding, onVoteClicked)
    }

    override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CandidateViewHolder(
        private val binding: ItemCandidateBinding,
        private val onVoteClicked: (CandidateRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CandidateRecord) {
            binding.textCandidateName.text = item.fullName
            binding.textCandidateParty.text = item.party
            binding.textCandidatePlatform.text = item.platform
            binding.buttonVote.setOnClickListener { onVoteClicked(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CandidateRecord>() {
        override fun areItemsTheSame(oldItem: CandidateRecord, newItem: CandidateRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CandidateRecord, newItem: CandidateRecord): Boolean {
            return oldItem == newItem
        }
    }
}

