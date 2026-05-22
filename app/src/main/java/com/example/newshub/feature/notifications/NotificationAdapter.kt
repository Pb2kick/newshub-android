package com.example.newshub.feature.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.R
import com.example.newshub.databinding.ItemNotificationBinding
import com.example.newshub.formatRelativeTime

class NotificationAdapter(
    private val onDelete: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onDelete: (NotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            binding.unreadAccent.visibility = if (item.isRead) View.GONE else View.VISIBLE
            binding.textActor.text = item.actorName.ifBlank { "NewsHub" }
            binding.textTime.text = formatRelativeTime(item.createdAt)
            binding.textMessage.text = formatMessage(item)
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }

        private fun formatMessage(item: NotificationItem): String {
            val context = binding.root.context
            return when (item.type.uppercase()) {
                "COMMENT_REPLY" -> context.getString(
                    R.string.notification_comment_reply,
                    item.actorName,
                    item.articleTitle.orEmpty().ifBlank { "an article" }
                )
                "VERIFICATION_APPROVED" -> context.getString(R.string.notification_verification_approved)
                "VERIFICATION_REJECTED" -> context.getString(
                    R.string.notification_verification_rejected,
                    item.rejectionReason.orEmpty().ifBlank { "—" }
                )
                else -> item.type
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}
