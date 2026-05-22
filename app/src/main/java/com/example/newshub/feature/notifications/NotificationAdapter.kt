package com.example.newshub.feature.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
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
            val unread = !item.isRead
            binding.dotUnread.visibility = if (unread) View.VISIBLE else View.GONE
            binding.iconContainer.setBackgroundResource(
                if (unread) R.drawable.bg_notification_icon else R.drawable.bg_notification_icon_muted
            )
            binding.imageIcon.setImageResource(iconForType(item.type))
            binding.imageIcon.setColorFilter(
                ContextCompat.getColor(
                    binding.root.context,
                    if (unread) R.color.home_primary else R.color.home_muted
                )
            )
            binding.textTitle.text = titleFor(item)
            binding.textMessage.text = formatMessage(item)
            binding.textTime.text = formatRelativeTime(item.createdAt)
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }

        private fun titleFor(item: NotificationItem): String {
            return when (item.type.lowercase()) {
                "election_reminder", "election" -> "Upcoming Election Reminder"
                "verification", "voter_verification" -> "Voter ID Verified"
                "comment", "article" -> item.articleTitle ?: "News Update"
                "system", "maintenance" -> "System Maintenance"
                else -> item.actorName.ifBlank { "NewsHub Alert" }
            }
        }

        private fun iconForType(type: String): Int {
            return when (type.lowercase()) {
                "verification", "voter_verification" -> R.drawable.ic_shield
                "comment", "article" -> R.drawable.ic_nav_home
                "system", "maintenance" -> android.R.drawable.ic_dialog_alert
                else -> R.drawable.ic_nav_elections
            }
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
                else -> {
                    val article = item.articleTitle?.takeIf { it.isNotBlank() }
                    listOfNotNull(item.actorName.takeIf { it.isNotBlank() }, article)
                        .joinToString(" • ")
                        .ifBlank { item.type.replace('_', ' ') }
                }
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
