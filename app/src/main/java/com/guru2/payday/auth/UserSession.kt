package com.guru2.payday.auth

import android.content.Context

class UserSession(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val userId: Long
        get() = preferences.getLong(KEY_USER_ID, NO_USER)

    val isLoggedIn: Boolean
        get() = userId != NO_USER

    fun signIn(userId: Long) {
        preferences.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun signOut() {
        preferences.edit().remove(KEY_USER_ID).apply()
    }

    companion object {
        const val NO_USER = -1L
        private const val PREFERENCES_NAME = "payday_session"
        private const val KEY_USER_ID = "current_user_id"
    }
}
