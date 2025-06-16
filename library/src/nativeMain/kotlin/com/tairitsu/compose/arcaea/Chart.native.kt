package com.tairitsu.compose.arcaea

import kotlin.math.roundToInt

internal actual val Double.affFormat: String
    get() {
        val rounded = (this * 100.0).roundToInt() / 100.0
        val str = rounded.toString()
        return if (str.contains('.')) {
            val (whole, decimal) = str.split('.')
            "$whole.${decimal.take(2).padEnd(2, '0')}"
        } else {
            "$str.00"
        }
    }