package com.stock.auth

import android.content.Context

actual object SessionStorage {
    private var _context: Context? = null

        fun init(context: Context) {
            _context = context.applicationContext
        }

        private fun prefs() = _context?.getSharedPreferences("stockflow_auth", Context.MODE_PRIVATE)

        actual suspend fun save(json: String) {
            prefs()?.edit()?.putString("session", json)?.apply()
        }

        actual suspend fun load(): String? = prefs()?.getString("session", null)

        actual suspend fun clear() {
            prefs()?.edit()?.remove("session")?.apply()
        }
}
