package com.tairitsu.compose

import com.tairitsu.compose.filter.ShimFilter
import com.tairitsu.compose.filter.ShimFilter.C2A.toArcaeaAngleFormat
import com.tairitsu.compose.parser.ArcCreateChartSerializer
import com.tairitsu.compose.parser.ArcaeaChartParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ShimFilterTest {

    @Test
    fun angleToArcaeaFormatTest() {
        assertEquals("0", 0.0.toArcaeaAngleFormat())
        assertEquals("10", 1.0.toArcaeaAngleFormat())
        assertEquals("0", 360.0.toArcaeaAngleFormat())
        assertEquals("1", 360.1.toArcaeaAngleFormat())
        assertEquals("3400", (-20.0).toArcaeaAngleFormat())
        assertEquals("3599", (-.1).toArcaeaAngleFormat())
        assertEquals("0", (-.01).toArcaeaAngleFormat())
    }
}

class ShimFilterConversionTest {

    class A2CConverter : ArcaeaChartParser() {
        override val globalEffectFilter: TimingGroupSpecialEffectFilter = ShimFilter.A2C
        override val globalEventFilter: EventFilter = ShimFilter.A2C
    }

    @Test
    fun a2cTest() {
        val expected = """
            AudioOffset:0
            -
            timing(0,100.00,4.00);
            arc(1200,1201,0.00,0.00,s,0.00,0.00,3,none,true)[arctap(1200,0.32)];
            timinggroup(anglex=100.0){
              timing(0,1.00,4.00);
              scenecontrol(0,hidegroup,0,0);
              scenecontrol(53485,trackdisplay,1000,0);
              scenecontrol(58800,hidegroup,0,0);
              timing(62300,0.00,4.00);
              timing(62400,1.00,4.00);
              timing(62500,2.00,4.00);
              timing(62600,3.00,4.00);
              timing(62700,5.00,4.00);
              timing(62800,10.00,4.00);
              timing(62900,20.00,4.00);
              timing(63000,35.00,4.00);
              timing(63100,50.00,4.00);
              timing(63200,75.00,4.00);
              timing(63300,100.00,4.00);
              timing(63400,150.00,4.00);
              timing(63500,200.00,4.00);
              timing(63600,200.00,4.00);
              arc(63600,63601,0.50,0.50,s,0.50,0.50,3,none,true)[arctap(63600,44.0)];
              scenecontrol(90237,trackdisplay,953000,255);
            };
        """.trimIndent()

        val chart =
            A2CConverter().parse(
                """
                AudioOffset:0
                -
                timing(0,100,4);
                arc(1200,1200,-0.08,0.08,s,0.00,0.00,3,none,false);
                timinggroup(anglex1000){
                  timing(0,1.00,4.00);
                  scenecontrol(0,hidegroup,0.00,0);
                  scenecontrol(53485,trackhide);
                  scenecontrol(58800,hidegroup,0.00,0);
                  timing(62300,0.00,4.00);
                  timing(62400,1.00,4.00);
                  timing(62500,2.00,4.00);
                  timing(62600,3.00,4.00);
                  timing(62700,5.00,4.00);
                  timing(62800,10.00,4.00);
                  timing(62900,20.00,4.00);
                  timing(63000,35.00,4.00);
                  timing(63100,50.00,4.00);
                  timing(63200,75.00,4.00);
                  timing(63300,100.00,4.00);
                  timing(63400,150.00,4.00);
                  timing(63500,200.00,4.00);
                  timing(63600,200.00,4.00);
                  arc(63600,63600,-10.50,11.50,s,0.50,0.50,3,none,false);
                  scenecontrol(90237,trackdisplay,953.00,255);
                };
            """.trimIndent()
            ).chart

        ArcCreateChartSerializer.serialize(chart).joinToString("\n").let {
            assertEquals(expected, it)
        }
    }
}