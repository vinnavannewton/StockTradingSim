package com.stock.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stock.auth.SessionStorage
import com.stock.api.SupabaseManager
import com.stock.storage.DataStore
import com.stock.ui.StockFlowApp
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SessionStorage.init(applicationContext)
        DataStore.context = applicationContext
        // Handle deep link if app was cold-started via OAuth redirect
        SupabaseManager.client.handleDeeplinks(intent)
        setContent {
            StockFlowApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle deep link when app was already running (warm start)
        SupabaseManager.client.handleDeeplinks(intent)
    }
}
