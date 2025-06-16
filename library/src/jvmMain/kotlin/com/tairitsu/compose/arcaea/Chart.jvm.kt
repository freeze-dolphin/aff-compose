package com.tairitsu.compose.arcaea

import java.text.DecimalFormat
import kotlin.math.roundToInt

internal actual val Double.affFormat: String
    get() {
        return DecimalFormat("#0.00").format((this * 100.00).roundToInt() / 100.00)
    }