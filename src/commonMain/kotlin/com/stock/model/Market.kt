package com.stock.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Market {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private var onUpdateCallback: (() -> Unit)? = null

    private val _stocks = mutableListOf<Stock>()
    private val stockMap = mutableMapOf<String, Stock>()
    private val _sectors = mutableListOf<String>()

    val stocks: List<Stock> get() = _stocks
    val sectors: List<String> get() = _sectors

    init {
        fun add(stock: Stock) {
            _stocks.add(stock)
            stockMap[stock.symbol.uppercase()] = stock
            if (stock.sector !in _sectors) _sectors.add(stock.sector)
        }

        add(Stock("AAPL", "Apple Inc.", "Technology", 189.50))
        add(Stock("GOOG", "Alphabet Inc.", "Technology", 2820.00))
        add(Stock("MSFT", "Microsoft Corp.", "Technology", 378.00))
        add(Stock("NVDA", "NVIDIA Corp.", "Technology", 875.00))
        add(Stock("META", "Meta Platforms", "Technology", 485.00))
        add(Stock("AMZN", "Amazon.com", "Technology", 3420.00))
        add(Stock("TSLA", "Tesla Inc.", "Automotive", 245.00))
        add(Stock("F", "Ford Motor Co.", "Automotive", 12.50))
        add(Stock("RIVN", "Rivian Automotive", "Automotive", 18.75))
        add(Stock("JPM", "JPMorgan Chase", "Finance", 195.00))
        add(Stock("V", "Visa Inc.", "Finance", 275.00))
        add(Stock("GS", "Goldman Sachs", "Finance", 385.00))
        add(Stock("JNJ", "Johnson & Johnson", "Healthcare", 156.00))
        add(Stock("PFE", "Pfizer Inc.", "Healthcare", 28.50))
        add(Stock("UNH", "UnitedHealth Group", "Healthcare", 527.00))
        add(Stock("KO", "Coca-Cola Co.", "Consumer", 59.00))
        add(Stock("NKE", "Nike Inc.", "Consumer", 108.00))
        add(Stock("SBUX", "Starbucks Corp.", "Consumer", 98.00))
        add(Stock("COIN", "Coinbase Global", "Fintech", 185.00))
        add(Stock("SQ", "Block Inc.", "Fintech", 78.00))
    }

    fun setOnUpdateCallback(callback: (() -> Unit)?) {
        onUpdateCallback = callback
    }

    fun startSimulation() {
        if (updateJob?.isActive == true) return
        updateJob = scope.launch {
            while (isActive) {
                _stocks.forEach { it.updatePrice() }
                onUpdateCallback?.invoke()
                delay(1000)
            }
        }
    }

    fun stopSimulation() {
        updateJob?.cancel()
        updateJob = null
    }

    fun getStock(symbol: String): Stock? = stockMap[symbol.uppercase()]
}
