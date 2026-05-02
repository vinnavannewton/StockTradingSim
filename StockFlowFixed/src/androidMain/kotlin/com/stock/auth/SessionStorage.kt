package com.stock.auth

import android.content.Context

actual object SessionStorage {
    // Injected by MainActivity before SupabaseManager initialises
    lateinit var context: Context

    private fun prefs() = context.getSharedPreferences("stockflow_auth", Context.MODE_PRIVATE)

    actual suspend fun save(json: String) {
        prefs().edit().putString("session", json).apply()
    }

    actual suspend fun load(): String? = prefs().getString("session", null)

    actual suspend fun clear() {
        prefs().edit().remove("session").apply()
    }
}
