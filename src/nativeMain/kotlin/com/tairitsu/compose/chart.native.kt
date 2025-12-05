package com.tairitsu.compose

actual fun Double.toAffFormat(precision: Int): String {
    if (this.isNaN()) return "0.00"
    if (this.isInfinite()) return if (this > 0) "99999999999999.99" else "-99999999999999.99"

    val sign = if (this < 0) "-" else ""
    val absValue = kotlin.math.abs(this)

    val roundedCents = (absValue * 100.0 + 0.5).toLong()

    val decimal = roundedCents / 100
    val fraction = roundedCents % 100

    return "$sign$decimal.${fraction.toString().padStart(precision, '0')}"
}
