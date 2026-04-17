package com.stock.util

import kotlin.math.abs
import kotlin.math.roundToLong

fun formatMoney(value: Double): String {
    val negative = value < 0
    val cents = (abs(value) * 100.0).roundToLong()
    val integerPart = cents / 100
    val fraction = (cents % 100).toInt()
    val grouped = integerPart.toString().reversed().chunked(3).joinToString(",").reversed()
    return buildString {
        if (negative) append('-')
        append(grouped)
        append('.')
        append(fraction.toString().padStart(2, '0'))
    }
}

fun formatSignedMoney(value: Double): String = if (value >= 0) "+\$${formatMoney(value)}" else "-\$${formatMoney(abs(value))}"

fun formatPercent(value: Double): String {
    val negative = value < 0
    val scaled = (abs(value) * 100.0).roundToLong()
    val integerPart = scaled / 100
    val fraction = (scaled % 100).toInt()
    return buildString {
        if (negative) append('-')
        append(integerPart)
        append('.')
        append(fraction.toString().padStart(2, '0'))
        append('%')
    }
}
