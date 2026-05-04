package com.stock.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stock.api.SupabaseManager
import com.stock.model.Market
import com.stock.model.Stock
import com.stock.model.Transaction
import com.stock.model.UiState
import com.stock.model.User
import com.stock.storage.DataStore
import com.stock.util.formatMoney
import com.stock.util.formatSignedMoney
import com.stock.util.formatTime
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

// ── Auth state ───────────────────────────────────────────────────────────────
private enum class AuthState { SPLASH, CHECKING, LOGGED_OUT, LOGGED_IN, WAITING_FOR_AUTH }

// ── Root composable ─────────────────────────────────────────────────────────
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StockFlowApp() {
    val isPreview = LocalInspectionMode.current
    var authState by remember { mutableStateOf(if (isPreview) AuthState.LOGGED_IN else AuthState.SPLASH) }
    var isLoading by remember { mutableStateOf(false) }
    val market = remember { if (isPreview) null else Market() }
    var user by remember { mutableStateOf<User?>(if (isPreview) User(1_000_000.0) else null) }

    val uiStateFlow = remember { MutableStateFlow(if (isPreview) fakeUiState() else UiState.empty()) }
    val uiState by uiStateFlow.collectAsState()
    val scope = rememberCoroutineScope()

    // Splash Screen logic
    LaunchedEffect(Unit) {
        if (!isPreview) {
            delay(2000) // Show splash for 2 seconds
            authState = AuthState.CHECKING
            val hasSession = SupabaseManager.restoreSession()
            authState = if (hasSession) AuthState.LOGGED_IN else AuthState.LOGGED_OUT
        }
    }

    // ── Listen to Supabase session status changes ──────────────────────────
    // This is critical for Android Google OAuth: after the deep-link redirect
    // handleDeeplinks() updates the Supabase session internally, but the
    // app's authState is never transitioned. This collector detects when
    // the session becomes Authenticated and transitions the UI.
    LaunchedEffect(Unit) {
        if (!isPreview) {
            SupabaseManager.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        // Only auto-transition if we're currently waiting for auth
                        if (authState == AuthState.WAITING_FOR_AUTH) {
                            authState = AuthState.LOGGED_IN
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        // If the session expires or user is signed out externally
                        if (authState == AuthState.LOGGED_IN || authState == AuthState.WAITING_FOR_AUTH) {
                            authState = AuthState.LOGGED_OUT
                        }
                    }
                    else -> { /* LoadingFromStorage, RefreshFailure — handled elsewhere */ }
                }
            }
        }
    }

    LaunchedEffect(authState) {
        if (!isPreview && authState == AuthState.LOGGED_IN) {
            isLoading = true
            try {
                if (!SupabaseManager.isLoggedIn()) {
                    authState = AuthState.LOGGED_OUT
                    isLoading = false
                    return@LaunchedEffect
                }

                user = DataStore.load(1_000_000.0)
                uiStateFlow.value = UiState.from(user!!, market!!)

                val cloudUser = SupabaseManager.loadUser(1_000_000.0)
                user = cloudUser
                DataStore.save(cloudUser)
                uiStateFlow.value = UiState.from(cloudUser, market!!)

                market.setOnUpdateCallback {
                    val u = user ?: return@setOnUpdateCallback
                    uiStateFlow.value = UiState.from(u, market)
                }
                market.startSimulation()
            } catch (e: Exception) {
                if (user == null) {
                    user = User(1_000_000.0)
                    market?.let { uiStateFlow.value = UiState.from(user!!, it) }
                }
            }
            isLoading = false
        }
    }

    if (!isPreview && market != null) {
        DisposableEffect(Unit) {
            onDispose { market.stopSimulation() }
        }
    }

    val sync: () -> Unit = {
        if (!isPreview) {
            scope.launch {
                val u = user ?: return@launch
                DataStore.save(u)
                SupabaseManager.saveUser(u)
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
                SupabaseManager.saveUser(fresh)
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

    MaterialTheme {
        Surface(color = SFColor.Bg, modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = authState,
                transitionSpec = {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                }
            ) { state ->
                when (state) {
                    AuthState.SPLASH -> SplashScreen()
                    AuthState.CHECKING -> LoadingScreen("Checking session...")
                    AuthState.WAITING_FOR_AUTH -> LoadingScreen("Waiting for sign-in...")
                    AuthState.LOGGED_OUT -> LoginScreen(
                        onLoginSuccess = { authState = AuthState.LOGGED_IN },
                        onWaitingForAuth = { authState = AuthState.WAITING_FOR_AUTH },
                        onAuthFailed = { authState = AuthState.LOGGED_OUT }
                    )
                    AuthState.LOGGED_IN -> {
                        if (isLoading) {
                            LoadingScreen("Syncing your portfolio...")
                        } else {
                            val currentUser = user ?: return@AnimatedContent
                            AppContent(
                                market = market,
                                user = currentUser,
                                state = uiState,
                                sync = sync,
                                onReset = reset,
                                onSignOut = signOut,
                                isPreview = isPreview,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    // Animated entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val logoScale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "logoScale"
    )
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.10f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SFGradient.bgRadial),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + scaleIn(tween(800), initialScale = 0.8f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Glow behind
                    Box(
                        modifier = Modifier
                            .size((120 * logoScale).dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(SFColor.Accent.copy(alpha = glowAlpha * 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .size((100 * logoScale).dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SFGradient.accentBrand),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("StockFlow", color = SFColor.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp)
                Spacer(Modifier.height(6.dp))
                Text("Trading Simulator", color = SFColor.TextSecondary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun LoadingScreen(msg: String) {
    val pulse = rememberInfiniteTransition(label = "loadingPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "dotAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SFColor.Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = SFColor.Accent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                msg,
                color = SFColor.TextSecondary.copy(alpha = dotAlpha),
                fontSize = 14.sp
            )
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
    var selectedSymbol by remember { mutableStateOf("AAPL") }
    var quantityText   by remember { mutableStateOf("1") }
    var message        by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf(Section.Market) }
    var sectorFilter   by remember { mutableStateOf("All") }
    var detailSymbol   by remember { mutableStateOf<String?>(null) }

    val stockList = if (isPreview) fakeStocks() else market!!.stocks
    val sectors = if (isPreview) {
        listOf("Technology", "Automotive", "Finance", "Healthcare", "Consumer", "Fintech")
    } else {
        market!!.sectors
    }

    if (detailSymbol != null) {
        StockDetailScreen(
            symbol           = detailSymbol!!,
            market           = market,
            user             = user,
            state            = state,
            quantityText     = quantityText,
            onQuantityChange = { quantityText = it },
            message          = message,
            messageIsError   = messageIsError,
            onMessage        = { t, e ->
                message = t
                messageIsError = e
            },
            sync             = sync,
            onBack           = {
                detailSymbol = null
                market?.focusedSymbol = null
            },
            isPreview        = isPreview,
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val compact = maxWidth < 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 0.dp else 16.dp)
                .then(if (compact) Modifier else Modifier.clip(RoundedCornerShape(24.dp)).background(SFColor.Surface1))
                .padding(if (compact) 0.dp else 16.dp)
        ) {
            if (compact) {
                Box(Modifier.padding(16.dp)) { HeaderBar(state.balance, state.netWorth, state.totalPnL) }
            } else {
                HeaderBar(state.balance, state.netWorth, state.totalPnL)
            }
            
            Spacer(Modifier.height(if (compact) 0.dp else 16.dp))

            Box(Modifier.weight(1f)) {
                if (compact) {
                    MobileLayout(
                        stockList            = stockList,
                        sectors              = sectors,
                        user                 = user,
                        state                = state,
                        sync                 = sync,
                        onReset              = onReset,
                        onSignOut            = onSignOut,
                        selectedSymbol       = selectedSymbol,
                        onSelectSymbol       = { sym ->
                            selectedSymbol = sym
                            detailSymbol = sym
                            market?.focusedSymbol = sym
                        },
                        onDismissSymbol      = {
                            market?.focusedSymbol = null
                            detailSymbol = null
                        },
                        quantityText         = quantityText,
                        onQuantityChange     = { quantityText = it },
                        message              = message,
                        messageIsError       = messageIsError,
                        onMessage            = { t, e ->
                            message = t
                            messageIsError = e
                        },
                        currentSection       = currentSection,
                        onSectionChange      = { currentSection = it },
                        sectorFilter         = sectorFilter,
                        onSectorFilterChange = { sectorFilter = it },
                        isPreview            = isPreview,
                    )
                } else {
                    DesktopLayout(
                        stockList            = stockList,
                        sectors              = sectors,
                        user                 = user,
                        state                = state,
                        sync                 = sync,
                        onReset              = onReset,
                        onSignOut            = onSignOut,
                        selectedSymbol       = selectedSymbol,
                        onSelectSymbol       = { sym ->
                            selectedSymbol = sym
                            detailSymbol = sym
                            market?.focusedSymbol = sym
                        },
                        quantityText         = quantityText,
                        onQuantityChange     = { quantityText = it },
                        message              = message,
                        messageIsError       = messageIsError,
                        onMessage            = { t, e ->
                            message = t
                            messageIsError = e
                        },
                        currentSection       = currentSection,
                        onSectionChange      = { currentSection = it },
                        sectorFilter         = sectorFilter,
                        onSectorFilterChange = { sectorFilter = it },
                        isPreview            = isPreview,
                        focusedSymbol        = market?.focusedSymbol,
                    )
                }
            }
        }
    }
}

private enum class Section(val icon: ImageVector) {
    Market(Icons.Default.TrendingUp),
    Watchlist(Icons.Default.Star),
    Portfolio(Icons.Default.AccountBalanceWallet),
    History(Icons.Default.History),
    Settings(Icons.Default.Settings)
}

// ── Layouts ──────────────────────────────────────────────────────────────────

@Composable
private fun DesktopLayout(
    stockList            : List<Stock>,
    sectors              : List<String>,
    user                 : User,
    state                : UiState,
    sync                 : () -> Unit,
    onReset              : () -> Unit,
    onSignOut            : () -> Unit,
    selectedSymbol       : String,
    onSelectSymbol       : (String) -> Unit,
    quantityText         : String,
    onQuantityChange     : (String) -> Unit,
    message              : String,
    messageIsError       : Boolean,
    onMessage            : (String, Boolean) -> Unit,
    currentSection       : Section,
    onSectionChange      : (Section) -> Unit,
    sectorFilter         : String,
    onSectorFilterChange : (String) -> Unit,
    isPreview            : Boolean,
    focusedSymbol        : String?,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1.5f).fillMaxSize()) {
            TopTabs(currentSection, onSectionChange)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(targetState = currentSection) { section ->
                Column {
                    when (section) {
                        Section.Market -> {
                            SectorFilter(sectors, sectorFilter, onSectorFilterChange)
                            Spacer(Modifier.height(12.dp))
                            val filtered = if (sectorFilter == "All") stockList
                            else stockList.filter { it.sector == sectorFilter }
                            MarketList(filtered, state.prices, selectedSymbol, state.watchlist, focusedSymbol, onSelectSymbol) { sym ->
                                if (!isPreview) { user.toggleWatchlist(sym); sync() }
                            }
                        }
                        Section.Watchlist -> {
                            val filtered = stockList.filter { it.symbol in state.watchlist }
                            MarketList(filtered, state.prices, selectedSymbol, state.watchlist, focusedSymbol, onSelectSymbol) { sym ->
                                if (!isPreview) { user.toggleWatchlist(sym); sync() }
                            }
                        }
                        Section.Portfolio -> PortfolioView(stockList, state, onSelectSymbol)
                        Section.History -> HistoryList(state.transactions)
                        Section.Settings -> SettingsView(onReset, onSignOut)
                    }
                }
            }
        }

        Spacer(Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            val stock = stockList.find { it.symbol == selectedSymbol }
            if (stock != null) {
                StockDetailCard(
                    stock            = stock,
                    currentPrice     = state.prices[stock.symbol] ?: stock.price,
                    portfolioCount   = state.portfolio[stock.symbol] ?: 0,
                    avgPrice         = state.avgPrices[stock.symbol] ?: 0.0,
                    quantityText     = quantityText,
                    onQuantityChange = onQuantityChange,
                    message          = message,
                    messageIsError   = messageIsError,
                    isFocused        = focusedSymbol == stock.symbol,
                    onBuy            = { qty ->
                        if (!isPreview) {
                            if (user.buyStock(stock, qty)) { onMessage("Bought $qty ${stock.symbol}", false); sync() }
                            else onMessage("Insufficient funds", true)
                        }
                    },
                    onSell           = { qty ->
                        if (!isPreview) {
                            if (user.sellStock(stock, qty)) { onMessage("Sold $qty ${stock.symbol}", false); sync() }
                            else onMessage("Not enough shares", true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MobileLayout(
    stockList            : List<Stock>,
    sectors              : List<String>,
    user                 : User,
    state                : UiState,
    sync                 : () -> Unit,
    onReset              : () -> Unit,
    onSignOut            : () -> Unit,
    selectedSymbol       : String,
    onSelectSymbol       : (String) -> Unit,
    onDismissSymbol      : () -> Unit,
    quantityText         : String,
    onQuantityChange     : (String) -> Unit,
    message              : String,
    messageIsError       : Boolean,
    onMessage            : (String, Boolean) -> Unit,
    currentSection       : Section,
    onSectionChange      : (Section) -> Unit,
    sectorFilter         : String,
    onSectorFilterChange : (String) -> Unit,
    isPreview            : Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            AnimatedContent(targetState = currentSection) { section ->
                Column {
                    when (section) {
                        Section.Market -> {
                            SectorFilter(sectors, sectorFilter, onSectorFilterChange)
                            Spacer(Modifier.height(12.dp))
                            val filtered = if (sectorFilter == "All") stockList
                            else stockList.filter { it.sector == sectorFilter }
                            MarketList(filtered, state.prices, selectedSymbol, state.watchlist, null, onSelectSymbol) { sym ->
                                if (!isPreview) { user.toggleWatchlist(sym); sync() }
                            }
                        }
                        Section.Watchlist -> {
                            val filtered = stockList.filter { it.symbol in state.watchlist }
                            MarketList(filtered, state.prices, selectedSymbol, state.watchlist, null, onSelectSymbol) { sym ->
                                if (!isPreview) { user.toggleWatchlist(sym); sync() }
                            }
                        }
                        Section.Portfolio -> PortfolioView(stockList, state, onSelectSymbol)
                        Section.History -> HistoryList(state.transactions)
                        Section.Settings -> SettingsView(onReset, onSignOut)
                    }
                }
            }
        }
        
        BottomNavigation(
            backgroundColor = SFColor.Surface1,
            elevation = 8.dp,
            modifier = Modifier.height(64.dp)
        ) {
            Section.values().forEach { section ->
                val selected = currentSection == section
                BottomNavigationItem(
                    selected = selected,
                    onClick = { onSectionChange(section) },
                    icon = {
                        Icon(
                            section.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(section.name, fontSize = 10.sp) },
                    selectedContentColor = SFColor.Accent,
                    unselectedContentColor = SFColor.TextMuted
                )
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
            .background(SFGradient.headerBar)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Cash Balance", color = SFColor.TextSecondary, fontSize = 12.sp)
            Text(formatMoney(balance), color = SFColor.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Net Worth", color = SFColor.TextSecondary, fontSize = 12.sp)
            Text(formatMoney(netWorth), color = SFColor.Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (pnl >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null,
                    tint = if (pnl >= 0) SFColor.Gain else SFColor.Loss,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatSignedMoney(pnl),
                    color = if (pnl >= 0) SFColor.Gain else SFColor.Loss,
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
        contentColor = SFColor.Accent,
        divider = {},
        indicator = {}
    ) {
        Section.values().forEach { section ->
            val selected = current == section
            Tab(
                selected = selected,
                onClick  = { onSelect(section) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            section.icon,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) SFColor.Accent else SFColor.TextSecondary
                        )
                        Text(
                            section.name,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) SFColor.Accent else SFColor.TextSecondary
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
                    .background(if (isSel) SFColor.Accent else SFColor.Surface2)
                    .clickable { onSelect(s) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(s, color = if (isSel) SFColor.Bg else SFColor.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarketList(
    stocks            : List<Stock>,
    prices            : Map<String, Double>,
    selectedSymbol    : String,
    watchlist         : List<String>,
    focusedSymbol     : String?,
    onSelect          : (String) -> Unit,
    onWatchlistToggle : (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(stocks, key = { it.symbol }) { stock ->
            val price = prices[stock.symbol] ?: stock.price
            val isSelected = stock.symbol == selectedSymbol
            val inWatchlist = stock.symbol in watchlist
            val isFocused = focusedSymbol == null || focusedSymbol == stock.symbol

            val alpha by animateFloatAsState(if (isFocused) 1f else 0.3f)
            val scale by animateFloatAsState(if (isFocused) 1f else 0.98f)

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .animateItemPlacement()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) SFColor.Surface2 else SFColor.SurfaceCard)
                        .clickable { onSelect(stock.symbol) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SFColor.sectorColors[stock.sector]?.copy(alpha = 0.15f) ?: SFColor.Border),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stock.symbol.take(1),
                            color = SFColor.sectorColors[stock.sector] ?: SFColor.TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stock.symbol, color = SFColor.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (focusedSymbol == stock.symbol) {
                                Spacer(Modifier.width(8.dp))
                                LiveBadge()
                            }
                        }
                        Text(stock.name, color = SFColor.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(price), color = SFColor.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        IconButton(
                            onClick = { onWatchlistToggle(stock.symbol) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = if (inWatchlist) Color(0xFFFCD34D) else SFColor.TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(SFColor.Gain.copy(alpha = alpha)))
        Spacer(Modifier.width(4.dp))
        Text("LIVE", color = SFColor.Gain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PortfolioView(stockList: List<Stock>, state: UiState, onSelect: (String) -> Unit) {
    val myStocks = stockList.filter { state.portfolio.containsKey(it.symbol) }

    if (myStocks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShowChart, null, tint = SFColor.TextMuted, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Your portfolio is empty", color = SFColor.TextSecondary)
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
                        .background(SFColor.SurfaceCard)
                        .clickable { onSelect(stock.symbol) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stock.symbol, color = SFColor.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$qty Shares", color = SFColor.TextSecondary, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(curr * qty), color = SFColor.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            formatSignedMoney(pnl),
                            color = if (pnl >= 0) SFColor.Gain else SFColor.Loss,
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
fun StockDetailCard(
    stock: Stock,
    currentPrice: Double,
    portfolioCount: Int,
    avgPrice: Double,
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    message: String,
    messageIsError: Boolean,
    isFocused: Boolean,
    onBuy: (Int) -> Unit,
    onSell: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SFColor.SurfaceCard)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stock.symbol, color = SFColor.Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(stock.sector, color = SFColor.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (isFocused) LiveBadge()
        }
        Text(stock.name, color = SFColor.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Market Price", color = SFColor.TextSecondary, fontSize = 12.sp)
                Text(formatMoney(currentPrice), color = SFColor.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (portfolioCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Your Holdings", color = SFColor.TextSecondary, fontSize = 12.sp)
                    Text("$portfolioCount Shares", color = SFColor.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val pnl = (currentPrice - avgPrice) * portfolioCount
                    Text(formatSignedMoney(pnl), color = if (pnl >= 0) SFColor.Gain else SFColor.Loss, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = { if (it.all { c -> c.isDigit() }) onQuantityChange(it) },
            label = { Text("Quantity", color = SFColor.TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = SFColor.TextPrimary,
                cursorColor = SFColor.Accent,
                focusedBorderColor = SFColor.Accent,
                unfocusedBorderColor = SFColor.Border,
                backgroundColor = SFColor.Surface1
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
                colors = ButtonDefaults.buttonColors(backgroundColor = SFColor.Gain),
                shape = RoundedCornerShape(16.dp),
                enabled = qty > 0
            ) { Text("BUY", color = Color.White, fontWeight = FontWeight.Black) }
            
            Button(
                onClick = { onSell(qty) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = SFColor.Loss),
                shape = RoundedCornerShape(16.dp),
                enabled = qty > 0 && portfolioCount >= qty
            ) { Text("SELL", color = Color.White, fontWeight = FontWeight.Black) }
        }

        AnimatedVisibility(
            visible = message.isNotEmpty(),
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(message, color = if (messageIsError) SFColor.Loss else SFColor.Gain, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HistoryList(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No trade history", color = SFColor.TextMuted)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(transactions.reversed()) { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SFColor.SurfaceCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (tx.type == Transaction.Type.BUY) SFColor.Gain else SFColor.Loss))
                            Spacer(Modifier.width(10.dp))
                            Text(tx.symbol, color = SFColor.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Text(formatTime(tx.timestamp), color = SFColor.TextMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val sign = if (tx.type == Transaction.Type.BUY) "-" else "+"
                        Text("$sign${formatMoney(tx.quantity * tx.pricePerShare)}", color = SFColor.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${tx.quantity} @ ${formatMoney(tx.pricePerShare)}", color = SFColor.TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsView(onReset: () -> Unit, onSignOut: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val pulse = rememberInfiniteTransition(label = "settingsPulse")
    val logoScale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "settingsLogoScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 30 },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size((80 * logoScale).dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SFGradient.accentBrand),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("StockFlow", color = SFColor.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("v1.0.0", color = SFColor.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier.width(220.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = SFColor.Surface2),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ExitToApp, null, tint = SFColor.TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = SFColor.TextPrimary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onReset) {
                Icon(Icons.Default.RestartAlt, null, tint = SFColor.Loss, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Account", color = SFColor.Loss)
            }
        }
    }
}

// ── Fakes ───────────────────────────────────────────────────────────────────
private fun fakeStocks() = listOf(
    Stock("AAPL", "Apple Inc.", "Technology", 182.50),
    Stock("TSLA", "Tesla Motors", "Automotive", 245.30),
)
private fun fakeUiState() = UiState(
    balance = 980000.0, netWorth = 1050000.0, totalPnL = 50000.0,
    prices = mapOf("AAPL" to 182.5, "TSLA" to 245.3),
    watchlist = listOf("AAPL"), portfolio = mapOf("AAPL" to 10),
    avgPrices = mapOf("AAPL" to 170.0), transactions = emptyList()
)
