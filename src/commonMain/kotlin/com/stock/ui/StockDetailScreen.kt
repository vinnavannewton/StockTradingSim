package com.stock.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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

private enum class TimeRange(val label: String, val windowMs: Long) {
    ONE_MIN("1M",   60_000L),
    FIVE_MIN("5M",  300_000L),
    FIFTEEN_MIN("15M", 900_000L),
    ONE_HOUR("1H",  3_600_000L),
    ALL("ALL",      Long.MAX_VALUE),
}

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

    val now     = System.currentTimeMillis()
    val cutoff  = if (selectedRange == TimeRange.ALL) 0L else now - selectedRange.windowMs
    val history = remember(stock.priceHistory.size, selectedRange) {
        stock.priceHistory.filter { it.first >= cutoff }
    }

    val firstPrice = history.firstOrNull()?.second ?: currentPrice
    val pnlVsOpen  = currentPrice - firstPrice
    val pctChange  = if (firstPrice != 0.0) (pnlVsOpen / firstPrice) * 100.0 else 0.0
    val lineColor  = if (pnlVsOpen >= 0) SFColor.Gain else SFColor.Loss

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SFColor.Bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable { onBack() }
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = SFColor.TextPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Market Details", color = SFColor.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        // Stock Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stock.symbol, color = SFColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stock.name, color = SFColor.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SFColor.sectorColors[stock.sector]?.copy(alpha = 0.1f) ?: SFColor.Surface2)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stock.sector, color = SFColor.sectorColors[stock.sector] ?: SFColor.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Price Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMoney(currentPrice), color = SFColor.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(lineColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (pnlVsOpen >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        null, tint = lineColor, modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${String.format("%.2f", pctChange)}%",
                        color = lineColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Chart Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(SFShape.Large)
                .background(SFColor.SurfaceCard)
                .border(1.dp, SFColor.Border, SFShape.Large)
                .padding(16.dp)
        ) {
            if (history.size >= 2) {
                PriceLineChart(history, lineColor, Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SFColor.Accent, strokeWidth = 2.dp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Time Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.values().forEach { range ->
                val selected = range == selectedRange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) SFColor.Accent else SFColor.Surface2)
                        .clickable { selectedRange = range }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(range.label, color = if (selected) SFColor.Bg else SFColor.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Trading Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val high = history.maxOfOrNull { it.second } ?: currentPrice
            val low = history.minOfOrNull { it.second } ?: currentPrice
            DetailStatCard("24h High", formatMoney(high), SFColor.Gain, Modifier.weight(1f))
            DetailStatCard("24h Low", formatMoney(low), SFColor.Loss, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // Integrated Trading Card
        StockDetailCard(
            stock = stock,
            currentPrice = currentPrice,
            portfolioCount = portfolioCount,
            avgPrice = avgPrice,
            quantityText = quantityText,
            onQuantityChange = onQuantityChange,
            message = message,
            messageIsError = messageIsError,
            isFocused = true,
            onBuy = { qty ->
                if (!isPreview) {
                    if (user.buyStock(stock, qty)) { onMessage("Order Filled: +$qty ${stock.symbol}", false); sync() }
                    else onMessage("Insufficient Funds", true)
                }
            },
            onSell = { qty ->
                if (!isPreview) {
                    if (user.sellStock(stock, qty)) { onMessage("Order Filled: -$qty ${stock.symbol}", false); sync() }
                    else onMessage("Not Enough Shares", true)
                }
            }
        )
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DetailStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SFShape.Medium)
            .background(SFColor.SurfaceCard)
            .border(1.dp, SFColor.Border, SFShape.Medium)
            .padding(12.dp)
    ) {
        Text(label, color = SFColor.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PriceLineChart(history: List<Pair<Long, Double>>, lineColor: Color, modifier: Modifier = Modifier) {
    val prices = history.map { it.second.toFloat() }
    val minP = prices.minOrNull() ?: 0f
    val maxP = prices.maxOrNull() ?: 1f
    val range = (maxP - minP).coerceAtLeast(0.01f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val xStep = w / (prices.size - 1).coerceAtLeast(1).toFloat()

        fun xOf(i: Int) = i * xStep
        fun yOf(p: Float) = h - ((p - minP) / range) * h * 0.8f - h * 0.1f

        val path = Path().apply {
            prices.forEachIndexed { i, p ->
                val x = xOf(i)
                val y = yOf(p)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)),
            // We need to close the path for the fill
        )
        
        // Proper fill path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(xOf(prices.lastIndex), h)
            lineTo(0f, h)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)))

        drawPath(path, lineColor, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Last price dot
        drawCircle(lineColor, 4.dp.toPx(), Offset(xOf(prices.lastIndex), yOf(prices.last())))
    }
}

private fun fakeDetailStocks() = listOf(Stock("AAPL", "Apple Inc.", "Technology", 182.50))
