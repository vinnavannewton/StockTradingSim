package com.stock.model

import androidx.compose.runtime.mutableStateListOf

class Stock(
    val symbol  : String,
    val name    : String,
    val sector  : String,
    initialPrice: Double,
) {
    var price: Double = initialPrice
    private set

    // Rolling price history: Pair(epochMillis, price)
    // Max 500 points — about 8 minutes at 1-second ticks
    val priceHistory = mutableStateListOf<Pair<Long, Double>>()

    init {
        // Seed history with the initial price so the chart is never empty
        priceHistory.add(System.currentTimeMillis() to initialPrice)
    }

    /** Called by Market every simulation tick. */
    fun updatePrice(newPrice: Double) {
        price = newPrice
        priceHistory.add(System.currentTimeMillis() to newPrice)
        if (priceHistory.size > 500) priceHistory.removeAt(0)
    }
}
