package com.budgettracker.utils
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("budget_session", Context.MODE_PRIVATE)
    companion object {
        private const val TAG = "SessionManager"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        const val NO_USER = -1
    }
    fun saveSession(userId: Int, username: String) {
        Log.d(TAG, "Saving session for user: $username")
        prefs.edit().putInt(KEY_USER_ID, userId).putString(KEY_USERNAME, username).apply()
    }
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, NO_USER)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun isLoggedIn(): Boolean = getUserId() != NO_USER
    fun clearSession() { prefs.edit().clear().apply() }
}
