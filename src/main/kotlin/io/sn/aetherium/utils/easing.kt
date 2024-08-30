@file:Suppress("unused")

package io.sn.aetherium.utils

import com.tairitsu.compose.arcaea.Position
import kotlin.math.*

typealias EasingFunction = (progress: Double) -> Double

typealias EasingFunction3D = (progress: Double) -> Position

fun buildEasingFunction3D(xAxis: EasingFunction, yAxis: EasingFunction): EasingFunction3D = { progress ->
    Position(xAxis(progress), yAxis(progress))
}

fun buildEasingFunction3D(xyAxis: EasingFunction): EasingFunction3D = buildEasingFunction3D(xyAxis, xyAxis)

const val c1 = 1.70158
const val c2 = c1 * 1.525
const val c3 = c1 + 1
const val c4 = (2 * PI) / 3
const val c5 = (2 * PI) / 4.5

fun cubicBezier3D(deltaTime: Long, p0: Position, p1: Position, p2: Position, p3: Position): EasingFunction3D {
    return { progress ->
        val u = 1 - progress
        val tt = progress * progress
        val uu = u * u
        val ttt = tt * progress
        val uuu = uu * u

        val x = uuu * p0.x + 3 * uu * progress * p1.x + 3 * u * tt * p2.x + ttt * p3.x
        val y = uuu * p0.y + 3 * uu * progress * p1.y + 3 * u * tt * p2.y + ttt * p3.y

        Position(x, y)
    }
}

val bounceOut: EasingFunction = { x ->
    val n1 = 7.5625
    val d1 = 2.75
    when {
        x < 1 / d1 -> n1 * x * x
        x < 2 / d1 -> n1 * (x - 1.5 / d1) * x + 0.75
        x < 2.5 / d1 -> n1 * (x - 2.25 / d1) * x + 0.9375
        else -> n1 * (x - 2.625 / d1) * x + 0.984375
    }
}

val linear: EasingFunction = { x -> x }
val easeInQuad: EasingFunction = { x -> x * x }
val easeOutQuad: EasingFunction = { x -> 1 - (1 - x) * (1 - x) }
val easeInOutQuad: EasingFunction = { x -> if (x < 0.5) 2 * x * x else 1 - (-2 * x + 2).pow(2) / 2 }
val easeInCubic: EasingFunction = { x -> x * x * x }
val easeOutCubic: EasingFunction = { x -> 1 - (1 - x).pow(3) }
val easeInOutCubic: EasingFunction = { x -> if (x < 0.5) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2 }
val easeInQuart: EasingFunction = { x -> x * x * x * x }
val easeOutQuart: EasingFunction = { x -> 1 - (1 - x).pow(4) }
val easeInOutQuart: EasingFunction = { x -> if (x < 0.5) 8 * x * x * x * x else 1 - (-2 * x + 2).pow(4) / 2 }
val easeInQuint: EasingFunction = { x -> x * x * x * x * x }
val easeOutQuint: EasingFunction = { x -> 1 - (1 - x).pow(5) }
val easeInOutQuint: EasingFunction = { x -> if (x < 0.5) 16 * x * x * x * x * x else 1 - (-2 * x + 2).pow(5) / 2 }
val easeInSine: EasingFunction = { x -> 1 - cos((x * PI) / 2) }
val easeOutSine: EasingFunction = { x -> sin((x * PI) / 2) }
val easeInOutSine: EasingFunction = { x -> -(cos(PI * x) - 1) / 2 }
val easeInExpo: EasingFunction = { x -> if (x == 0.0) 0.0 else 2.0.pow(10 * x - 10) }
val easeOutExpo: EasingFunction = { x -> if (x == 1.0) 1.0 else 1 - 2.0.pow(-10 * x) }
val easeInOutExpo: EasingFunction = { x ->
    when {
        x == 0.0 -> 0.0
        x == 1.0 -> 1.0
        x < 0.5 -> 2.0.pow(20 * x - 10) / 2
        else -> (2 - 2.0.pow(-20 * x + 10)) / 2
    }
}
val easeInCirc: EasingFunction = { x -> 1 - sqrt(1 - x.pow(2)) }
val easeOutCirc: EasingFunction = { x -> sqrt(1 - (x - 1).pow(2)) }
val easeInOutCirc: EasingFunction = { x -> if (x < 0.5) (1 - sqrt(1 - (2 * x).pow(2))) / 2 else (sqrt(1 - (-2 * x + 2).pow(2)) + 1) / 2 }
val easeInBack: EasingFunction = { x -> c3 * x * x * x - c1 * x * x }
val easeOutBack: EasingFunction = { x -> 1 + c3 * (x - 1).pow(3) + c1 * (x - 1).pow(2) }
val easeInOutBack: EasingFunction =
    { x -> if (x < 0.5) (2 * x).pow(2) * ((c2 + 1) * 2 * x - c2) / 2 else ((2 * x - 2).pow(2) * ((c2 + 1) * (2 * x - 2) + c2) + 2) / 2 }
val easeInElastic: EasingFunction =
    { x -> if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else (-2.0).pow(10 * x - 10) * sin((x * 10 - 10.75) * c4) }
val easeOutElastic: EasingFunction = { x -> if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else 2.0.pow(-10 * x) * sin((x * 10 - 0.75) * c4) + 1 }
val easeInOutElastic: EasingFunction = { x ->
    if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else if (x < 0.5) -(2.0.pow(20 * x - 10) * sin((20 * x - 11.125) * c5)) / 2 else (2.0.pow(-20 * x + 10) * sin(
        (20 * x - 11.125) * c5
    )) / 2 + 1
}
val easeInBounce: EasingFunction = { x -> 1 - bounceOut(1 - x) }
val easeOutBounce: EasingFunction = bounceOut
val easeInOutBounce: EasingFunction = { x -> if (x < 0.5) (1 - bounceOut(1 - 2 * x)) / 2 else (1 + bounceOut(2 * x - 1)) / 2 }
