package com.tairitsu.compose.arcaea

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LocalizedStringTest {
    @Test
    fun `test for serialization`() {
        val a = LocalizedString("Test").apply {
            ja = "テスト"
            ko = "테스트"
            zhHans = "测试"
            zhHant = "測試"
        }

        val json = Json.encodeToString(a)

        val b = Json.decodeFromString<LocalizedString>(json)

        assertEquals(a.en, b.en)
        assertEquals(a.ja, b.ja)
        assertEquals(a.ko, b.ko)
        assertEquals(a.zhHans, b.zhHans)
        assertEquals(a.zhHant, b.zhHant)
    }
}
