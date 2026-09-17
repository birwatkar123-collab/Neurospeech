package com.neurospeech.app.audio

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavFileWriterTest {

    @Test
    fun `produces a valid mono pcm wav file`() {
        val tmp = File.createTempFile("neurospeech", ".wav").also { file ->
            file.delete()
        }
        try {
            val writer = WavFileWriter(tmp)
            writer.writeShorts(shortArrayOf(-32768, 0, 32767, 1234), 4)
            writer.writeShorts(shortArrayOf(-1, 1), 2)
            writer.finish()

            val size = tmp.length()
            assertEquals(44 + 6 * 2L, size)

            RandomAccessFile(tmp, "r").use { raf ->
                val bytes = ByteArray(44)
                raf.readFully(bytes)
                assertEquals("RIFF", String(bytes, 0, 4))
                assertEquals("WAVE", String(bytes, 8, 4))
                assertEquals("fmt ", String(bytes, 12, 4))
                assertEquals(16, leInt(bytes, 16))
                assertEquals(1, leShort(bytes, 20)) // PCM
                assertEquals(1, leShort(bytes, 22)) // mono
                assertEquals(16000, leInt(bytes, 24))
                assertEquals(32000, leInt(bytes, 28)) // byte rate
                assertEquals(2, leShort(bytes, 32)) // block align
                assertEquals(16, leShort(bytes, 34)) // bits
                assertEquals("data", String(bytes, 36, 4))
                assertEquals(12, leInt(bytes, 40)) // data size = 6 frames * 2 bytes
            }
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun `empty recording still yields a valid header`() {
        val tmp = File.createTempFile("neurospeech", ".wav").also { file ->
            file.delete()
        }
        try {
            val writer = WavFileWriter(tmp)
            writer.finish()
            assertEquals(44L, tmp.length())
            RandomAccessFile(tmp, "r").use { raf ->
                val bytes = ByteArray(44)
                raf.readFully(bytes)
                assertEquals("RIFF", String(bytes, 0, 4))
                assertEquals(36, leInt(bytes, 4))
                assertEquals(0, leInt(bytes, 40))
            }
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun `writeShorts rejects count larger than buffer`() {
        val tmp = File.createTempFile("neurospeech", ".wav").also { file ->
            file.delete()
        }
        try {
            val writer = WavFileWriter(tmp)
            var threw = false
            try {
                writer.writeShorts(shortArrayOf(1, 2), 3)
            } catch (e: Exception) {
                threw = true
                assertTrue(e is IllegalArgumentException)
            }
            assertTrue(threw)
            writer.finish()
        } finally {
            tmp.delete()
        }
    }

    private fun leShort(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        return b0 or (b1 shl 8)
    }

    private fun leInt(bytes: ByteArray, offset: Int): Int {
        return leShort(bytes, offset) or (leShort(bytes, offset + 2) shl 16)
    }
}