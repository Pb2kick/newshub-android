package com.example.newshub.feature.news

data class CommentItem(
    val id: String,
    val articleId: String,
    val userId: String,
    val displayName: String,
    val content: String,
    val parentId: String?,
    val createdAt: String
)
