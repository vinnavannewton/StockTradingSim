package com.stock.auth

import java.io.File

actual object SessionStorage {
    private val file = File(System.getProperty("user.home"), ".stockflow_session.json")

    actual suspend fun save(json: String) {
        runCatching { file.writeText(json) }
    }

    actual suspend fun load(): String? = runCatching {
        if (file.exists()) file.readText() else null
    }.getOrNull()

    actual suspend fun clear() {
        runCatching { file.delete() }
    }
}
