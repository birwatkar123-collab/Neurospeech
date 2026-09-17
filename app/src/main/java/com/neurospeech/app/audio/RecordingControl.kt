package com.neurospeech.app.audio

import java.io.File

/** Thin abstraction so the assessment flow can be unit-tested without a microphone. */
interface RecordingControl {
    /** Starts capturing into [outputFile]. Returns monotonic time recording began. */
    fun start(outputFile: File): Long

    /** Stops capturing and finalizes [outputFile]. Safe to call when not recording. */
    fun stop()
}