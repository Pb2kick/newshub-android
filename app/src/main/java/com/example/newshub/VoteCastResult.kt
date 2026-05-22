package com.example.newshub

data class VoteCastResult(
    val success: Boolean,
    val receiptId: String = "",
    val message: String = "",
    val reason: String = ""
)
