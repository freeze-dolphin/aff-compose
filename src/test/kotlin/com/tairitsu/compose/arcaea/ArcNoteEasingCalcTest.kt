package com.tairitsu.compose.arcaea

import kotlin.test.Test
import kotlin.test.assertEquals

class ArcNoteEasingCalcTest {

    @Test
    fun `test arcnote easing calculation`() {
        assertEquals(0 pos 0, ArcNote.getEasingFunction3D(0 pos 0, 0 pos 0, ArcNote.CurveType.S).invoke(0.2, 0 pos 0, 0 pos 0))
    }

}