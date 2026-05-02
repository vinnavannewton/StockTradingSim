package com.stock.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stock.auth.SessionStorage
import com.stock.storage.DataStore
import com.stock.ui.StockFlowApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inject context before ANYTHING else so SessionStorage & DataStore work
        val ctx = applicationContext
        SessionStorage.context = ctx
        DataStore.context      = ctx

        setContent {
            StockFlowApp()
        }
    }
}
