package com.neurospeech.app

import android.app.Application
import com.neurospeech.app.audio.AudioPlayer
import com.neurospeech.app.audio.AudioRecorder
import com.neurospeech.app.audio.AudioStore
import com.neurospeech.app.audio.RecordingControl
import com.neurospeech.app.audio.RecordingStore
import com.neurospeech.app.audio.SoundPlayer
import com.neurospeech.app.whisper.Transcriber
import com.neurospeech.app.whisper.WhisperTranscriber

/** Simple manual dependency wiring for the assessment flow. */
class AppContainer(private val app: Application) {

    val recordingStore: RecordingStore by lazy { AudioStore(app) }

    val soundPlayer: SoundPlayer by lazy { AudioPlayer() }

    val transcriber: Transcriber by lazy { WhisperTranscriber(app) }

    val recorderFactory: () -> RecordingControl = { AudioRecorder() }
}