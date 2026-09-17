package com.neurospeech.app.whisper

import java.io.File

data class TranscriptionResult(
    /** Raw text produced by the local transcription engine (may be empty). */
    val text: String,
    /** Wall-clock time in milliseconds spent inside the engine for this clip. */
    val processingMs: Long,
)

interface Transcriber {

    /** Makes the on-device model available (bundled asset or one-time download). */
    suspend fun ensureModel(
        onProgress: ((downloadedBytes: Long) -> Unit)? = null,
    )

    suspend fun isModelReady(): Boolean

    /** Transcribes [audioFile] fully on-device. */
    suspend fun transcribe(audioFile: File): TranscriptionResult

    /** Frees the loaded model; safe to call repeatedly. */
    fun releaseModel()
}