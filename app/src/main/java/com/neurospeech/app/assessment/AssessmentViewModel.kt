package com.neurospeech.app.assessment

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurospeech.app.audio.RecordingControl
import com.neurospeech.app.audio.RecordingStore
import com.neurospeech.app.audio.SoundPlayer
import com.neurospeech.app.core.MatchStatus
import com.neurospeech.app.core.RecordingStats
import com.neurospeech.app.core.TargetMatcher
import com.neurospeech.app.whisper.Transcriber
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssessmentViewModel(
private val transcriber: Transcriber,
    private val recordingStore: RecordingStore,
    private val soundPlayer: SoundPlayer,
    private val recorderFactory: () -> RecordingControl,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssessmentUiState())
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    private var activeRecorder: RecordingControl? = null
    private var currentSoundFile: File? = null
    private var listeningRequestedAt: Long = 0
    private var recordingStartedAt: Long = 0

    // ---- Model preparation -------------------------------------------------

    fun startAssessment() {
        val state = _uiState.value
        if (state.phase == AssessmentPhase.PREPARING || state.phase == AssessmentPhase.LISTENING) return

        _uiState.update {
            it.copy(
                phase = AssessmentPhase.PREPARING,
                results = emptyList(),
                index = 0,
                pendingResult = null,
                error = null,
                modelPrep = ModelPrepState.Idle,
            )
        }
        viewModelScope.launch {
            runCatching {
                transcriber.ensureModel { bytes ->
                    _uiState.update { s -> s.copy(modelPrep = ModelPrepState.Downloading(bytes)) }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(phase = AssessmentPhase.READY, modelPrep = ModelPrepState.Ready, error = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        phase = AssessmentPhase.HOME,
                        modelPrep = ModelPrepState.Failed(error.message ?: "Unable to prepare speech model"),
                        error = error.message ?: "Unable to prepare the speech model.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // ---- Recording ---------------------------------------------------------

    fun onStartListening() {
        val state = _uiState.value
        val item = state.currentItem ?: return
        if (state.phase != AssessmentPhase.READY) return

        val file = recordingStore.nextFile()
        val recorder = recorderFactory()
        listeningRequestedAt = clock()
        val startedAt = runCatching { recorder.start(file) }.getOrElse { error ->
            _uiState.update {
                it.copy(
                    phase = AssessmentPhase.READY,
                    error = error.message ?: "Could not start the microphone.",
                )
            }
            return
        }

        activeRecorder = recorder
        currentSoundFile = file
        recordingStartedAt = startedAt
        _uiState.update { it.copy(phase = AssessmentPhase.LISTENING, error = null) }
    }

    fun onStopListening() {
        val state = _uiState.value
        if (state.phase != AssessmentPhase.LISTENING) return
        val item = state.currentItem ?: return

        activeRecorder?.stop()
        activeRecorder = null
        val file = currentSoundFile
        if (file == null) {
            _uiState.update { it.copy(phase = AssessmentPhase.READY) }
            return
        }

        val recordingMs = clock() - recordingStartedAt
        val tapToStartMs = recordingStartedAt - listeningRequestedAt

        _uiState.update { it.copy(phase = AssessmentPhase.PROCESSING) }
        viewModelScope.launch {
            runCatching { transcriber.transcribe(file) }
                .onSuccess { result ->
                    val status = TargetMatcher.match(result.text, item.acceptedAnswers)
                    val transcript = if (status == MatchStatus.NO_SPEECH) "" else result.text
                    val attempt = ItemAttempt(
                        item = item,
                        soundFile = file,
                        status = status,
                        transcript = transcript,
                        recordingStats = RecordingStats(
                            tapToStartMs = maxOf(0L, tapToStartMs),
                            recordingMs = maxOf(0L, recordingMs),
                            processingMs = maxOf(0L, result.processingMs),
                        ),
                    )
                    _uiState.update {
                        it.copy(phase = AssessmentPhase.RESULT, pendingResult = attempt)
                    }
                }
                .onFailure { error ->
                    recordingStore.delete(file)
                    currentSoundFile = null
                    _uiState.update {
                        it.copy(
                            phase = AssessmentPhase.READY,
                            error = "Transcription failed: ${error.message ?: "unknown error"}",
                        )
                    }
                }
        }
    }

    fun onCancelListening() {
        activeRecorder?.stop()
        activeRecorder = null
        currentSoundFile?.let { recordingStore.delete(it) }
        currentSoundFile = null
        _uiState.update { it.copy(phase = AssessmentPhase.READY) }
    }

    // ---- Result navigation -------------------------------------------------

    fun onReplay() {
        val attempt = _uiState.value.pendingResult ?: return
        soundPlayer.play(attempt.soundFile)
    }

    fun onRetry() {
        val attempt = _uiState.value.pendingResult ?: return
        recordingStore.delete(attempt.soundFile)
        currentSoundFile = null
        _uiState.update {
            it.copy(phase = AssessmentPhase.READY, pendingResult = null, error = null)
        }
    }

    fun onNext() {
        val state = _uiState.value
        val attempt = state.pendingResult ?: return
        val updatedResults = state.results.toMutableList().apply { add(attempt) }
        currentSoundFile = null
        if (state.isLastItem) {
            _uiState.update {
                it.copy(
                    phase = AssessmentPhase.FINISHED,
                    results = updatedResults,
                    pendingResult = null,
                    index = state.index + 1,
                    error = null,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    phase = AssessmentPhase.READY,
                    index = state.index + 1,
                    results = updatedResults,
                    pendingResult = null,
                    error = null,
                )
            }
        }
    }

    fun goHome() {
        activeRecorder?.stop()
        activeRecorder = null
        currentSoundFile?.let { recordingStore.delete(it) }
        currentSoundFile = null
        transcriber.releaseModel()
        _uiState.value = AssessmentUiState()
    }

    fun micPermissionDenied() {
        if (_uiState.value.phase == AssessmentPhase.HOME) {
            _uiState.update {
                it.copy(error = "Microphone permission is required to record answers.")
            }
        }
    }

    override fun onCleared() {
        activeRecorder?.stop()
        soundPlayer.release()
        transcriber.releaseModel()
    }
}

