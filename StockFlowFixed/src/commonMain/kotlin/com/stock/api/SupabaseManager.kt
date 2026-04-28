package com.stock.api

import com.stock.model.Transaction
import com.stock.model.User
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SUPABASE_URL = "https://lrwnpjulgtkcvkrknmsg.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_LEQvPGnrusAM--OYJntoxA_CR4wESMJ"

    @Serializable private data class DbProfile(val balance: Double, val initial_balance: Double)
    @Serializable private data class DbPortfolio(val symbol: String, val quantity: Int, val avg_price: Double)
    @Serializable private data class DbWatchlist(val symbol: String)
    @Serializable private data class DbTx(
        val type: String, val symbol: String,
        val quantity: Int, val price_per_share: Double, val timestamp: Long
    )

    object SupabaseManager {

        val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
            install(Auth)
            install(Postgrest)
        }

        // ── Auth ────────────────────────────────────────────────────────────────

        suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
            client.auth.signUpWith(Email) { this.email = email; this.password = password }
        }

        suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
            client.auth.signInWith(Email) { this.email = email; this.password = password }
        }

        suspend fun signOut() = runCatching { client.auth.signOut() }

        fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

        fun isLoggedIn(): Boolean = client.auth.currentUserOrNull() != null

        // ── Data ────────────────────────────────────────────────────────────────

        suspend fun loadUser(default: Double): User {
            return try {
                val profile = client.from("profiles")
                .select { }.decodeSingle<DbProfile>()

                val portfolio = linkedMapOf<String, Int>()
                val avgPrices = linkedMapOf<String, Double>()
                client.from("portfolio_items").select { }
                .decodeList<DbPortfolio>()
                .forEach { portfolio[it.symbol] = it.quantity; avgPrices[it.symbol] = it.avg_price }

                val watchlist = client.from("watchlist_items").select { }
                .decodeList<DbWatchlist>().map { it.symbol }.toMutableList()

                val transactions = client.from("transactions_log")
                .select { order("timestamp", Order.ASCENDING) }
                .decodeList<DbTx>()
                .mapNotNull { row ->
                    val type = runCatching { Transaction.Type.valueOf(row.type) }.getOrNull() ?: return@mapNotNull null
                    Transaction(type, row.symbol, row.quantity, row.price_per_share, row.timestamp)
                }.toMutableList()

                User(profile.balance, profile.initial_balance, portfolio, avgPrices, transactions, watchlist)
            } catch (_: Exception) {
                User(default)
            }
        }

        suspend fun saveUser(user: User) {
            try {
                client.from("profiles").update(
                    mapOf("balance" to user.balance, "initial_balance" to user.initialBalance)
                ) { filter { } }

                client.from("portfolio_items").delete { filter { } }
                if (user.portfolio.isNotEmpty()) {
                    client.from("portfolio_items").insert(
                        user.portfolio.map { (sym, qty) ->
                            DbPortfolio(sym, qty, user.avgBuyPrice[sym] ?: 0.0)
                        }
                    )
                }

                client.from("watchlist_items").delete { filter { } }
                if (user.watchlist.isNotEmpty()) {
                    client.from("watchlist_items").insert(user.watchlist.map { DbWatchlist(it) })
                }

                val txs = user.transactions.takeLast(200)
                client.from("transactions_log").delete { filter { } }
                if (txs.isNotEmpty()) {
                    client.from("transactions_log").insert(
                        txs.map { DbTx(it.type.name, it.symbol, it.quantity, it.pricePerShare, it.timestamp) }
                    )
                }
            } catch (_: Exception) {}
        }

        suspend fun resetUser() {
            try {
                client.from("portfolio_items").delete { filter { } }
                client.from("watchlist_items").delete { filter { } }
                client.from("transactions_log").delete { filter { } }
                client.from("profiles").update(
                    mapOf("balance" to 1000000.0, "initial_balance" to 1000000.0)
                ) { filter { } }
            } catch (_: Exception) {}
        }
    }
