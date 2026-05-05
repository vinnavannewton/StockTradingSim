package com.stock.model

class User(
    var balance: Double,
    var initialBalance: Double = balance,
    val portfolio: MutableMap<String, Int> = linkedMapOf(),
    val avgBuyPrice: MutableMap<String, Double> = linkedMapOf(),
    val transactions: MutableList<Transaction> = mutableListOf(),
    val watchlist: MutableList<String> = mutableListOf(),
) {
    fun buyStock(stock: Stock, quantity: Int): Boolean {
        if (quantity <= 0) return false
        val cost = stock.price * quantity
        if (cost > balance) return false

        balance -= cost
        val symbol = stock.symbol
        val existingQty = portfolio[symbol] ?: 0
        val existingCost = (avgBuyPrice[symbol] ?: 0.0) * existingQty
        val newQty = existingQty + quantity
        portfolio[symbol] = newQty
        avgBuyPrice[symbol] = (existingCost + cost) / newQty
        transactions.add(Transaction(Transaction.Type.BUY, symbol, quantity, stock.price))
        return true
    }

    fun sellStock(stock: Stock, quantity: Int): Boolean {
        if (quantity <= 0) return false
        val symbol = stock.symbol
        val held = portfolio[symbol] ?: 0
        if (held < quantity) return false

        balance += stock.price * quantity
        val remaining = held - quantity
        if (remaining == 0) {
            portfolio.remove(symbol)
            avgBuyPrice.remove(symbol)
        } else {
            portfolio[symbol] = remaining
        }
        transactions.add(Transaction(Transaction.Type.SELL, symbol, quantity, stock.price))
        return true
    }

    fun toggleWatchlist(symbol: String) {
        if (!watchlist.remove(symbol)) watchlist.add(symbol)
    }

    fun isInWatchlist(symbol: String): Boolean = symbol in watchlist

    fun getNetWorth(market: Market): Double {
        var total = balance
        for ((symbol, qty) in portfolio) {
            val stock = market.getStock(symbol) ?: continue
            total += stock.price * qty
        }
        return total
    }

    fun getTotalProfitLoss(market: Market): Double = getNetWorth(market) - initialBalance
}
