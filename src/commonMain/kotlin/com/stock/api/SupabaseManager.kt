package com.stock.api

import com.stock.auth.SessionStorage
import com.stock.model.Transaction
import com.stock.model.User
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val SUPABASE_URL = "https://lrwnpjulgtkcvkrknmsg.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_LEQvPGnrusAM--OYJntoxA_CR4wESMJ"

// Lenient JSON for session de/serialisation
private val sessionJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable private data class DbProfile(val balance: Double, val initial_balance: Double)
@Serializable private data class DbPortfolio(val symbol: String, val quantity: Int, val avg_price: Double)
@Serializable private data class DbWatchlist(val symbol: String)
@Serializable private data class DbTx(
    val type: String, val symbol: String,
    val quantity: Int, val price_per_share: Double, val timestamp: Long
)

// Strong types for inserting/updating with user_id
@Serializable private data class DbProfileUpdate(val balance: Double, val initial_balance: Double)
@Serializable private data class DbPortfolioInsert(val user_id: String, val symbol: String, val quantity: Int, val avg_price: Double)
@Serializable private data class DbWatchlistInsert(val user_id: String, val symbol: String)
@Serializable private data class DbTxInsert(
    val user_id: String, val type: String, val symbol: String,
    val quantity: Int, val price_per_share: Double, val timestamp: Long
)

object SupabaseManager {

    val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        install(Auth) {
            if (com.stock.auth.isAndroid) {
                scheme = "stockflow"
                host = "login-callback"
            }
            // ── Persistent session: survives app restarts ──────────────────
            sessionManager = object : SessionManager {
                override suspend fun loadSession(): UserSession? {
                    val raw = SessionStorage.load() ?: return null
                    return runCatching {
                        sessionJson.decodeFromString<UserSession>(raw)
                    }.getOrNull()
                }
                override suspend fun saveSession(session: UserSession) {
                    runCatching {
                        SessionStorage.save(sessionJson.encodeToString(UserSession.serializer(), session))
                    }
                }
                override suspend fun deleteSession() {
                    SessionStorage.clear()
                }
            }
            alwaysAutoRefresh  = true   // silently refresh the JWT before it expires
            autoLoadFromStorage = true  // load session from sessionManager on init
        }
        install(Postgrest)
    }

    // ── Auth ────────────────────────────────────────────────────────────────

    /**
     * Called once at startup. Waits for the Auth plugin to finish loading the
     * persisted session, then validates it is still usable.
     * Returns true only when the user is genuinely logged in with a valid token.
     */
    suspend fun restoreSession(): Boolean {
        return try {
            client.auth.awaitInitialization()
            val user = client.auth.currentUserOrNull()
            if (user != null) {
                // Force a session refresh to verify the token is still valid.
                // If the refresh_token is expired/revoked this will throw.
                try {
                    client.auth.refreshCurrentSession()
                    client.auth.currentUserOrNull() != null
                } catch (e: Exception) {
                    // Token refresh failed — session is stale
                    println("Session refresh failed, clearing stale session: ${e.message}")
                    SessionStorage.clear()
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            println("restoreSession error: ${e.message}")
            // If anything goes wrong loading the session, clear it and start fresh
            SessionStorage.clear()
            false
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) { this.email = email; this.password = password }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) { this.email = email; this.password = password }
    }


    suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        // Pass redirectUrl as a parameter to the function
        client.auth.signInWith(
            provider = Google,
            redirectUrl = if (com.stock.auth.isAndroid) "stockflow://login-callback" else null
        )
    }

    suspend fun signOut() = runCatching {
        client.auth.signOut()
        SessionStorage.clear()
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id
    fun isLoggedIn(): Boolean    = client.auth.currentUserOrNull() != null

    // ── Data ────────────────────────────────────────────────────────────────

    suspend fun loadUser(default: Double): User {
        return try {
            val uid = currentUserId() ?: return User(default)

            // RLS (auth.uid() = id) ensures only the current user's row comes back
            val profileList = client.from("profiles").select().decodeList<DbProfile>()
            val profile = profileList.firstOrNull() ?: return User(default)

            val portfolio = linkedMapOf<String, Int>()
            val avgPrices = linkedMapOf<String, Double>()
            client.from("portfolio_items").select()
                .decodeList<DbPortfolio>()
                .forEach { portfolio[it.symbol] = it.quantity; avgPrices[it.symbol] = it.avg_price }

            val watchlist = client.from("watchlist_items").select()
                .decodeList<DbWatchlist>()
                .map { it.symbol }.toMutableList()

            val transactions = client.from("transactions_log")
                .select { order("timestamp", Order.ASCENDING) }
                .decodeList<DbTx>()
                .mapNotNull { row ->
                    val type = runCatching { Transaction.Type.valueOf(row.type) }.getOrNull()
                        ?: return@mapNotNull null
                    Transaction(type, row.symbol, row.quantity, row.price_per_share, row.timestamp)
                }.toMutableList()

            User(profile.balance, profile.initial_balance, portfolio, avgPrices, transactions, watchlist)
        } catch (e: Exception) {
            println("loadUser error: ${e.message}")
            User(default)
        }
    }

    suspend fun saveUser(user: User) {
        val uid = currentUserId() ?: return
        try {
            // Update balance
            client.from("profiles").update(
                DbProfileUpdate(user.balance, user.initialBalance)
            ) { filter { eq("id", uid) } }

            // Replace portfolio
            client.from("portfolio_items").delete { filter { eq("user_id", uid) } }
            if (user.portfolio.isNotEmpty()) {
                client.from("portfolio_items").insert(
                    user.portfolio.map { (sym, qty) ->
                        DbPortfolioInsert(uid, sym, qty, user.avgBuyPrice[sym] ?: 0.0)
                    }
                )
            }

            // Replace watchlist
            client.from("watchlist_items").delete { filter { eq("user_id", uid) } }
            if (user.watchlist.isNotEmpty()) {
                client.from("watchlist_items").insert(
                    user.watchlist.map { sym -> DbWatchlistInsert(uid, sym) }
                )
            }

            // Replace last 200 transactions
            val txs = user.transactions.takeLast(200)
            client.from("transactions_log").delete { filter { eq("user_id", uid) } }
            if (txs.isNotEmpty()) {
                client.from("transactions_log").insert(
                    txs.map { tx ->
                        DbTxInsert(uid, tx.type.name, tx.symbol, tx.quantity, tx.pricePerShare, tx.timestamp)
                    }
                )
            }
        } catch (e: Exception) {
            println("saveUser error: ${e.message}")
        }
    }

    suspend fun resetUser() {
        val uid = currentUserId() ?: return
        try {
            client.from("portfolio_items").delete  { filter { eq("user_id", uid) } }
            client.from("watchlist_items").delete  { filter { eq("user_id", uid) } }
            client.from("transactions_log").delete { filter { eq("user_id", uid) } }
            client.from("profiles").update(
                DbProfileUpdate(1_000_000.0, 1_000_000.0)
            ) { filter { eq("id", uid) } }
        } catch (e: Exception) {
            println("resetUser error: ${e.message}")
        }
    }
}
