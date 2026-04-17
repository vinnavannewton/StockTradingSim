package com.stock.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
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

// ── Colours ────────────────────────────────────────────────────────────────
private val Background = Color(0xFF0B0F17)
private val Surface1   = Color(0xFF111827)
private val Surface2   = Color(0xFF1F2937)
private val Accent     = Color(0xFF60A5FA)
private val Good       = Color(0xFF34D399)
private val Bad        = Color(0xFFF87171)
private val Text1      = Color(0xFFF9FAFB)
private val Text2      = Color(0xFF9CA3AF)
private val Border     = Color(0xFF334155)

private val sectorsColor = mapOf(
    "Technology" to Accent,
    "Automotive" to Color(0xFFF59E0B),
    "Finance"    to Good,
    "Healthcare" to Color(0xFFF472B6),
    "Consumer"   to Color(0xFFA78BFA),
    "Fintech"    to Color(0xFF22D3EE),
)

private enum class Section { Market, Watchlist, History, Settings }

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

// ── Root composable ─────────────────────────────────────────────────────────
@Composable
fun StockFlowApp() {
    val isPreview = LocalInspectionMode.current          // TRUE inside Android Studio preview

    val market = remember { if (isPreview) null else Market() }
    var user   by remember { mutableStateOf(if (isPreview) User(1_000_000.0) else DataStore.load(1_000_000.0)) }

    val uiStateFlow = remember {
        MutableStateFlow(
            if (isPreview) fakeUiState()
            else UiState.from(user, market!!)
        )
    }
    val uiState by uiStateFlow.collectAsState()

    // Only run the real simulation when NOT in preview
    if (!isPreview) {
        DisposableEffect(Unit) {
            market!!.setOnUpdateCallback {
                uiStateFlow.value = UiState.from(user, market)
            }
            market.startSimulation()
            onDispose { market.stopSimulation() }
        }
    }

    val sync: () -> Unit = {
        if (!isPreview) {
            DataStore.save(user)
            uiStateFlow.value = UiState.from(user, market!!)
        }
    }

    val reset: () -> Unit = {
        if (!isPreview) {
            DataStore.reset()
            user = User(1_000_000.0)
            DataStore.save(user)
            uiStateFlow.value = UiState.from(user, market!!)
        }
    }

    MaterialTheme {
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            AppContent(
                market        = market,
                user          = user,
                state         = uiState,
                sync          = sync,
                onReset       = reset,
                isPreview     = isPreview,
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
    isPreview : Boolean = false,
) {
    var selectedSymbol  by remember { mutableStateOf("AAPL") }
    var quantityText    by remember { mutableStateOf("1") }
    var message         by remember { mutableStateOf("") }
    var messageIsError  by remember { mutableStateOf(false) }
    var currentSection  by remember { mutableStateOf(Section.Market) }
    var sectorFilter    by remember { mutableStateOf("All") }

    // Fake stocks for preview; real stocks from market otherwise
    val stockList = if (isPreview) fakeStocks() else market!!.stocks
    val sectors   = if (isPreview) listOf("Technology","Automotive","Finance","Healthcare","Consumer","Fintech")
    else market!!.sectors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        val compact = maxWidth < 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface1)
                .padding(14.dp)
        ) {
            HeaderBar(state.balance, state.netWorth, state.totalPnL)
            Spacer(Modifier.height(12.dp))

            if (compact) {
                MobileLayout(
                    stockList       = stockList,
                    sectors         = sectors,
                    market          = market,
                    user            = user,
                    state           = state,
                    sync            = sync,
                    onReset         = onReset,
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
                    market          = market,
                    user            = user,
                    state           = state,
                    sync            = sync,
                    onReset         = onReset,
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
    market           : Market?,
    user             : User,
    state            : UiState,
    sync             : () -> Unit,
    onReset          : () -> Unit,
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
        Column(modifier = Modifier.weight(1.4f).fillMaxSize()) {
            TopTabs(currentSection, onSectionChange)
            Spacer(Modifier.height(8.dp))

            when (currentSection) {
                Section.Market -> {
                    SectorFilter(sectors, sectorFilter, onSectorFilterChange)
                    Spacer(Modifier.height(8.dp))
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
                Section.History -> {
                    HistoryList(state.transactions)
                }
                Section.Settings -> {
                    SettingsView(onReset)
                }
            }
        }

        Spacer(Modifier.width(16.dp))

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
    market           : Market?,
    user             : User,
    state            : UiState,
    sync             : () -> Unit,
    onReset          : () -> Unit,
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
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (currentSection) {
                Section.Market -> {
                    Column {
                        SectorFilter(sectors, sectorFilter, onSectorFilterChange)
                        Spacer(Modifier.height(8.dp))
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
                Section.History -> {
                    HistoryList(state.transactions)
                }
                Section.Settings -> {
                    SettingsView(onReset)
                }
            }
        }

        if (showSheet) {
            val stock = stockList.find { it.symbol == selectedSymbol }
            if (stock != null) {
                // Simple overlay for mobile "sheet"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Surface2)
                            .padding(20.dp)
                            .clickable(enabled = false) {}
                    ) {
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
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showSheet = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Surface1)
                        ) {
                            Text("Close", color = Text1)
                        }
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
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Available Balance", color = Text2, fontSize = 12.sp)
            Text(formatMoney(balance), color = Text1, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Net Worth", color = Text2, fontSize = 12.sp)
            Text(formatMoney(netWorth), color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = formatSignedMoney(pnl),
                color = if (pnl >= 0) Good else Bad,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TopTabs(current: Section, onSelect: (Section) -> Unit) {
    TabRow(
        selectedTabIndex = current.ordinal,
        backgroundColor = Color.Transparent,
        contentColor = Accent,
        divider = {}
    ) {
        Section.values().forEach { section ->
            Tab(
                selected = current == section,
                onClick = { onSelect(section) },
                text = { Text(section.name, fontSize = 13.sp) }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val list = listOf("All") + sectors
        list.forEach { s ->
            val isSel = s == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (isSel) Accent else Surface2)
                    .clickable { onSelect(s) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(s, color = if (isSel) Background else Text1, fontSize = 12.sp)
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stocks) { stock ->
            val price = prices[stock.symbol] ?: stock.price
            val isSelected = stock.symbol == selectedSymbol
            val inWatchlist = stock.symbol in watchlist

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Surface2 else Color.Transparent)
                    .clickable { onSelect(stock.symbol) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(sectorsColor[stock.sector] ?: Border),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stock.symbol.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(stock.symbol, color = Text1, fontWeight = FontWeight.Bold)
                    Text(stock.name, color = Text2, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(price), color = Text1, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (inWatchlist) "★ Watchlist" else "☆ Add",
                        color = if (inWatchlist) Color(0xFFFCD34D) else Text2,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable { onWatchlistToggle(stock.symbol) }
                    )
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
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .padding(16.dp)
    ) {
        Text(stock.symbol, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(stock.name, color = Text1, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(stock.sector, color = sectorsColor[stock.sector] ?: Text2, fontSize = 12.sp)

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Current Price", color = Text2, fontSize = 12.sp)
                Text(formatMoney(currentPrice), color = Text1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (portfolioCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Your Position", color = Text2, fontSize = 12.sp)
                    Text("$portfolioCount Shares", color = Text1, fontWeight = FontWeight.Bold)
                    val pnl = (currentPrice - avgPrice) * portfolioCount
                    Text(formatSignedMoney(pnl), color = if (pnl >= 0) Good else Bad, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Divider(color = Border)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = { if (it.all { c -> c.isDigit() }) onQuantityChange(it) },
            label = { Text("Quantity", color = Text2) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Text1,
                cursorColor = Accent,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border
            ),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val qty = quantityText.toIntOrNull() ?: 0
            Button(
                onClick = { onBuy(qty) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Good),
                shape = RoundedCornerShape(12.dp),
                enabled = qty > 0
            ) {
                Text("BUY", color = Background, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSell(qty) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Bad),
                shape = RoundedCornerShape(12.dp),
                enabled = qty > 0 && portfolioCount >= qty
            ) {
                Text("SELL", color = Background, fontWeight = FontWeight.Bold)
            }
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = if (messageIsError) Bad else Good,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HistoryList(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet", color = Text2)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(transactions.reversed()) { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = if (tx.type == Transaction.Type.BUY) Good else Bad
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tx.symbol, color = Text1, fontWeight = FontWeight.Bold)
                        }
                        Text(formatTime(tx.timestamp), color = Text2, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val sign = if (tx.type == Transaction.Type.BUY) "-" else "+"
                        val total = tx.quantity * tx.pricePerShare
                        Text("$sign${formatMoney(total)}", color = Text1, fontWeight = FontWeight.SemiBold)
                        Text("${tx.quantity} @ ${formatMoney(tx.pricePerShare)}", color = Text2, fontSize = 11.sp)
                    }
                }
                Divider(color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SettingsView(onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Trading Simulator v1.0", color = Text1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Kotlin Multiplatform + Compose", color = Text2, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onReset) {
            Text("Reset All Data", color = Bad)
        }
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

@Preview
@Composable
fun PreviewHeader() {
    MaterialTheme {
        Box(Modifier.background(Background).padding(16.dp)) {
            HeaderBar(balance = 50000.0, netWorth = 120000.0, pnl = 1500.0)
        }
    }
}
