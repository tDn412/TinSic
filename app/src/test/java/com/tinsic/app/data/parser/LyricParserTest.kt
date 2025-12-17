package com.tinsic.app.data.parser

import com.tinsic.app.data.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricParserTest {

    @Test
    fun `parseLrc parses valid lrc content correctly`() {
        val lrcContent = """
            [00:12.00]First line
            [00:17.50]Second line
            [01:05.10]Third line
        """.trimIndent()

        val result = LyricParser.parseLrc(lrcContent)

        assertEquals(3, result.size)
        
        // Check first line
        assertEquals(12000L, result[0].timeMs)
        assertEquals("First line", result[0].text)

        // Check second line
        assertEquals(17500L, result[1].timeMs)
        assertEquals("Second line", result[1].text)

        // Check third line
        assertEquals(65100L, result[2].timeMs)
        assertEquals("Third line", result[2].text)
    }

    @Test
    fun `parseLrc ignores empty lines and bad formats`() {
        val lrcContent = """
            
            [00:12.00]Valid line
            Bad format line
            [invalid]
            [00:15.00]   
        """.trimIndent()

        val result = LyricParser.parseLrc(lrcContent)

        assertEquals(1, result.size)
        assertEquals(12000L, result[0].timeMs)
        assertEquals("Valid line", result[0].text)
    }

    @Test
    fun `parseLrc sorts lines by timestamp`() {
        val lrcContent = """
            [00:20.00]Later line
            [00:10.00]Earlier line
        """.trimIndent()

        val result = LyricParser.parseLrc(lrcContent)

        assertEquals(2, result.size)
        assertEquals("Earlier line", result[0].text)
        assertEquals("Later line", result[1].text)
    }
}
