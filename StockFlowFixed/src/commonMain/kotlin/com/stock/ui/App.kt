package com.stock.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stock.model.Market
import com.stock.model.Stock
import com.stock.model.Transaction
import com.stock.model.UiState
import com.stock.model.User
import com.stock.storage.DataStore
import com.stock.util.formatMoney
import com.stock.util.formatSignedMoney
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.rememberCoroutineScope
import com.stock.storage.loadUserFromCloud
import com.stock.storage.saveUserToCloud
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.stock.api.SupabaseManager
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke

// ── Colours ────────────────────────────────────────────────────────────────
private val Background = Color(0xFF07090F)
private val Surface1   = Color(0xFF0F172A)
private val Surface2   = Color(0xFF1E293B)
private val Accent     = Color(0xFF38BDF8)
private val Good       = Color(0xFF4ADE80)
private val Bad        = Color(0xFFF87171)
private val Text1      = Color(0xFFF8FAFC)
private val Text2      = Color(0xFF94A3B8)
private val Border     = Color(0xFF334155)

private val sectorsColor = mapOf(
    "Technology" to Color(0xFF38BDF8),
    "Automotive" to Color(0xFFFB923C),
    "Finance"    to Color(0xFF4ADE80),
    "Healthcare" to Color(0xFFF472B6),
    "Consumer"   to Color(0xFFA78BFA),
    "Fintech"    to Color(0xFF22D3EE),
)

private enum class Section(val icon: ImageVector) {
    Market(Icons.Default.TrendingUp),
    Watchlist(Icons.Default.Star),
    Portfolio(Icons.Default.AccountBalanceWallet),
    History(Icons.Default.History),
    Settings(Icons.Default.Settings)
}

