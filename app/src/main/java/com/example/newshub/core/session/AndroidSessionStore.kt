package com.example.newshub.core.session

import android.content.Context
import com.example.newshub.SessionPrefs

class AndroidSessionStore(
    private val context: Context
) : SessionStore {

    override fun saveSession(userId: String, accessToken: String) {
        SessionPrefs.saveSession(context, userId, accessToken)
    }

    override fun getUserId(): String? {
        return SessionPrefs.getUserId(context)
    }

    override fun getAccessToken(): String? {
        return SessionPrefs.getAccessToken(context)
    }

    override fun clear() {
        SessionPrefs.clear(context)
    }
}

