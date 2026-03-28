package com.example.newshub

import android.content.Context

object SessionPrefs {
    private const val PREFS_NAME = "newshub_prefs"
    private const val PREF_USER_ID = "sb_user_id"
    private const val PREF_ACCESS_TOKEN = "sb_access_token"

    fun saveSession(context: Context, userId: String, accessToken: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_USER_ID, userId)
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .apply()
    }

    fun getUserId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_USER_ID, null)
    }

    fun getAccessToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_ACCESS_TOKEN, null)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_USER_ID)
            .remove(PREF_ACCESS_TOKEN)
            .apply()
    }
}

