package com.stock.auth

/**
 * Platform-specific session persistence.
 * Android → SharedPreferences  |  Desktop → ~/.stockflow_session file
 */
expect object SessionStorage {
    suspend fun save(json: String)
    suspend fun load(): String?
    suspend fun clear()
}
