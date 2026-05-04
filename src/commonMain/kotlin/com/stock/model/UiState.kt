package com.stock.model

data class UiState(
    val balance: Double,
    val portfolio: Map<String, Int>,
    val avgPrices: Map<String, Double>,
    val transactions: List<Transaction>,
    val watchlist: List<String>,
    val prices: Map<String, Double>,
    val netWorth: Double,
    val totalPnL: Double,
) {
    companion object {
        fun from(user: User, market: Market): UiState = UiState(
            balance      = user.balance,
            portfolio    = user.portfolio.toMap(),
                                                                avgPrices    = user.avgBuyPrice.toMap(),
                                                                transactions = user.transactions.toList(),
                                                                watchlist    = user.watchlist.toList(),
                                                                prices       = market.stocks.associate { it.symbol to it.price },
                                                                netWorth     = user.getNetWorth(market),
                                                                totalPnL     = user.getTotalProfitLoss(market),
        )

        fun empty(): UiState = UiState(
            balance = 0.0, portfolio = emptyMap(), avgPrices = emptyMap(),
                                       transactions = emptyList(), watchlist = emptyList(),
                                       prices = emptyMap(), netWorth = 0.0, totalPnL = 0.0,
        )
    }
}
