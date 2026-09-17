package com.neurospeech.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `duration formats seconds with tenths`() {
        assertEquals("0.0s", Formatting.duration(0))
        assertEquals("0.5s", Formatting.duration(500))
        assertEquals("1.2s", Formatting.duration(1250))
        assertEquals("59.9s", Formatting.duration(59_900))
    }

    @Test
    fun `duration formats minutes`() {
        assertEquals("1m 0s", Formatting.duration(60_000))
        assertEquals("2m 5s", Formatting.duration(125_000))
    }

    @Test
    fun `duration handles negatives as zero`() {
        assertEquals("0.0s", Formatting.duration(-10))
    }

    @Test
    fun `millis is exact`() {
        assertEquals("12 ms", Formatting.millis(12))
    }

    @Test
    fun `bytes formats units`() {
        assertEquals("500 B", Formatting.bytes(500))
        assertEquals("2 KB", Formatting.bytes(2048))
        assertEquals("1.5 MB", Formatting.bytes((1_048_576 * 1.5).toLong()))
    }
}