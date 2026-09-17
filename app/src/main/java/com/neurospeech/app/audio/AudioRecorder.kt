package com.neurospeech.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import java.io.File

/** Native microphone capture using [AudioRecord], writing 16-bit PCM WAV. */
class AudioRecorder : RecordingControl {

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private var state = State.IDLE
    private var captureThread: Thread? = null
    private var activeWriter: WavFileWriter? = null

    @Volatile
    private var recording = false

    private enum class State { IDLE, RECORDING }

    @Synchronized
    @SuppressLint("MissingPermission") // RECORD_AUDIO is granted before the assessment starts.
    override fun start(outputFile: File): Long {
        check(state == State.IDLE) { "Recorder already active" }
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        require(minBuffer > 0) { "Unsupported audio parameters on this device" }
        val bufferBytes = maxOf(minBuffer, 2048)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_IN, ENCODING, bufferBytes
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "Unable to initialize the microphone"
        }

        record.startRecording()
        val startedAt = SystemClock.elapsedRealtime()
        recording = true
        state = State.RECORDING

        val writer = WavFileWriter(outputFile)
        activeWriter = writer
        captureThread = Thread(
            { captureLoop(record, writer, bufferBytes) },
            "neurospeech-recorder"
        ).apply { start() }

        return startedAt
    }

    private fun captureLoop(record: AudioRecord, writer: WavFileWriter, bufferBytes: Int) {
        val buffer = ShortArray(bufferBytes / 2)
        try {
            while (recording) {
                val read = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read > 0) writer.writeShorts(buffer, read)
            }
        } finally {
            try {
                record.stop()
            } catch (_: IllegalStateException) {
                // recorder was never started or already released
            }
            record.release()
        }
    }

    @Synchronized
    override fun stop() {
        if (state != State.RECORDING) return
        recording = false
        captureThread?.join(2000)
        captureThread = null
        state = State.IDLE
        activeWriter?.finish()
        activeWriter = null
    }
}