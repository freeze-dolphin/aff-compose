@file:Suppress("unused")

package io.sn.aetherium.utils

import com.tairitsu.compose.Position
import kotlin.math.*

typealias EaseFunc = (progress: Double) -> Double

typealias EaseFunc2 = (progress: Double, startPosition: Position, endPosition: Position) -> Position

fun easeFunc2(xAxis: EaseFunc, yAxis: EaseFunc): EaseFunc2 = { progress, startPosition, endPosition ->
    Position(
        (endPosition.x - startPosition.x) * xAxis(progress) + startPosition.x,
        (endPosition.y - startPosition.y) * yAxis(progress) + startPosition.y
    )
}

fun easeFunc2(xyAxis: EaseFunc): EaseFunc2 = easeFunc2(xyAxis, xyAxis)

const val c1 = 1.70158
const val c2 = c1 * 1.525
const val c3 = c1 + 1
const val c4 = (2 * PI) / 3
const val c5 = (2 * PI) / 4.5

private fun bezier(t: Double, controls: List<Double>): Double {
    if (controls.size == 1) {
        return controls[0]
    }

    val newPoints = mutableListOf<Double>()
    for (i in 0 until controls.size - 1) {
        val point = (1 - t) * controls[i] + t * controls[i + 1]
        newPoints.add(point)
    }

    return bezier(t, newPoints)
}

fun bezierEaseFunc2(vararg p: Double): EaseFunc2 = easeFunc2({ progress ->
    bezier(progress, p.toList())
}, linear)

val bounceOut: EaseFunc = { x ->
    val n1 = 7.5625
    val d1 = 2.75
    when {
        x < 1 / d1 -> n1 * x * x
        x < 2 / d1 -> n1 * (x - 1.5 / d1) * x + 0.75
        x < 2.5 / d1 -> n1 * (x - 2.25 / d1) * x + 0.9375
        else -> n1 * (x - 2.625 / d1) * x + 0.984375
    }
}

val linear: EaseFunc = { x -> x }
val easeInQuad: EaseFunc = { x -> x * x }
val easeOutQuad: EaseFunc = { x -> 1 - (1 - x) * (1 - x) }
val easeInOutQuad: EaseFunc = { x -> if (x < 0.5) 2 * x * x else 1 - (-2 * x + 2).pow(2) / 2 }
val easeInCubic: EaseFunc = { x -> x * x * x }
val easeOutCubic: EaseFunc = { x -> 1 - (1 - x).pow(3) }
val easeInOutCubic: EaseFunc = { x -> if (x < 0.5) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2 }
val easeInQuart: EaseFunc = { x -> x * x * x * x }
val easeOutQuart: EaseFunc = { x -> 1 - (1 - x).pow(4) }
val easeInOutQuart: EaseFunc = { x -> if (x < 0.5) 8 * x * x * x * x else 1 - (-2 * x + 2).pow(4) / 2 }
val easeInQuint: EaseFunc = { x -> x * x * x * x * x }
val easeOutQuint: EaseFunc = { x -> 1 - (1 - x).pow(5) }
val easeInOutQuint: EaseFunc = { x -> if (x < 0.5) 16 * x * x * x * x * x else 1 - (-2 * x + 2).pow(5) / 2 }
val easeInSine: EaseFunc = { x -> 1 - cos((x * PI) / 2) }
val easeOutSine: EaseFunc = { x -> sin((x * PI) / 2) }
val easeInOutSine: EaseFunc = { x -> -(cos(PI * x) - 1) / 2 }
val easeInExpo: EaseFunc = { x -> if (x == 0.0) 0.0 else 2.0.pow(10 * x - 10) }
val easeOutExpo: EaseFunc = { x -> if (x == 1.0) 1.0 else 1 - 2.0.pow(-10 * x) }
val easeInOutExpo: EaseFunc = { x ->
    when {
        x == 0.0 -> 0.0
        x == 1.0 -> 1.0
        x < 0.5 -> 2.0.pow(20 * x - 10) / 2
        else -> (2 - 2.0.pow(-20 * x + 10)) / 2
    }
}
val easeInCirc: EaseFunc = { x -> 1 - sqrt(1 - x.pow(2)) }
val easeOutCirc: EaseFunc = { x -> sqrt(1 - (x - 1).pow(2)) }
val easeInOutCirc: EaseFunc = { x -> if (x < 0.5) (1 - sqrt(1 - (2 * x).pow(2))) / 2 else (sqrt(1 - (-2 * x + 2).pow(2)) + 1) / 2 }
val easeInBack: EaseFunc = { x -> c3 * x * x * x - c1 * x * x }
val easeOutBack: EaseFunc = { x -> 1 + c3 * (x - 1).pow(3) + c1 * (x - 1).pow(2) }
val easeInOutBack: EaseFunc =
    { x -> if (x < 0.5) (2 * x).pow(2) * ((c2 + 1) * 2 * x - c2) / 2 else ((2 * x - 2).pow(2) * ((c2 + 1) * (2 * x - 2) + c2) + 2) / 2 }
val easeInElastic: EaseFunc =
    { x -> if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else (-2.0).pow(10 * x - 10) * sin((x * 10 - 10.75) * c4) }
val easeOutElastic: EaseFunc = { x -> if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else 2.0.pow(-10 * x) * sin((x * 10 - 0.75) * c4) + 1 }
val easeInOutElastic: EaseFunc = { x ->
    if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else if (x < 0.5) -(2.0.pow(20 * x - 10) * sin((20 * x - 11.125) * c5)) / 2 else (2.0.pow(-20 * x + 10) * sin(
        (20 * x - 11.125) * c5
    )) / 2 + 1
}
val easeInBounce: EaseFunc = { x -> 1 - bounceOut(1 - x) }
val easeOutBounce: EaseFunc = bounceOut
val easeInOutBounce: EaseFunc = { x -> if (x < 0.5) (1 - bounceOut(1 - 2 * x)) / 2 else (1 + bounceOut(2 * x - 1)) / 2 }
