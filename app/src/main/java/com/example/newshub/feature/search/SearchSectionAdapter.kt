package com.example.newshub.feature.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.databinding.ItemSearchResultBinding

class SearchSectionAdapter(
    private val onArticleClick: (SearchRow.Article) -> Unit,
    private val onElectionClick: (SearchRow.Election) -> Unit,
    private val onCandidateClick: (SearchRow.Candidate) -> Unit
) : ListAdapter<SearchRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchRow.Header -> VIEW_HEADER
            is SearchRow.Article -> VIEW_ITEM
            is SearchRow.Election -> VIEW_ITEM
            is SearchRow.Candidate -> VIEW_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == VIEW_HEADER) {
            HeaderViewHolder(binding)
        } else {
            ItemViewHolder(binding, onArticleClick, onElectionClick, onCandidateClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is SearchRow.Header -> (holder as HeaderViewHolder).bind(row)
            is SearchRow.Article -> (holder as ItemViewHolder).bind(row)
            is SearchRow.Election -> (holder as ItemViewHolder).bind(row)
            is SearchRow.Candidate -> (holder as ItemViewHolder).bind(row)
        }
    }

    private class HeaderViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: SearchRow.Header) {
            binding.textTitle.text = row.title
            binding.textSubtitle.visibility = android.view.View.GONE
            binding.root.setOnClickListener(null)
            binding.root.isClickable = false
        }
    }

    private class ItemViewHolder(
        private val binding: ItemSearchResultBinding,
        private val onArticleClick: (SearchRow.Article) -> Unit,
        private val onElectionClick: (SearchRow.Election) -> Unit,
        private val onCandidateClick: (SearchRow.Candidate) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: SearchRow) {
            binding.textSubtitle.visibility = android.view.View.VISIBLE
            when (row) {
                is SearchRow.Article -> {
                    binding.textTitle.text = row.article.title
                    binding.textSubtitle.text = listOf(row.article.source, row.article.category)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    binding.root.setOnClickListener { onArticleClick(row) }
                }
                is SearchRow.Election -> {
                    binding.textTitle.text = row.election.name
                    binding.textSubtitle.text = row.election.status
                    binding.root.setOnClickListener { onElectionClick(row) }
                }
                is SearchRow.Candidate -> {
                    binding.textTitle.text = row.candidate.fullName
                    binding.textSubtitle.text = row.candidate.party
                    binding.root.setOnClickListener { onCandidateClick(row) }
                }
                is SearchRow.Header -> Unit
            }
        }
    }

    private companion object {
        const val VIEW_HEADER = 0
        const val VIEW_ITEM = 1

        val Diff = object : DiffUtil.ItemCallback<SearchRow>() {
            override fun areItemsTheSame(oldItem: SearchRow, newItem: SearchRow): Boolean {
                return when {
                    oldItem is SearchRow.Header && newItem is SearchRow.Header ->
                        oldItem.title == newItem.title
                    oldItem is SearchRow.Article && newItem is SearchRow.Article ->
                        oldItem.article.id == newItem.article.id
                    oldItem is SearchRow.Election && newItem is SearchRow.Election ->
                        oldItem.election.id == newItem.election.id
                    oldItem is SearchRow.Candidate && newItem is SearchRow.Candidate ->
                        oldItem.candidate.id == newItem.candidate.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: SearchRow, newItem: SearchRow): Boolean {
                return oldItem == newItem
            }
        }
    }
}
