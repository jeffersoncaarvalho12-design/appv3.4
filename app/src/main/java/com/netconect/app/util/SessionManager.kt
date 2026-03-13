package com.netconect.app.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("netconect_session", Context.MODE_PRIVATE)

    fun saveLogin(token: String, username: String) {
        prefs.edit()
            .putString("token", token)
            .putString("username", username)
            .apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", "")
    }

    fun getUsername(): String? {
        return prefs.getString("username", "")
    }

    fun saveBaseUrl(baseUrl: String) {
        prefs.edit()
            .putString("base_url", baseUrl)
            .apply()
    }

    fun getBaseUrl(): String {
        return prefs.getString("base_url", "http://200.106.207.13:27004") ?: "http://200.106.207.13:27004"
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
