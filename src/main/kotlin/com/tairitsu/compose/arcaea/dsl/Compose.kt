package com.tairitsu.compose.arcaea.dsl

import com.tairitsu.compose.arcaea.Difficulty

@Deprecated(message = "This feature is currently not supported", level = DeprecationLevel.WARNING)
fun Difficulty.bar(id: Int, closure: Bar.() -> Unit = {}): Bar {
    val bar = Bar(this, id)
    bar.closure()
    return bar
}
