package ru.ryabov.studentperformance.data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Хранение текущей сессии (userId, JWT) после входа.
 * Для production предпочтительно использовать EncryptedSharedPreferences или DataStore.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: Long?
        get() = if (prefs.contains(KEY_USER_ID)) prefs.getLong(KEY_USER_ID, -1L).takeIf { it >= 0 } else null
        set(value) = prefs.edit().apply {
            if (value != null) putLong(KEY_USER_ID, value)
            else remove(KEY_USER_ID)
        }.apply()

    var userRole: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var userFullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(value) = prefs.edit().putString(KEY_FULL_NAME, value).apply()

    fun saveSession(token: String, userId: Long, role: String, email: String? = null, fullName: String? = null) {
        this.token = token
        this.userId = userId
        this.userRole = role
        this.userEmail = email
        this.userFullName = fullName
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_ROLE)
            .remove(KEY_EMAIL)
            .remove(KEY_FULL_NAME)
            .apply()
    }

    /** true, если есть токен (онлайн-вход) или сохранён userId (офлайн-вход). */
    fun isLoggedIn(): Boolean = !token.isNullOrBlank() || userId != null

    companion object {
        private const val PREFS_NAME = "student_performance_session"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "user_role"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_FULL_NAME = "user_full_name"
    }
}
