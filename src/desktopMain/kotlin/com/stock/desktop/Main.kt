package com.stock.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.stock.ui.StockFlowApp

fun main() = application {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)
    val icon = painterResource("icon.png")
    var uiScale by remember { mutableStateOf(1.0f) }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "StockFlow",
        state = windowState,
        icon = icon,
        onKeyEvent = { event ->
            if (event.isCtrlPressed && event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.Equals, Key.NumPadAdd -> { uiScale = (uiScale + 0.1f).coerceAtMost(3.0f); true }
                    Key.Minus, Key.NumPadSubtract -> { uiScale = (uiScale - 0.1f).coerceAtLeast(0.5f); true }
                    Key.Zero, Key.NumPad0 -> { uiScale = 1.0f; true }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        CompositionLocalProvider(LocalDensity provides Density(uiScale)) {
            StockFlowApp()
        }
    }
}
