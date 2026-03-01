package com.tairitsu.compose

import com.tairitsu.compose.Position.Companion.pos
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeDslTest {

    @Test
    fun dslTest() {
        compose {
            val a1 = arcNote(
                1000, // time
                2000, // endTime
                0.5 pos 1, // startPos (x, y)
                s, // ease
                0.5 pos 0, // endPos (x, y)
                blue // color
            )

            val a2 = arc(
                1000, // time
                2000, // endTime
                0.5, // startPosX
                0.5, // endPosX
                s, // ease
                1, // startPosY
                0, // endPosY
                0, // color id
                noneHitSound, // hitSound
                false // arcType (isGuidingLine)
            )

            // there two arc notes are equivalent
            assertEquals(a1, a2)
        }
    }
}