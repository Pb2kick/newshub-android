package com.example.newshub.feature.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.databinding.ItemCommentBinding
import com.example.newshub.formatRelativeTime

class CommentAdapter(
    private val currentUserId: String?,
    private val onDelete: (CommentItem) -> Unit,
    private val onReport: (CommentItem) -> Unit
) : ListAdapter<CommentItem, CommentAdapter.CommentViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding, currentUserId, onDelete, onReport)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(
        private val binding: ItemCommentBinding,
        private val currentUserId: String?,
        private val onDelete: (CommentItem) -> Unit,
        private val onReport: (CommentItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentItem) {
            val initials = item.displayName.trim().split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { "?" }
            binding.textAvatarInitials.text = initials
            binding.textDisplayName.text = item.displayName
            binding.textTimestamp.text = formatRelativeTime(item.createdAt)
            binding.textContent.text = item.content
            binding.badgeReply.visibility = if (item.parentId.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.textLikeCount.text = "0"

            val isOwn = !currentUserId.isNullOrBlank() && item.userId == currentUserId
            binding.buttonDelete.visibility = if (isOwn) View.VISIBLE else View.GONE
            binding.buttonDelete.setOnClickListener { onDelete(item) }
            binding.buttonReport.setOnClickListener { onReport(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CommentItem>() {
        override fun areItemsTheSame(oldItem: CommentItem, newItem: CommentItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommentItem, newItem: CommentItem): Boolean {
            return oldItem == newItem
        }
    }
}
