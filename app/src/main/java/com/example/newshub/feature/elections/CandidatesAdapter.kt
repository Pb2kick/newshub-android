package com.example.newshub.feature.elections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.newshub.CandidateRecord
import com.example.newshub.R
import com.example.newshub.databinding.ItemCandidateBinding

class CandidatesAdapter(
    private val onViewProfile: (CandidateRecord) -> Unit
) : ListAdapter<CandidateRecord, CandidatesAdapter.CandidateViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        val binding = ItemCandidateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CandidateViewHolder(binding, onViewProfile)
    }

    override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CandidateViewHolder(
        private val binding: ItemCandidateBinding,
        private val onViewProfile: (CandidateRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CandidateRecord) {
            binding.textCandidateName.text = item.fullName
            binding.textCandidateParty.text = item.party
            binding.textCandidatePosition.text = item.position.ifBlank { "Candidate" }
            binding.imageCandidate.load(item.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_home_feature_placeholder)
                error(R.drawable.bg_home_feature_placeholder)
            }
            binding.buttonViewProfile.setOnClickListener { onViewProfile(item) }
            binding.root.setOnClickListener { onViewProfile(item) }
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
