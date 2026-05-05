package com.stock.desktop

import com.stock.api.SupabaseManager
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DesktopAuthTest {
    @Test
    fun testLoginFailsCleanly() = runBlocking {
        try {
            val result = SupabaseManager.signIn("test@test.com", "wrongpassword")
            println("Test result: $result")
            result.onFailure {
                it.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
