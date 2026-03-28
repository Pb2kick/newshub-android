package com.example.newshub.network.model

import com.google.gson.annotations.SerializedName

data class ProfileRowResponse(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String? = null
)

data class ProfileUpsertRequest(
    @SerializedName("auth_user_id") val authUserId: String? = null,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerializedName("email") val email: String? = null
)

