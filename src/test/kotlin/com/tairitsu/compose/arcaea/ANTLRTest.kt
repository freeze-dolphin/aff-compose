package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.antlr.PropertiesLexer
import com.tairitsu.compose.arcaea.antlr.PropertiesParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ANTLRTest {

    private val json = Json {
        prettyPrint = true
    }

    private fun testAffAndPrint(aff: String, printAsAff: Boolean = true) {
        val crlfAff = aff.replace(Regex(System.lineSeparator()), "\r\n")

        if (!printAsAff) {
            println(json.encodeToString(ANTLRChartParser.fromAff(crlfAff)))
        } else {
            println(json.encodeToString(ANTLRChartParser.fromAff(crlfAff)).let {
                val chart: Chart = json.decodeFromString(it)
                chart.serialize()
            })
        }
    }

    @Test
    fun `arcaea chart antlr test`() {
        """
        AudioOffset:-600
        TimingPointDensityFactor:1
        Version:1.0
        -
        timing(0,126.00,4.00);
        scenecontrol(19045,trackhide);
        (1,2);
        arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
        scenecontrol(40960,redline,1.88,0);
        arc(17140,19045,0.00,1.00,s,1.00,1.00,3,none,false)[arctap(17140),arctap(19045)];
        arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
        timinggroup(fadingholds_anglex3600){
            timing(0,126.00,4.00);
            hold(17140,18807,4);
            arc(17140,19045,0.00,1.00,si,1.00,1.00,0,none,true)[arctap(17140),arctap(19045)];
            arc(17140,18569,0.00,0.50,siso,1.00,0.00,0,none,true)[arctap(18569)];
            arc(17140,18093,0.00,0.25,siso,1.00,0.25,0,none,true)[arctap(18093)];
            arc(17140,17616,0.00,0.00,siso,1.00,0.50,0,none,true);
            hold(19045,20712,4);
            arc(19045,20712,-0.25,1.50,b,1.00,0.00,0,none,true);
            arc(19045,20712,1.25,-0.50,b,1.00,0.00,1,none,false);
        };
        """.trimIndent().let {
            testAffAndPrint(it)
        }
    }

    @Test
    fun `antlr playground`() {
        val properties = """
            key1=1
            key2=22
            key3=three
        """.trimIndent()
        val stream = CharStreams.fromString(properties)
        val lexer = PropertiesLexer(stream)
        val tokens = CommonTokenStream(lexer)
        val parser = PropertiesParser(tokens)

        val list = mutableListOf<Pair<String, String>>()

        while (parser.currentToken.type != PropertiesParser.EOF) {
            val nextLine = parser.line()
            val kw = nextLine.keyValue()
            val key = kw.key().text
            val value = kw.separatorAndValue().chars()
                .joinToString(separator = "") {
                    it.text
                }

            list.add(key to value)
        }
        assertEquals(
            list, mutableListOf(
                "key1" to "1",
                "key2" to "22",
                "key3" to "three"
            )
        )
    }


}