package com.neurospeech.app.core

/**
 * Timing captured around one recording attempt.
 *
 * All values are measurements from the system monotonic clock. They describe
 * app-level events only and explicitly do NOT claim to measure true speech
 * onset, reaction time, or any clinical latency.
 */
data class RecordingStats(
    /** Time from the user tapping "Start Listening" until recording actually began. */
    val tapToStartMs: Long,
    /** Time the microphone was actively capturing audio. */
    val recordingMs: Long,
    /** Wall-clock time the local transcription engine spent on the audio. */
    val processingMs: Long,
)

object Formatting {

    fun duration(ms: Long): String {
        val safe = if (ms < 0) 0 else ms
        val totalSec = safe / 1000
        val millis = safe % 1000
        return if (totalSec >= 60) {
            val min = totalSec / 60
            val sec = totalSec % 60
            "${min}m ${sec}s"
        } else {
            "${totalSec}.${(millis / 100)}s"
        }
    }

    fun millis(ms: Long): String = "$ms ms"

    fun bytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}