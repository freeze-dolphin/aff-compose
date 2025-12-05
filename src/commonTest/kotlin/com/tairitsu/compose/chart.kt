package com.tairitsu.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartTest {

    @Test
    fun toAffFormatTest() {
        assertEquals("9999999990.00", 9999999990.00.toAffFormat()) // big number
        assertEquals("9999999990.46", 9999999990.455.toAffFormat()) // big number round
        assertEquals("9999999990.45", 9999999990.454.toAffFormat()) // big number round
        assertEquals("0.00", .0.toAffFormat()) // keep two decimal places
        assertEquals("12.46", 12.455.toAffFormat()) // round
        assertEquals("12.45", 12.454.toAffFormat()) // round
    }
}