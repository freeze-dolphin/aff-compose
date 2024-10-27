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
                chart.serializeForArcaea()
            })
        }
    }

    private fun testAcfAndPrint(acf: String, printAsAff: Boolean = true) {
        val crlfAcf = acf.replace(Regex(System.lineSeparator()), "\r\n")

        val parsed = ANTLRChartParser.fromAcf(crlfAcf)

        if (!printAsAff) {
            println(json.encodeToString(parsed.first))
        } else {
            println(json.encodeToString(parsed.first).let {
                val chart: Chart = json.decodeFromString(it)
                chart.serializeForArcaea()
            })
        }

        println("\n\nIgnored scenecontrols (${parsed.second.ignoredScenecontrols.size}):\n")
        parsed.second.ignoredScenecontrols.forEach {
            println("${it.first}, ${it.second}")
        }

        println("\nIgnored timinggroup effects (${parsed.second.ignoredTimingGroupEffects.size}):\n")
        parsed.second.ignoredTimingGroupEffects.forEach {
            val sec = if (it.second == null) {
                ""
            } else {
                ", ${it.second}"
            }
            println("${it.first}$sec")
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
    fun `arccreate chart antlr test distortedfate`() {
        Thread.currentThread().contextClassLoader.getResource("distortedfate.acf")
            ?.readText()?.trimIndent().let {
                testAcfAndPrint(it!!)
            }
    }

    @Test
    fun `arccreate chart antlr test avantgarde`() {
        Thread.currentThread().contextClassLoader.getResource("avantgarde.acf")
            ?.readText()?.trimIndent().let {
                testAcfAndPrint(it!!)
            }
    }

    @Test
    fun `arccreate timinggroup name test`() {
        testAcfAndPrint(
            """
            AudioOffset:0
            TimingPointDensityFactor:1.2
            -
            timing(0,186.00,4.00);
            timing(1333,180.00,0.00);
            timinggroup(name="glass"){
              timing(0,0.00,4.00);
              scenecontrol(-10000,hidegroup,0,1);
              arc(45161,45565,0.00,1.00,si,1.00,1.00,0,a,false);
            };
        """.trimIndent()
        )
    }

    @Test
    fun `arccreate timing not first command test`() {
        testAcfAndPrint(
            """
            AudioOffset:0
            -
            timing(0,186.00,4.00);
            timinggroup(name="glass"){
              scenecontrol(-10000,hidegroup,0,1);
              timing(0,0.00,4.00);
              scenecontrol(0,hidegroup,0,1);
              scenecontrol(66461,hidegroup,0,0);
              arc(112500,112501,0.50,0.50,s,0.50,0.50,0,empty.wav,true)[arctap(112500)];
            };
            """.trimIndent()
        )
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