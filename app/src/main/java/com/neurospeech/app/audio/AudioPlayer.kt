package com.neurospeech.app.audio

import android.media.MediaPlayer
import java.io.File

/** Playback of a recorded waveform; abstracted for testability. */
interface SoundPlayer {
    fun play(file: File)
    fun release()
}

/** Local playback using the platform [MediaPlayer]. */
class AudioPlayer : SoundPlayer {

    private var player: MediaPlayer? = null

    override fun play(file: File) {
        release()
        val p = MediaPlayer()
        try {
            p.setDataSource(file.absolutePath)
            p.prepare()
            p.start()
            p.setOnCompletionListener { it.release() }
            player = p
        } catch (e: Exception) {
            p.release()
            player = null
        }
    }

    override fun release() {
        player?.apply {
            setOnCompletionListener(null)
            release()
        }
        player = null
    }
}