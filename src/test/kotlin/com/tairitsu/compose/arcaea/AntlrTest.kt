package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.antlr.PropertiesLexer
import com.tairitsu.compose.arcaea.antlr.PropertiesParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.Test

class AntlrTest {

    @Test
    fun `test antlr`() {
        val stream = readSampleFile()
        val lexer = PropertiesLexer(stream)
        val tokens = CommonTokenStream(lexer)
        val parser = PropertiesParser(tokens)

        while (parser.currentToken.type != PropertiesParser.EOF) {
            val nextLine = parser.line()
            val kw = nextLine.keyValue()
            val key = kw.key().text
            val value = kw.separatorAndValue().chars()
                .joinToString(separator = "") {
                    it.text
                }

            println("$key = $value")
        }

    }

    private fun readSampleFile(): CharStream {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        return contextClassLoader.getResourceAsStream("sample.properties").use { input ->
            CharStreams.fromStream(input)
        }
    }

}