private fun formatTime(epochMillis: Long): String {
    val totalSeconds = epochMillis / 1000
    val hours   = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

// ── Preview-safe fake data ──────────────────────────────────────────────────
private fun fakeStocks() = listOf(
    Stock("AAPL", "Apple Inc.",      "Technology", 182.50),
    Stock("TSLA", "Tesla Motors",    "Automotive",  245.30),
    Stock("GOOG", "Alphabet Inc.",   "Technology", 142.10),
    Stock("JPM",  "JP Morgan",       "Finance",     198.75),
    Stock("AMZN", "Amazon.com",      "Consumer",    178.90),
)

private fun fakeUiState(): UiState {
    val stocks = fakeStocks()
    return UiState(
        balance      = 980_000.0,
        netWorth     = 1_050_000.0,
        totalPnL     = 50_000.0,
        prices       = stocks.associate { it.symbol to it.price },
        watchlist    = listOf("AAPL", "TSLA"),
        portfolio    = mapOf("AAPL" to 10, "GOOG" to 5),
        avgPrices    = mapOf("AAPL" to 170.0, "GOOG" to 130.0),
        transactions = listOf(
            Transaction(Transaction.Type.BUY, "AAPL", 10, 170.0, System.currentTimeMillis() - 60_000),
            Transaction(Transaction.Type.BUY, "GOOG",  5, 130.0, System.currentTimeMillis() - 30_000),
            Transaction(Transaction.Type.SELL, "TSLA",  3, 240.0, System.currentTimeMillis() - 10_000),
        ),
    )
}

// ── Auth state ───────────────────────────────────────────────────────────────
private enum class AuthState { CHECKING, LOGGED_OUT, LOGGED_IN }

// ── Root composable ─────────────────────────────────────────────────────────
@Composable
fun StockFlowApp() {
    val isPreview = LocalInspectionMode.current


    // Start as CHECKING so we wait for session restore before showing login
    var authState   by remember { mutableStateOf(if (isPreview) AuthState.LOGGED_IN else AuthState.CHECKING) }
    var isLoading   by remember { mutableStateOf(false) }
    val market      = remember { if (isPreview) null else Market() }
    var user        by remember { mutableStateOf<User?>(if (isPreview) User(1_000_000.0) else null) }

    val uiStateFlow = remember { MutableStateFlow(if (isPreview) fakeUiState() else UiState.empty()) }
    val uiState     by uiStateFlow.collectAsState()
    val scope = rememberCoroutineScope()

    // On first launch: restore persisted session before rendering login/main screen
    LaunchedEffect(Unit) {
        if (!isPreview) {
            val hasSession = SupabaseManager.restoreSession()
            authState = if (hasSession) AuthState.LOGGED_IN else AuthState.LOGGED_OUT
        }
    }

    // Whenever we become logged in, load user data
    LaunchedEffect(authState) {
        if (!isPreview && authState == AuthState.LOGGED_IN) {
            isLoading = true
            user = DataStore.load(1_000_000.0)
            uiStateFlow.value = UiState.from(user!!, market!!)
            isLoading = false
        }
    }

    if (!isPreview) {
        DisposableEffect(Unit) {
            scope.launch {
                val cloudUser = loadUserFromCloud(1_000_000.0)
                if (cloudUser != null) {
                    user = cloudUser
                    DataStore.save(cloudUser)
                    uiStateFlow.value = UiState.from(cloudUser, market!!)
                }
            }
            market!!.setOnUpdateCallback {
                val u = user ?: return@setOnUpdateCallback
                uiStateFlow.value = UiState.from(u, market)
            }
            market.startSimulation()
            onDispose { market.stopSimulation() }
        }
    }

    val sync: () -> Unit = {
        if (!isPreview) {
            scope.launch {
                val u = user ?: return@launch
                DataStore.save(u)
                saveUserToCloud(u)
                uiStateFlow.value = UiState.from(u, market!!)
            }
        }
    }

    val reset: () -> Unit = {
        if (!isPreview) {
            scope.launch {
                DataStore.reset()
                val fresh = User(1_000_000.0)
                user = fresh
                DataStore.save(fresh)
                saveUserToCloud(fresh)
                market?.let { uiStateFlow.value = UiState.from(fresh, it) }
            }
        }
    }

    val signOut: () -> Unit = {
        scope.launch {
            SupabaseManager.signOut()
            market?.stopSimulation()
            user = null
            uiStateFlow.value = UiState.empty()
            authState = AuthState.LOGGED_OUT
        }
    }

    // ── Show appropriate screen based on auth state ────────────────────────
    when (authState) {
        AuthState.CHECKING -> {
            // Splash / session restore in progress
            MaterialTheme {
                Surface(color = Background, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Accent)
                            Spacer(Modifier.height(16.dp))
                            Text("StockFlow", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Restoring session...", color = Text2, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        AuthState.LOGGED_OUT -> {
            LoginScreen(onLoginSuccess = { authState = AuthState.LOGGED_IN })
        }
        AuthState.LOGGED_IN -> {
            MaterialTheme {
                Surface(color = Background, modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Accent)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading your portfolio...", color = Text2, fontSize = 14.sp)
                            }
                        }
                        return@Surface
                    }
                    val currentUser = user ?: return@Surface
                    AppContent(
                        market    = market,
                        user      = currentUser,
                        state     = uiState,
                        sync      = sync,
                        onReset   = reset,
                        onSignOut = signOut,
                        isPreview = isPreview,
                    )
                }
            }
        }
    }
}

