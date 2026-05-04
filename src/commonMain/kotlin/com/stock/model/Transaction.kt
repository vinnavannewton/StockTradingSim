package com.stock.model

data class Transaction(
    val type: Type,
    val symbol: String,
    val quantity: Int,
    val pricePerShare: Double,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val totalAmount: Double = pricePerShare * quantity

    enum class Type { BUY, SELL }
}
