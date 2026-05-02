package com.stock.storage

import com.stock.model.Transaction
import com.stock.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(val id: String, val balance: Double, val initial_balance: Double)

@Serializable
data class PortfolioItemRow(
    val user_id: String,
    val symbol: String,
    val quantity: Int,
    val avg_price: Double
)

@Serializable
data class WatchlistItemRow(val user_id: String, val symbol: String)

@Serializable
data class TransactionRow(
    val user_id: String,
    val type: String,
    val symbol: String,
    val quantity: Int,
    val price_per_share: Double,
    val timestamp: Long
)

suspend fun loadUserFromCloud(defaultBalance: Double): User? {
    return try {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return null

        val profile = supabase.postgrest["profiles"]
        .select { filter { eq("id", uid) } }
        .decodeSingleOrNull<ProfileRow>() ?: return null

        val portfolioRows = supabase.postgrest["portfolio_items"]
        .select { filter { eq("user_id", uid) } }
        .decodeList<PortfolioItemRow>()

        val watchlistRows = supabase.postgrest["watchlist_items"]
        .select { filter { eq("user_id", uid) } }
        .decodeList<WatchlistItemRow>()

        val txRows = supabase.postgrest["transactions_log"]
        .select { filter { eq("user_id", uid) } }
        .decodeList<TransactionRow>()

        User(
            balance = profile.balance,
             initialBalance = profile.initial_balance,
             portfolio = linkedMapOf<String, Int>().also { map ->
                 portfolioRows.forEach { map[it.symbol] = it.quantity }
             },
             avgBuyPrice = linkedMapOf<String, Double>().also { map ->
                 portfolioRows.forEach { map[it.symbol] = it.avg_price }
             },
             watchlist = watchlistRows.map { it.symbol }.toMutableList(),
             transactions = txRows.map {
                 Transaction(
                     Transaction.Type.valueOf(it.type),
                             it.symbol,
                             it.quantity,
                             it.price_per_share,
                             it.timestamp
                 )
             }.toMutableList()
        )
    } catch (_: Exception) {
        null
    }
}

suspend fun saveUserToCloud(user: User) {
    try {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return

        supabase.postgrest["profiles"].upsert(
            ProfileRow(uid, user.balance, user.initialBalance)
        )

        supabase.postgrest["portfolio_items"]
        .delete { filter { eq("user_id", uid) } }
        if (user.portfolio.isNotEmpty()) {
            supabase.postgrest["portfolio_items"].insert(
                user.portfolio.map { (sym, qty) ->
                    PortfolioItemRow(uid, sym, qty, user.avgBuyPrice[sym] ?: 0.0)
                }
            )
        }

        supabase.postgrest["watchlist_items"]
        .delete { filter { eq("user_id", uid) } }
        if (user.watchlist.isNotEmpty()) {
            supabase.postgrest["watchlist_items"].insert(
                user.watchlist.map { WatchlistItemRow(uid, it) }
            )
        }
    } catch (_: Exception) {
        // local save already worked, cloud fail is silent
    }
}
