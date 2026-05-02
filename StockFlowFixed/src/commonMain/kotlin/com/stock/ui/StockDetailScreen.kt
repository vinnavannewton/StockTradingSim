package com.stock.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stock.model.Market
import com.stock.model.Stock
import com.stock.model.UiState
import com.stock.model.User
import com.stock.util.formatMoney
import com.stock.util.formatSignedMoney

// ── Colour aliases (same as App.kt) ─────────────────────────────────────────
private val Bg       = Color(0xFF07090F)
private val Surf1    = Color(0xFF0F172A)
private val Surf2    = Color(0xFF1E293B)
private val Acc      = Color(0xFF38BDF8)
private val Gd       = Color(0xFF4ADE80)
private val Bd       = Color(0xFFF87171)
private val Tx1      = Color(0xFFF8FAFC)
private val Tx2      = Color(0xFF94A3B8)
private val Bdr      = Color(0xFF334155)

private val sectorColors = mapOf(
    "Technology" to Color(0xFF38BDF8),
                                 "Automotive" to Color(0xFFFB923C),
                                 "Finance"    to Color(0xFF4ADE80),
                                 "Healthcare" to Color(0xFFF472B6),
                                 "Consumer"   to Color(0xFFA78BFA),
                                 "Fintech"    to Color(0xFF22D3EE),
)

// ── Time range filter ────────────────────────────────────────────────────────
private enum class TimeRange(val label: String, val windowMs: Long) {
    ONE_MIN("1M",   60_000L),
    FIVE_MIN("5M",  300_000L),
    FIFTEEN_MIN("15M", 900_000L),
    ONE_HOUR("1H",  3_600_000L),
    ALL("ALL",      Long.MAX_VALUE),
}

