package com.fyp.healthcare

import android.content.Context
import android.content.SharedPreferences

/**
 * FUTURE ROADMAP (AUTH / ACCOUNTS):
 * - This login section will connect into SQL for usability instead of locally in the future.
 * - Login section will also need to verify email (OTP / confirmation) so people won't abuse it.
 * - Passwords are stored in plain text ONLY for testing — must be hashed/encrypted later.
 */

class UserManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("care_app_prefs", Context.MODE_PRIVATE)

    // Creates a test account. Returns false if the email is already registered
    fun registerUser(fullName: String, email: String, password: String): Boolean {
        if (getPassword(email) != null) return false
        prefs.edit()
            .putString("pwd_$email", password)
            .putString("name_$email", fullName)
            .apply()
        return true
    }

    fun loginUser(email: String, password: String): Boolean {
        val saved = getPassword(email)
        return saved != null && saved == password
    }

    fun getFullName(email: String): String? = prefs.getString("name_$email", null)

    private fun getPassword(email: String): String? = prefs.getString("pwd_$email", null)

    // ===== "Don't login twice" logic =====
    fun saveSession(email: String) {
        prefs.edit().putString("logged_in_user", email).apply()
    }

    fun getLoggedInUser(): String? = prefs.getString("logged_in_user", null)
    // Clears the saved session so the user must login again
    fun clearSession() {
        prefs.edit().remove("logged_in_user").apply()
    }
}
