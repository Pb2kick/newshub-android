package com.example.newshub.feature.profile

sealed class VerificationStatus {
    data object Verified : VerificationStatus()
    data object Pending : VerificationStatus()
    data class Rejected(val reason: String) : VerificationStatus()
    data object NotSubmitted : VerificationStatus()
}
