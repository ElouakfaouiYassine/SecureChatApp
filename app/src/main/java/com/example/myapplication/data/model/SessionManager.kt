package com.example.myapplication.data.model

import android.content.Context

object SessionManager {

    fun saveLoggedInUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().putString("username", username).apply()
    }

    fun getLoggedInUsername(context: Context): String? {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("username", null)
    }
}
