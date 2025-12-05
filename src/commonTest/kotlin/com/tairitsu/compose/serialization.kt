package com.tairitsu.compose

import com.tairitsu.compose.TimingGroup.SpecialEffectType
import com.tairitsu.compose.parser.ArcaeaChartParser
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ArcaeaChartParserTest {

    @Test
    fun serializeTest() {
        val content = """
            AudioOffset:0
            ChartVersion:1.0
            -
            timing(0,100,4);
            (1000,1);
            hold(1200,1400,2);
            arc(1500,2100,0,0.5,s,1,0,0,none,false);
            arc(2100,2400,0.5,1,si,0,0.5,0,none,false,2);
            arc(2400,3000,0.5,0.5,s,1,1,0,none,true)[arctap(2400),arctap(2600)];
        """.trimIndent()
        val expect = ArcaeaChartParser.Instance.parse(content).chart
        val expectNotesInMain = expect.mainTiming.getNotes()

        val actual = compose(chartConfig(0, "ChartVersion" to "1.0")) {
            timing(0, 100, 4)
            normalNote(1000, 1)
            hold(1200, 1400, 2)
            arc(1500, 2100, 0, 0.5, s, 1, 0, 0, none, false)
            arc(2100, 2400, 0.5, 1, si, 0, 0.5, 0, none, false, 2.0)
            arc(2400, 3000, 0.5, 0.5, s, 1, 1, 0, none, true) { arctap(2400); arctap(2600) }
        }.chart

        assertEquals(expect.configuration, actual.configuration)
        actual.mainTiming.getNotes().forEach {
            assertContains(expectNotesInMain, it)
        }
    }

    @Test
    fun parseTest() {
        val content = """
            AudioOffset:0
            ChartVersion:1.0
            -
            timing(0,100,4);
            (1000,1);
            hold(1200,1400,2);
            arc(1500,2100,0,0.5,s,1,0,0,none,false);
            arc(2100,2400,0.5,1,si,0,0.5,0,none,false,2);
            arc(2400,3000,0.5,0.5,s,1,1,0,none,true)[arctap(2400),arctap(2600)];
            timinggroup(){
              timing(0,100,4);
              (1234,0.56);
              hold(1234,1400,0.23);
              arc(2500,3100,0,0.5,s,1,0,0,none,false);
              arc(3100,3400,0.5,1,si,0,0.5,0,none,false,2);
              arc(3400,4000,0.5,0.5,s,1,1,0,none,true)[arctap(3400),arctap(3600)];
            };
            timinggroup(noinput){
              timing(0,100,4);
              camera(152141,0.00,3000.00,2000.00,0.00,0.00,0.00,qo,1785);
              camera(152141,0.00,0.00,0.00,0.00,-10.00,0.00,qo,1785);
              camera(153926,0.00,-3000.00,-2000.00,0.00,0.00,0.00,qi,1785);
              camera(153926,0.00,0.00,0.00,0.00,10.00,0.00,qi,1785);
              camera(155712,0.00,0.00,0.00,0.00,0.00,0.00,reset,23);
            };
            timinggroup(noinput_anglex1900){
              timing(0,252.00,4.00);
              scenecontrol(82618,trackdisplay,953.00,0);
              scenecontrol(90237,trackdisplay,953.00,255);
              scenecontrol(129285,enwidencamera,1905.00,1);
              scenecontrol(129285,enwidenlanes,952.50,1);
              scenecontrol(142619,enwidencamera,1905.00,0);
              scenecontrol(142619,enwidenlanes,952.50,0);
            };
            timinggroup(unknown_uint32){
              timing(0,100,4);
            };
        """.trimIndent()
        val chart = ArcaeaChartParser.Instance.parse(content).chart

        // header
        chart.configuration.extra.first().apply {
            assertEquals("ChartVersion", key)
            assertEquals("1.0", value)
        }

        // arctap
        chart.mainTiming.getNotes().filter { it is ArcNote && it.arcTapList.isNotEmpty() }.map { (it as ArcNote).arcTapList }.last().apply {
            assertEquals(2400, this[0].time)
            assertEquals(2600, this[1].time)
        }

        // timinggroup fx
        assertEquals(
            listOf(
                TimingGroup.SpecialEffect(SpecialEffectType.NO_INPUT),
                TimingGroup.SpecialEffect(
                    SpecialEffectType.ANGLEX,
                    1900
                )
            ),
            chart.subTiming.values.toList()[2].getSpecialEffects()
        )

        // unknown timinggroup fx
        assertEquals(
            listOf("unknown" to null, "uint" to "32"),
            chart.subTiming.values.toList()[3].getSpecialEffects().map { it.type.value to it.param }
        )
    }
}