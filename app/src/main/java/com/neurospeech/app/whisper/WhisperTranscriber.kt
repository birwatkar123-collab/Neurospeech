package com.neurospeech.app.whisper

import android.content.Context
import android.os.SystemClock
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local transcription backed by whisper.cpp via the prebuilt
 * `whisper-android` library. The model is loaded once and cached for the
 * duration of an assessment session.
 */
class WhisperTranscriber(private val context: Context) : Transcriber {

    @Volatile
    private var cachedModel: WhisperModel? = null

    override suspend fun ensureModel(
        onProgress: ((downloadedBytes: Long) -> Unit)?,
    ) {
        ModelManager.ensureModel(context, onProgress)
    }

    override suspend fun isModelReady(): Boolean = ModelManager.isModelReady(context)

    override suspend fun transcribe(audioFile: File): TranscriptionResult =
        withContext(Dispatchers.Default) {
            val model = cachedModel ?: loadModel().also { cachedModel = it }
            val startedAt = SystemClock.elapsedRealtime()
            val result = runCatching {
                Whisper.transcribe(model, audioFile.absolutePath, WhisperConfig(language = "en"))
            }.getOrElse { error ->
                cachedModel = null
                Whisper.releaseModel(model)
                throw error
            }
            TranscriptionResult(result.text.trim(), SystemClock.elapsedRealtime() - startedAt)
        }

    private suspend fun loadModel(): WhisperModel {
        val file = ModelManager.modelFile(context)
        check(file.exists()) { "Speech model unavailable" }
        return Whisper.loadModel(context, file.absolutePath)
    }

    override fun releaseModel() {
        cachedModel?.let { model ->
            runCatching { Whisper.releaseModel(model) }
        }
        cachedModel = null
    }
}