// ── Main composable ──────────────────────────────────────────────────────────
@Composable
fun StockDetailScreen(
    symbol           : String,
    market           : Market?,
    user             : User,
    state            : UiState,
    quantityText     : String,
    onQuantityChange : (String) -> Unit,
                      message          : String,
                      messageIsError   : Boolean,
                      onMessage        : (String, Boolean) -> Unit,
                      sync             : () -> Unit,
                      onBack           : () -> Unit,
                      isPreview        : Boolean,
) {
    val stockList = if (isPreview) fakeDetailStocks() else market!!.stocks
    val stock = stockList.find { it.symbol == symbol } ?: return

    val currentPrice   = state.prices[symbol] ?: stock.price
    val portfolioCount = state.portfolio[symbol] ?: 0
    val avgPrice       = state.avgPrices[symbol] ?: 0.0

    var selectedRange by remember { mutableStateOf(TimeRange.FIVE_MIN) }

    // Snapshot of history filtered to time window
    val now     = System.currentTimeMillis()
    val cutoff  = if (selectedRange == TimeRange.ALL) 0L else now - selectedRange.windowMs
    val history = remember(stock.priceHistory.size, selectedRange) {
        stock.priceHistory.filter { it.first >= cutoff }
    }

    val firstPrice = history.firstOrNull()?.second ?: currentPrice
    val pnlVsOpen  = currentPrice - firstPrice
    val pctChange  = if (firstPrice != 0.0) (pnlVsOpen / firstPrice) * 100.0 else 0.0
    val lineColor  = if (pnlVsOpen >= 0) Gd else Bd

    // Animate draw progress on range change
    var animTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(selectedRange) { animTrigger++ }
    val drawProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
                                            label = "chartDraw"
    )

    Column(
        modifier = Modifier
        .fillMaxSize()
        .background(Bg)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
        // ── Back button ──────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onBack() }
            .padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Tx2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Back", color = Tx2, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        // ── Stock header ─────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stock.symbol, color = Acc, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                stock.sector,
                 color = sectorColors[stock.sector] ?: Tx2,
                 fontSize = 12.sp
            )
        }
        Text(stock.name, color = Tx1, fontSize = 26.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(12.dp))

        // ── Price + change ───────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                formatMoney(currentPrice),
                    color = Tx1,
                 fontSize = 32.sp,
                 fontWeight = FontWeight.ExtraBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (pnlVsOpen >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                     tint = lineColor,
                     modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "${formatSignedMoney(pnlVsOpen)}  (${String.format("%.2f", pctChange)}%)",
                     color = lineColor,
                     fontSize = 14.sp,
                     fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Chart ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surf1)
            .padding(12.dp)
        ) {
            if (history.size >= 2) {
                PriceLineChart(
                    history    = history,
                    lineColor  = lineColor,
                    modifier   = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Collecting data…", color = Tx2, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Time range tabs ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.values().forEach { range ->
                val selected = range == selectedRange
                Box(
                    modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Acc else Surf2)
                    .clickable { selectedRange = range }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        range.label,
                         color = if (selected) Bg else Tx2,
                         fontSize = 12.sp,
                         fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Stats row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surf1)
            .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val hi = history.maxOfOrNull { it.second } ?: currentPrice
            val lo = history.minOfOrNull { it.second } ?: currentPrice
            StatChip("HIGH", formatMoney(hi), Gd)
            StatChip("LOW",  formatMoney(lo),  Bd)
            StatChip("OPEN", formatMoney(firstPrice), Tx1)
            StatChip("LAST", formatMoney(currentPrice), Acc)
        }

        Spacer(Modifier.height(24.dp))

        // ── Trade card ───────────────────────────────────────────────────────
        StockDetailCard(
            stock            = stock,
            currentPrice     = currentPrice,
            portfolioCount   = portfolioCount,
            avgPrice         = avgPrice,
            quantityText     = quantityText,
            onQuantityChange = onQuantityChange,
            message          = message,
            messageIsError   = messageIsError,
            isFocused        = true,
            onBuy  = { qty ->
                if (!isPreview) {
                    if (user.buyStock(stock, qty)) {
                        onMessage("Bought $qty ${stock.symbol}", false)
                        sync()
                    } else {
                        onMessage("Insufficient funds", true)
                    }
                }
            },
            onSell = { qty ->
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

        Spacer(Modifier.height(32.dp))
    }
}

// ── Line chart via Canvas ────────────────────────────────────────────────────
@Composable
private fun PriceLineChart(
    history   : List<Pair<Long, Double>>,
    lineColor : Color,
    modifier  : Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

            val prices = history.map { it.second.toFloat() }
            val minP   = prices.min()
            val maxP   = prices.max()
            val range  = if (maxP - minP < 0.01f) 1f else maxP - minP

            val w = size.width
            val h = size.height

            val xStep = w / (prices.size - 1).toFloat()

            fun xOf(i: Int)   = i * xStep
            fun yOf(p: Float) = h - ((p - minP) / range) * h * 0.88f - h * 0.06f

            // Build path
            val path = Path()
            prices.forEachIndexed { i, p ->
                val x = xOf(i)
                val y = yOf(p)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Fill gradient below line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(xOf(prices.lastIndex), h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path  = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                                               startY = 0f,
                                               endY   = h
                )
            )

            // Draw line
            drawPath(
                path  = path,
                color = lineColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Endpoint dot
            val lastX = xOf(prices.lastIndex)
            val lastY = yOf(prices.last())
            drawCircle(color = lineColor, radius = 5f, center = Offset(lastX, lastY))
            drawCircle(color = lineColor.copy(alpha = 0.3f), radius = 10f, center = Offset(lastX, lastY))

            // Horizontal grid lines (3 lines)
            val gridPaint = lineColor.copy(alpha = 0.08f)
            for (i in 1..3) {
                val y = h * i / 4f
                drawLine(color = gridPaint, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            }
    }
}

// ── Stat chip ────────────────────────────────────────────────────────────────
@Composable
private fun StatChip(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Tx2, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Preview helpers ───────────────────────────────────────────────────────────
private fun fakeDetailStocks() = listOf(
    Stock("AAPL", "Apple Inc.",   "Technology", 182.50),
                                        Stock("TSLA", "Tesla Motors", "Automotive",  245.30),
)