// ── AppContent ───────────────────────────────────────────────────────────────
@Composable
fun AppContent(
    market    : Market?,
    user      : User,
    state     : UiState,
    sync      : () -> Unit,
    onReset   : () -> Unit,
    onSignOut : () -> Unit,
    isPreview : Boolean,
) {
    var selectedSymbol  by remember { mutableStateOf("AAPL") }
    var quantityText    by remember { mutableStateOf("1") }
    var message         by remember { mutableStateOf("") }
    var messageIsError  by remember { mutableStateOf(false) }
    var currentSection  by remember { mutableStateOf(Section.Market) }
    var sectorFilter    by remember { mutableStateOf("All") }

    val stockList = if (isPreview) fakeStocks() else market!!.stocks
    val sectors   = if (isPreview) listOf("Technology","Automotive","Finance","Healthcare","Consumer","Fintech")
    else market!!.sectors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val compact = maxWidth < 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface1)
                .padding(16.dp)
        ) {
            HeaderBar(state.balance, state.netWorth, state.totalPnL)
            Spacer(Modifier.height(16.dp))

            if (compact) {
                MobileLayout(
                    stockList       = stockList,
                    sectors         = sectors,
                    user            = user,
                    state           = state,
                    sync            = sync,
                    onReset         = onReset,
                    onSignOut       = onSignOut,
                    selectedSymbol  = selectedSymbol,
                    onSelectSymbol  = { selectedSymbol = it },
                    quantityText    = quantityText,
                    onQuantityChange= { quantityText = it },
                    message         = message,
                    messageIsError  = messageIsError,
                    onMessage       = { t, e -> message = t; messageIsError = e },
                    currentSection  = currentSection,
                    onSectionChange = { currentSection = it },
                    sectorFilter    = sectorFilter,
                    onSectorFilterChange = { sectorFilter = it },
                    isPreview       = isPreview,
                )
            } else {
                DesktopLayout(
                    stockList       = stockList,
                    sectors         = sectors,
                    user            = user,
                    state           = state,
                    sync            = sync,
                    onReset         = onReset,
                    onSignOut       = onSignOut,
                    selectedSymbol  = selectedSymbol,
                    onSelectSymbol  = { selectedSymbol = it },
                    quantityText    = quantityText,
                    onQuantityChange= { quantityText = it },
                    message         = message,
                    messageIsError  = messageIsError,
                    onMessage       = { t, e -> message = t; messageIsError = e },
                    currentSection  = currentSection,
                    onSectionChange = { currentSection = it },
                    sectorFilter    = sectorFilter,
                    onSectorFilterChange = { sectorFilter = it },
                    isPreview       = isPreview,
                )
            }
        }
    }
}

// ── Desktop layout ───────────────────────────────────────────────────────────
@Composable
private fun DesktopLayout(
    stockList        : List<Stock>,
    sectors          : List<String>,
    user             : User,
    state            : UiState,
    sync             : () -> Unit,
    onReset          : () -> Unit,
    onSignOut        : () -> Unit,
    selectedSymbol   : String,
    onSelectSymbol   : (String) -> Unit,
    quantityText     : String,
    onQuantityChange : (String) -> Unit,
    message          : String,
    messageIsError   : Boolean,
    onMessage        : (String, Boolean) -> Unit,
    currentSection   : Section,
    onSectionChange  : (Section) -> Unit,
    sectorFilter     : String,
    onSectorFilterChange: (String) -> Unit,
    isPreview        : Boolean,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1.5f).fillMaxSize()) {
            TopTabs(currentSection, onSectionChange)
            Spacer(Modifier.height(16.dp))

            when (currentSection) {
                Section.Market -> {
                    SectorFilter(sectors, sectorFilter, onSectorFilterChange)
                    Spacer(Modifier.height(12.dp))
                    val filtered = if (sectorFilter == "All") stockList
                    else stockList.filter { it.sector == sectorFilter }
                    MarketList(
                        stocks         = filtered,
                        prices         = state.prices,
                        selectedSymbol = selectedSymbol,
                        watchlist      = state.watchlist,
                        onSelect       = onSelectSymbol,
                        onWatchlistToggle = { sym ->
                            if (!isPreview) {
                                user.toggleWatchlist(sym)
                                sync()
                            }
                        }
                    )
                }
                Section.Watchlist -> {
                    val filtered = stockList.filter { it.symbol in state.watchlist }
                    MarketList(
                        stocks         = filtered,
                        prices         = state.prices,
                        selectedSymbol = selectedSymbol,
                        watchlist      = state.watchlist,
                        onSelect       = onSelectSymbol,
                        onWatchlistToggle = { sym ->
                            if (!isPreview) {
                                user.toggleWatchlist(sym)
                                sync()
                            }
                        }
                    )
                }
                Section.Portfolio -> {
                    PortfolioView(
                        stockList = stockList,
                        state = state,
                        onSelect = onSelectSymbol
                    )
                }
                Section.History -> {
                    HistoryList(state.transactions)
                }
                Section.Settings -> {
                    SettingsView(onReset = onReset, onSignOut = onSignOut)
                }
            }
        }

        Spacer(Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            val stock = stockList.find { it.symbol == selectedSymbol }
            if (stock != null) {
                StockDetailCard(
                    stock          = stock,
                    currentPrice   = state.prices[stock.symbol] ?: stock.price,
                    portfolioCount = state.portfolio[stock.symbol] ?: 0,
                    avgPrice       = state.avgPrices[stock.symbol] ?: 0.0,
                    quantityText   = quantityText,
                    onQuantityChange = onQuantityChange,
                    message        = message,
                    messageIsError = messageIsError,
                    onBuy          = { qty ->
                        if (!isPreview) {
                            if (user.buyStock(stock, qty)) {
                                onMessage("Bought $qty ${stock.symbol}", false)
                                sync()
                            } else {
                                onMessage("Insufficient funds", true)
                            }
                        }
                    },
                    onSell         = { qty ->
                        if (!isPreview) {
                            if (user.sellStock(stock, qty)) {
                                onMessage("Sold $qty ${stock.symbol}", false)
                                sync()
                            } else {
                                onMessage("Not enough shares", true)
                            }
                        }
                    }
                )
            }
        }
    }
}

