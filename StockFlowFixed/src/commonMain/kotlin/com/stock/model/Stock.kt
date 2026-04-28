package com.stock.model

import kotlin.random.Random

class Stock(
    val symbol: String,
    val name: String,
    val sector: String,
    initialPrice: Double,
) {
    var price: Double = initialPrice
    private set
    var previousPrice: Double = initialPrice
    private set
    val openPrice: Double = initialPrice
    var dayHigh: Double = initialPrice
    private set
    var dayLow: Double = initialPrice
    private set

    val changePercent: Double
    get() = if (openPrice == 0.0) 0.0 else ((price - openPrice) / openPrice) * 100.0

    // Called by Finnhub real-price updates
    fun updatePriceFrom(newPrice: Double) {
        if (newPrice <= 0.0) return
            previousPrice = price
            price = newPrice
            dayHigh = maxOf(dayHigh, price)
            dayLow = minOf(dayLow, price)
    }

    // Kept as fallback if Finnhub returns 0 for a symbol
    fun updatePrice() {
        previousPrice = price
        val change = (Random.nextDouble() * 0.004) - 0.002
        price = (price * (1.0 + change)).coerceAtLeast(0.01)
        dayHigh = maxOf(dayHigh, price)
        dayLow = minOf(dayLow, price)
    }
}
