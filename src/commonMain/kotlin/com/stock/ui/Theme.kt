package com.stock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Core palette ──────────────────────────────────────────────────────────────
object SFColor {
    // Background layers (deep dark with subtle blue undertones)
    val Bg           = Color(0xFF050810)
    val Surface0     = Color(0xFF0A0F1E)
    val Surface1     = Color(0xFF0F1629)
    val Surface2     = Color(0xFF161D33)
    val Surface3     = Color(0xFF1C2541)
    val SurfaceCard  = Color(0xFF131A2E)

    // Accent & brand
    val Accent       = Color(0xFF38BDF8)  // Vibrant sky blue
    val AccentMuted  = Color(0xFF1D6FA0)
    val AccentSoft   = Color(0xFF1E3A5F)  // Subdued accent for bg tints
    val Indigo       = Color(0xFF818CF8)
    val IndigoMuted  = Color(0xFF4F46E5)

    // Semantic
    val Gain         = Color(0xFF22C55E)
    val GainBright   = Color(0xFF4ADE80)
    val GainBg       = Color(0xFF052E16)
    val Loss         = Color(0xFFEF4444)
    val LossBright   = Color(0xFFF87171)
    val LossBg       = Color(0xFF450A0A)

    // Text
    val TextPrimary  = Color(0xFFF1F5F9)
    val TextSecondary= Color(0xFF94A3B8)
    val TextMuted    = Color(0xFF64748B)
    val TextDim      = Color(0xFF475569)

    // Border & divider
    val Border       = Color(0xFF1E293B)
    val BorderLight  = Color(0xFF334155)
    val Divider      = Color(0xFF1E293B)

    // Overlay
    val Overlay      = Color(0xCC000000)

    // Sector colours (refined palette)
    val sectorColors = mapOf(
        "Technology" to Color(0xFF38BDF8),
        "Automotive" to Color(0xFFFB923C),
        "Finance"    to Color(0xFF4ADE80),
        "Healthcare" to Color(0xFFF472B6),
        "Consumer"   to Color(0xFFA78BFA),
        "Fintech"    to Color(0xFF22D3EE),
    )
}

// ── Gradients ─────────────────────────────────────────────────────────────────
object SFGradient {
    val accentBrand = Brush.horizontalGradient(
        listOf(SFColor.Accent, SFColor.Indigo)
    )
    val accentBrandVertical = Brush.verticalGradient(
        listOf(SFColor.Accent, SFColor.Indigo)
    )
    val cardSurface = Brush.verticalGradient(
        listOf(SFColor.Surface2, SFColor.Surface1)
    )
    val headerBar = Brush.horizontalGradient(
        listOf(SFColor.Surface2.copy(alpha = 0.9f), SFColor.Surface1.copy(alpha = 0.7f))
    )
    val gainBg = Brush.horizontalGradient(
        listOf(SFColor.GainBg.copy(alpha = 0.4f), Color.Transparent)
    )
    val lossBg = Brush.horizontalGradient(
        listOf(SFColor.LossBg.copy(alpha = 0.4f), Color.Transparent)
    )
    val bgRadial = Brush.radialGradient(
        colors = listOf(Color(0xFF0F2942), SFColor.Bg),
        radius = 1400f
    )
    val loginCard = Brush.verticalGradient(
        listOf(Color(0xFF111827).copy(alpha = 0.95f), SFColor.Surface1)
    )
}

// ── Shapes ────────────────────────────────────────────────────────────────────
object SFShape {
    val Small   = RoundedCornerShape(8.dp)
    val Medium  = RoundedCornerShape(12.dp)
    val Large   = RoundedCornerShape(16.dp)
    val XLarge  = RoundedCornerShape(20.dp)
    val XXLarge = RoundedCornerShape(24.dp)
    val Pill    = RoundedCornerShape(50)
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

// ── Small reusable decoratives ────────────────────────────────────────────────
@Composable
fun DotSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(SFColor.BorderLight)
    )
}
