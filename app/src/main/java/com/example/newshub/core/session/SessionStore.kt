package com.example.newshub.core.session

interface SessionStore {
    fun saveSession(userId: String, accessToken: String)
    fun getUserId(): String?
    fun getAccessToken(): String?
    fun clear()
}

