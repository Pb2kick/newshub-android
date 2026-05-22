package com.example.newshub.feature.notifications

data class NotificationItem(
    val id: String,
    val type: String,
    val actorName: String,
    val articleTitle: String?,
    val rejectionReason: String?,
    val createdAt: String,
    val isRead: Boolean
)
