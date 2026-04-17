package com.stock.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stock.storage.DataStore
import com.stock.ui.StockFlowApp   // This is actually in App.kt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()   // ← Modern Android best practice
        super.onCreate(savedInstanceState)

        // Give DataStore the application context (unchanged, just clearer)
        DataStore.context = applicationContext

        setContent {
            StockFlowApp()
        }
    }
}
