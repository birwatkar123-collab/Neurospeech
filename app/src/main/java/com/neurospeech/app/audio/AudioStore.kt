package com.neurospeech.app.audio

import android.content.Context
import java.io.File

/** Owns the on-device recordings directory. Audio never leaves the device. */
interface RecordingStore {
    fun nextFile(): File
    fun delete(file: File)
}

class AudioStore(private val context: Context) : RecordingStore {

    private val root = File(context.filesDir, "recordings")

    override fun nextFile(): File {
        val dir = root.apply { mkdirs() }
        return File(dir, "rec_${System.currentTimeMillis()}_${(1000..9999).random()}.wav")
    }

    override fun delete(file: File) {
        runCatching { file.delete() }
    }
}