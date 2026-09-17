package com.neurospeech.app.audio

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/**
 * Minimal 16-bit PCM WAV writer (mono, 16 kHz by default). Operates on plain
 * java.io so it can be unit-tested without an Android device.
 */
class WavFileWriter(
    private val file: File,
    val sampleRate: Int = 16000,
    val channelCount: Int = 1,
    val bitsPerSample: Int = 16,
) : Closeable {

    private val bytesPerSample = bitsPerSample / 8
    private val blockAlign = bytesPerSample * channelCount
    private val headerSize = 44L
    private val raf = RandomAccessFile(file, "rw").apply {
        setLength(0)
        seek(headerSize)
    }
    private var sampleFrames: Long = 0
    private var closed = false

    fun writeShorts(data: ShortArray, count: Int) {
        check(!closed) { "Writer already closed" }
        require(count <= data.size) { "count out of range" }
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            val v = data[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        raf.write(bytes)
        sampleFrames += count
    }

    fun finish() {
        if (closed) return
        writeHeader()
        close()
    }

    private fun writeHeader() {
        val dataSize = sampleFrames * blockAlign
        val byteRate = sampleRate * blockAlign
        raf.seek(0)
        raf.write("RIFF".toByteArray())
        writeLeInt((36 + dataSize).toInt())
        raf.write("WAVE".toByteArray())
        raf.write("fmt ".toByteArray())
        writeLeInt(16)
        writeLeShort(1)
        writeLeShort(channelCount)
        writeLeInt(sampleRate)
        writeLeInt(byteRate)
        writeLeShort(blockAlign)
        writeLeShort(bitsPerSample)
        raf.write("data".toByteArray())
        writeLeInt(dataSize.toInt())
        raf.seek(raf.length())
    }

    private fun writeLeInt(v: Int) {
        raf.write(v and 0xFF)
        raf.write((v ushr 8) and 0xFF)
        raf.write((v ushr 16) and 0xFF)
        raf.write((v ushr 24) and 0xFF)
    }

    private fun writeLeShort(v: Int) {
        raf.write(v and 0xFF)
        raf.write((v ushr 8) and 0xFF)
    }

    override fun close() {
        if (closed) return
        closed = true
        raf.close()
    }
}