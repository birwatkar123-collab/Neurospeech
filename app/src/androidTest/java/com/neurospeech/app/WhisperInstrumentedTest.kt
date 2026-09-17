package com.neurospeech.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurospeech.app.audio.WavFileWriter
import com.neurospeech.app.whisper.WhisperTranscriber
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end on-device transcription test. Requires a physical ARM64 device
 * (the bundled native library is arm64-v8a only). Generates a short synthetic
 * tone and verifies the Whisper pipeline runs without crashing.
 */
@RunWith(AndroidJUnit4::class)
class WhisperInstrumentedTest {

    @Test
    fun transcribeGeneratedAudio() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val transcriber = WhisperTranscriber(context)

        transcriber.ensureModel()
        assertTrue(transcriber.isModelReady())

        val audio = File(context.cacheDir, "tone.wav")
        generateTone(audio)

        val result = transcriber.transcribe(audio)
        assertNotNull(result)
        // A generated tone is unlikely to be speech; the meaningful assertion is
        // that the pipeline ran to completion (above) without throwing.

        transcriber.releaseModel()
    }

    private fun generateTone(file: File) {
        val writer = WavFileWriter(file, sampleRate = 16000)
        val buffer = ShortArray(16000 / 2) { i -> // 0.5s
            (Math.sin(2.0 * Math.PI * 440.0 * i / 16000.0) * 8000.0).toInt().toShort()
        }
        writer.writeShorts(buffer, buffer.size)
        writer.finish()
    }
}