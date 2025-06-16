package com.tairitsu.compose.arcaea.serializer

import com.tairitsu.compose.arcaea.Difficulty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Difficulty.generateTimingGroupJson(timingName: String): String {
    return Json.encodeToString(if (timingName == "main") this.chart.mainTiming else this.chart.subTiming[timingName])
}

fun Difficulty.generateChartJson(): String {
    return Json.encodeToString(this.chart)
}

