package com.stock.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TOKEN   = "d7od0n9r01qsb7befgngd7od0n9r01qsb7befgo0"
private const val BASE    = "https://finnhub.io/api/v1"
    private const val TTL_MS  = 60_000L

    @Serializable
    private data class Quote(
        @SerialName("c")  val current:   Double = 0.0,
                             @SerialName("pc") val prevClose: Double = 0.0,
    )

    object FinnhubClient {

        private val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        private val cache = mutableMapOf<String, Pair<Double, Long>>()

        suspend fun getPrice(symbol: String): Double {
            val now = System.currentTimeMillis()
            cache[symbol]?.let { (p, t) -> if (now - t < TTL_MS) return p }
            return try {
                val q: Quote = client.get("$BASE/quote") {
                    parameter("symbol", symbol)
                    parameter("token", TOKEN)
                }.body()
                val price = if (q.current > 0) q.current else q.prevClose
                if (price > 0) cache[symbol] = price to now
                    price.takeIf { it > 0 } ?: (cache[symbol]?.first ?: 0.0)
            } catch (_: Exception) {
                cache[symbol]?.first ?: 0.0
            }
        }

        suspend fun fetchPrice(symbol: String): Double? = getPrice(symbol).takeIf { it > 0.0 }

        suspend fun fetchAll(symbols: List<String>, onEach: (String, Double) -> Unit) {
            symbols.forEach { sym ->
                val price = getPrice(sym)
                if (price > 0.0) onEach(sym, price)
                    delay(200) // stay under 60 req/min free tier limit
            }
        }
    }
