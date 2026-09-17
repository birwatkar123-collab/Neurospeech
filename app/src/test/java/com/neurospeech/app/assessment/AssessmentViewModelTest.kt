package com.neurospeech.app.assessment

import com.neurospeech.app.audio.RecordingControl
import com.neurospeech.app.audio.RecordingStore
import com.neurospeech.app.audio.SoundPlayer
import com.neurospeech.app.core.MatchStatus
import com.neurospeech.app.whisper.Transcriber
import com.neurospeech.app.whisper.TranscriptionResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentViewModelTest {

    private class FakeTranscriber(
        var text: String = "",
        var failOnTranscribe: Throwable? = null,
        var failOnPrepare: Boolean = false,
    ) : Transcriber {
        var prepared = false
        var released = false
        override suspend fun ensureModel(onProgress: ((Long) -> Unit)?) {
            prepared = true
            if (failOnPrepare) throw IllegalStateException("offline: no model available")
        }

        override suspend fun isModelReady(): Boolean = true

        override suspend fun transcribe(audioFile: File): TranscriptionResult {
            failOnTranscribe?.let { throw it }
            return TranscriptionResult(text.trim(), processingMs = 55L)
        }

        override fun releaseModel() {
            released = true
        }
    }

    private class FakeRecorder(
        val startAt: Long = 20L,
        var failStart: Boolean = false,
    ) : RecordingControl {
        var started = false
        var stopped = false
        var startedFile: File? = null

        override fun start(outputFile: File): Long {
            if (failStart) throw IllegalStateException("Microphone busy")
            started = true
            startedFile = outputFile
            return startAt
        }

        override fun stop() {
            stopped = true
        }
    }

    private class FakeStore : RecordingStore {
        var next = File("rec_1.wav")
        val deleted = mutableListOf<File>()

        override fun nextFile(): File = next

        override fun delete(file: File) {
            deleted += file
        }
    }

    private class FakePlayer : SoundPlayer {
        var played: File? = null

        override fun play(file: File) {
            played = file
        }

        override fun release() {
        }
    }

    private inner class Harness {
        var now = 0L
        val transcriber = FakeTranscriber()
        val store = FakeStore()
        val player = FakePlayer()
        var recorder = FakeRecorder()
        val scheduler = TestCoroutineScheduler()
        val viewModel: AssessmentViewModel

        init {
            Dispatchers.setMain(StandardTestDispatcher(scheduler))
            viewModel = AssessmentViewModel(
                transcriber = transcriber,
                recordingStore = store,
                soundPlayer = player,
                recorderFactory = { recorder },
                clock = { now },
            )
        }

        fun prepare() {
            viewModel.startAssessment()
            scheduler.advanceUntilIdle()
        }

        fun recordAndStopWith(text: String) {
            transcriber.text = text
            now = 1L
            viewModel.onStartListening()
            assertEquals(AssessmentPhase.LISTENING, viewModel.uiState.value.phase)
            now = 150L
            viewModel.onStopListening()
            scheduler.advanceUntilIdle()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start prepares model and lands on first item`() {
        val h = Harness()
        h.viewModel.startAssessment()
        assertEquals(AssessmentPhase.PREPARING, h.viewModel.uiState.value.phase)
        h.scheduler.advanceUntilIdle()
        assertTrue(h.transcriber.prepared)
        val state = h.viewModel.uiState.value
        assertEquals(AssessmentPhase.READY, state.phase)
        assertEquals("Apple", state.currentItem?.expectedAnswer)
    }

    @Test
    fun `failed model preparation returns home with an error`() {
        val h = Harness()
        h.transcriber.failOnPrepare = true
        h.prepare()
        val state = h.viewModel.uiState.value
        assertEquals(AssessmentPhase.HOME, state.phase)
        assertTrue(state.error?.contains("offline", ignoreCase = true) == true)
    }

    @Test
    fun `recognized answer moves to result with timing stats`() {
        val h = Harness()
        h.prepare()
        h.recordAndStopWith("It's an apple.")

        val state = h.viewModel.uiState.value
        assertEquals(AssessmentPhase.RESULT, state.phase)
        val attempt = state.pendingResult!!
        assertEquals(MatchStatus.RECOGNIZED, attempt.status)
        assertEquals("It's an apple.", attempt.transcript)
        assertEquals(19L, attempt.recordingStats.tapToStartMs) // startAt(20) - tap(1)
        assertEquals(130L, attempt.recordingStats.recordingMs) // stop(150) - start(20)
        assertEquals(55L, attempt.recordingStats.processingMs)
    }

    @Test
    fun `wrong word is not recognized`() {
        val h = Harness()
        h.prepare()
        h.recordAndStopWith("I see a banana.")
        assertEquals(
            MatchStatus.NOT_RECOGNIZED,
            h.viewModel.uiState.value.pendingResult?.status,
        )
    }

    @Test
    fun `empty transcript yields no speech without transcript text`() {
        val h = Harness()
        h.prepare()
        h.recordAndStopWith("")
        val attempt = h.viewModel.uiState.value.pendingResult!!
        assertEquals(MatchStatus.NO_SPEECH, attempt.status)
        assertEquals("", attempt.transcript)
    }

    @Test
    fun `retry keeps the same item and deletes the audio`() {
        val h = Harness()
        h.prepare()
        h.recordAndStopWith("wrong")
        h.viewModel.onRetry()
        val state = h.viewModel.uiState.value
        assertEquals(AssessmentPhase.READY, state.phase)
        assertEquals(0, state.index)
        assertEquals("Apple", state.currentItem?.expectedAnswer)
        assertNull(state.pendingResult)
        assertEquals(listOf(File("rec_1.wav")), h.store.deleted)
    }

    @Test
    fun `next advances items and finishing collects all results`() {
        val h = Harness()
        h.prepare()
        val answers = listOf(
            "it's an apple",
            "banana",
            "mug",
            "behind the chair",
            "a book",
        )
        var expectedRecognized = 0
        var index = 0
        answers.forEach { answer ->
            h.recordAndStopWith(answer)
            val attempt = h.viewModel.uiState.value.pendingResult!!
            if (attempt.status == MatchStatus.RECOGNIZED) expectedRecognized++
            h.viewModel.onNext()
            index++
        }
        val state = h.viewModel.uiState.value
        assertEquals(AssessmentPhase.FINISHED, state.phase)
        assertEquals(answers.size, state.results.size)
        assertEquals(expectedRecognized, state.summary().recognized)
    }

    @Test
    fun `transcription failure returns to ready and deletes audio`() {
        val h = Harness()
        h.prepare()
        h.transcriber.failOnTranscribe = RuntimeException("inference crash")
        h.recordAndStopWith("anything")
        val state = h.viewModel.uiState.value
        assertEquals(AssessmentPhase.READY, state.phase)
        assertTrue(state.error?.contains("Transcription failed") == true)
        assertEquals(listOf(File("rec_1.wav")), h.store.deleted)
    }

    @Test
    fun `replay plays the recorded audio`() {
        val h = Harness()
        h.prepare()
        h.recordAndStopWith("apple")
        h.viewModel.onReplay()
        assertEquals(File("rec_1.wav"), h.player.played)
    }

    @Test
    fun `go home releases the model and resets state`() {
        val h = Harness()
        h.prepare()
        h.recordAndStopWith("apple")
        h.viewModel.goHome()
        assertTrue(h.transcriber.released)
        assertEquals(AssessmentPhase.HOME, h.viewModel.uiState.value.phase)
    }
}