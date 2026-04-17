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

    fun updatePrice() {
        previousPrice = price
        val changePercent = (Random.nextDouble() * 0.04) - 0.02
        price = (price * (1.0 + changePercent)).coerceAtLeast(0.01)
        dayHigh = maxOf(dayHigh, price)
        dayLow = minOf(dayLow, price)
    }
}
