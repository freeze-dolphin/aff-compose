package com.tairitsu.compose

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

actual fun Double.toAffFormat(precision: Int): String {
    return if (this > 9_007_199) {
        BigDecimal(this.toString()).setScale(precision, RoundingMode.HALF_UP).toPlainString()
    } else {
        String.format("%.${precision}f", (this * 100.0).roundToInt() / 100.0)
    }
}