// ── Mobile layout ────────────────────────────────────────────────────────────
@Composable
private fun MobileLayout(
    stockList        : List<Stock>,
    sectors          : List<String>,
    user             : User,
    state            : UiState,
    sync             : () -> Unit,
    onReset          : () -> Unit,
    onSignOut        : () -> Unit,
    selectedSymbol   : String,
    onSelectSymbol   : (String) -> Unit,
    quantityText     : String,
    onQuantityChange : (String) -> Unit,
    message          : String,
    messageIsError   : Boolean,
    onMessage        : (String, Boolean) -> Unit,
    currentSection   : Section,
    onSectionChange  : (Section) -> Unit,
    sectorFilter     : String,
    onSectorFilterChange: (String) -> Unit,
    isPreview        : Boolean,
) {
    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopTabs(currentSection, onSectionChange)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (currentSection) {
                Section.Market -> {
                    Column {
                        SectorFilter(sectors, sectorFilter, onSectorFilterChange)
                        Spacer(Modifier.height(12.dp))
                        val filtered = if (sectorFilter == "All") stockList
                        else stockList.filter { it.sector == sectorFilter }
                        MarketList(
                            stocks         = filtered,
                            prices         = state.prices,
                            selectedSymbol = selectedSymbol,
                            watchlist      = state.watchlist,
                            onSelect       = { onSelectSymbol(it); showSheet = true },
                            onWatchlistToggle = { sym ->
                                if (!isPreview) {
                                    user.toggleWatchlist(sym)
                                    sync()
                                }
                            }
                        )
                    }
                }
                Section.Watchlist -> {
                    val filtered = stockList.filter { it.symbol in state.watchlist }
                    MarketList(
                        stocks         = filtered,
                        prices         = state.prices,
                        selectedSymbol = selectedSymbol,
                        watchlist      = state.watchlist,
                        onSelect       = { onSelectSymbol(it); showSheet = true },
                        onWatchlistToggle = { sym ->
                            if (!isPreview) {
                                user.toggleWatchlist(sym)
                                sync()
                            }
                        }
                    )
                }
                Section.Portfolio -> {
                    PortfolioView(
                        stockList = stockList,
                        state = state,
                        onSelect = { onSelectSymbol(it); showSheet = true }
                    )
                }
                Section.History -> {
                    HistoryList(state.transactions)
                }
                Section.Settings -> {
                    SettingsView(onReset = onReset, onSignOut = onSignOut)
                }
            }
        }

        AnimatedVisibility(
            visible = showSheet,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val stock = stockList.find { it.symbol == selectedSymbol }
            if (stock != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(Surface2)
                            .padding(24.dp)
                            .clickable(enabled = false) {}
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trade", color = Text1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Text2,
                                modifier = Modifier.clickable { showSheet = false }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        StockDetailCard(
                            stock          = stock,
                            currentPrice   = state.prices[stock.symbol] ?: stock.price,
                            portfolioCount = state.portfolio[stock.symbol] ?: 0,
                            avgPrice       = state.avgPrices[stock.symbol] ?: 0.0,
                            quantityText   = quantityText,
                            onQuantityChange = onQuantityChange,
                            message        = message,
                            messageIsError = messageIsError,
                            onBuy          = { qty ->
                                if (!isPreview) {
                                    if (user.buyStock(stock, qty)) {
                                        onMessage("Bought $qty ${stock.symbol}", false)
                                        sync()
                                    } else {
                                        onMessage("Insufficient funds", true)
                                    }
                                }
                            },
                            onSell         = { qty ->
                                if (!isPreview) {
                                    if (user.sellStock(stock, qty)) {
                                        onMessage("Sold $qty ${stock.symbol}", false)
                                        sync()
                                    } else {
                                        onMessage("Not enough shares", true)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Components ──────────────────────────────────────────────────────────────
@Composable
private fun HeaderBar(balance: Double, netWorth: Double, pnl: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Surface2, Surface2.copy(alpha = 0.7f))))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Cash Balance", color = Text2, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(formatMoney(balance), color = Text1, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Net Worth", color = Text2, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(formatMoney(netWorth), color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (pnl >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (pnl >= 0) Good else Bad,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatSignedMoney(pnl),
                    color = if (pnl >= 0) Good else Bad,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TopTabs(current: Section, onSelect: (Section) -> Unit) {
    TabRow(
        selectedTabIndex = current.ordinal,
        backgroundColor = Color.Transparent,
        contentColor = Accent,
        divider = {},
        indicator = {}
    ) {
        Section.values().forEach { section ->
            val selected = current == section
            Tab(
                selected = selected,
                onClick = { onSelect(section) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            section.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) Accent else Text2
                        )
                        Text(
                            section.name,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Accent else Text2
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SectorFilter(sectors: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val list = listOf("All") + sectors
        list.forEach { s ->
            val isSel = s == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSel) Accent else Surface2)
                    .clickable { onSelect(s) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(s, color = if (isSel) Background else Text1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MarketList(
    stocks            : List<Stock>,
    prices            : Map<String, Double>,
    selectedSymbol    : String,
    watchlist         : List<String>,
    onSelect          : (String) -> Unit,
    onWatchlistToggle : (String) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(stocks) { stock ->
            val price = prices[stock.symbol] ?: stock.price
            val isSelected = stock.symbol == selectedSymbol
            val inWatchlist = stock.symbol in watchlist

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Surface2 else Surface2.copy(alpha = 0.3f))
                    .clickable { onSelect(stock.symbol) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(sectorsColor[stock.sector]?.copy(alpha = 0.2f) ?: Border),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stock.symbol.take(1),
                        color = sectorsColor[stock.sector] ?: Text1,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(stock.symbol, color = Text1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stock.name, color = Text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(price), color = Text1, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Icon(
                        if (inWatchlist) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = "Watchlist",
                        tint = if (inWatchlist) Color(0xFFFCD34D) else Text2.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onWatchlistToggle(stock.symbol) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioView(
    stockList: List<Stock>,
    state: UiState,
    onSelect: (String) -> Unit
) {
    val myStocks = stockList.filter { state.portfolio.containsKey(it.symbol) }

    if (myStocks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = Text2, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Your portfolio is empty", color = Text2, fontSize = 16.sp)
                Text("Start trading to see your assets", color = Text2.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(myStocks) { stock ->
                val qty = state.portfolio[stock.symbol] ?: 0
                val avg = state.avgPrices[stock.symbol] ?: 0.0
                val curr = state.prices[stock.symbol] ?: stock.price
                val pnl = (curr - avg) * qty

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface2)
                        .clickable { onSelect(stock.symbol) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stock.symbol, color = Text1, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$qty Shares", color = Text2, fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(curr * qty), color = Text1, fontWeight = FontWeight.Bold)
                        Text(
                            formatSignedMoney(pnl),
                            color = if (pnl >= 0) Good else Bad,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDetailCard(
    stock          : Stock,
    currentPrice   : Double,
    portfolioCount : Int,
    avgPrice       : Double,
    quantityText   : String,
    onQuantityChange : (String) -> Unit,
    message        : String,
    messageIsError : Boolean,
    onBuy          : (Int) -> Unit,
    onSell         : (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stock.symbol, color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(stock.sector, color = sectorsColor[stock.sector] ?: Text2, fontSize = 12.sp)
        }
        Text(stock.name, color = Text1, fontSize = 28.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Market Price", color = Text2, fontSize = 12.sp)
                Text(formatMoney(currentPrice), color = Text1, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (portfolioCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Your Holdings", color = Text2, fontSize = 12.sp)
                    Text("$portfolioCount Shares", color = Text1, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val pnl = (currentPrice - avgPrice) * portfolioCount
                    Text(formatSignedMoney(pnl), color = if (pnl >= 0) Good else Bad, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = { if (it.all { c -> c.isDigit() }) onQuantityChange(it) },
            label = { Text("Quantity to trade", color = Text2) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Text1,
                cursorColor = Accent,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border,
                backgroundColor = Surface1.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val qty = quantityText.toIntOrNull() ?: 0
            Button(
                onClick = { onBuy(qty) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Good),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                enabled = qty > 0
            ) {
                Text("BUY", color = Color(0xFF064E3B), fontWeight = FontWeight.Black)
            }
            Button(
                onClick = { onSell(qty) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Bad),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                enabled = qty > 0 && portfolioCount >= qty
            ) {
                Text("SELL", color = Color(0xFF7F1D1D), fontWeight = FontWeight.Black)
            }
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (messageIsError) Bad.copy(alpha = 0.1f) else Good.copy(alpha = 0.1f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message,
                    color = if (messageIsError) Bad else Good,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HistoryList(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No trade history", color = Text2)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(transactions.reversed()) { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface2.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (tx.type == Transaction.Type.BUY) Good else Bad)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(tx.symbol, color = Text1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(formatTime(tx.timestamp), color = Text2, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val sign = if (tx.type == Transaction.Type.BUY) "-" else "+"
                        val total = tx.quantity * tx.pricePerShare
                        Text("$sign${formatMoney(total)}", color = Text1, fontWeight = FontWeight.Bold)
                        Text("${tx.quantity} @ ${formatMoney(tx.pricePerShare)}", color = Text2, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
@Composable
private fun SettingsView(onReset: () -> Unit, onSignOut: () -> Unit) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            backgroundColor = Surface2,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Reset Account?", color = Text1, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "This will wipe your entire portfolio, watchlist, transaction history, and restore your balance to \$1,000,000. This cannot be undone.",
                    color = Text2, fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetDialog = false; onReset() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Bad),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) { Text("Reset", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Text2)
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(listOf(Accent, Color(0xFF818CF8)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("StockFlow", color = Text1, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Next-Gen Trading Simulator", color = Text2, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Accent.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("v1.0.0 · Cloud Synced", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(48.dp))

        // Sign Out button
        Button(
            onClick = onSignOut,
            modifier = Modifier.width(240.dp).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(Accent, Color(0xFF818CF8))),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Reset account (destructive)
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.width(240.dp).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Bad.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Bad.copy(alpha = 0.08f))
        ) {
            Text("Reset Account", color = Bad, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Reset wipes portfolio & restores \$1,000,000",
            color = Text2.copy(alpha = 0.6f), fontSize = 11.sp
        )
    }
}

@Preview
@Composable
fun PreviewAppMobile() {
    StockFlowApp()
}

@Preview
@Composable
fun PreviewAppDesktop() {
    StockFlowApp()
}
