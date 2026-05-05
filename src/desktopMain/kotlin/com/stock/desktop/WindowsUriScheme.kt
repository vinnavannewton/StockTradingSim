package com.stock.desktop

import java.io.File

/**
 * Registers the "stockflow://" custom URL scheme in the Windows registry
 * (HKEY_CURRENT_USER\Software\Classes\stockflow) so that the browser can
 * redirect back to this app after Google OAuth.
 *
 * - Safe to call on every launch; it is idempotent.
 * - Does nothing on non-Windows platforms.
 * - Uses only HKCU (no admin/UAC required).
 */
fun registerWindowsUriSchemeIfNeeded() {
    val os = System.getProperty("os.name", "").lowercase()
    if (!os.contains("windows")) return

    try {
        // Find the path of the running EXE / launcher
        val exePath = ProcessHandle.current().info().command().orElse(null)
            ?: return

        // Write a tiny .reg script and import it silently
        val regContent = """
            Windows Registry Editor Version 5.00

            [HKEY_CURRENT_USER\Software\Classes\stockflow]
            @="StockFlow OAuth Handler"
            "URL Protocol"=""

            [HKEY_CURRENT_USER\Software\Classes\stockflow\shell]

            [HKEY_CURRENT_USER\Software\Classes\stockflow\shell\open]

            [HKEY_CURRENT_USER\Software\Classes\stockflow\shell\open\command]
            @="\"${exePath.replace("\\", "\\\\")}\" \"%1\""
        """.trimIndent()

        val regFile = File(System.getProperty("java.io.tmpdir"), "stockflow_uri.reg")
        regFile.writeText(regContent)

        // reg import is silent and requires no elevation for HKCU
        val process = ProcessBuilder("reg", "import", regFile.absolutePath)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        regFile.delete()

        println("URI scheme 'stockflow://' registered successfully.")
    } catch (e: Exception) {
        // Non-fatal: OAuth deep-link will just fail to auto-close the browser tab
        println("Could not register URI scheme: ${e.message}")
    }
}
