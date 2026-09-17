package com.neurospeech.app.assessment

import com.neurospeech.app.core.AssessmentCatalog
import com.neurospeech.app.core.AssessmentItem
import com.neurospeech.app.core.MatchStatus
import com.neurospeech.app.core.RecordingStats
import java.io.File

enum class AssessmentPhase { HOME, PREPARING, READY, LISTENING, PROCESSING, RESULT, FINISHED }

sealed interface ModelPrepState {
    data object Idle : ModelPrepState
    data class Downloading(val bytes: Long) : ModelPrepState
    data object Ready : ModelPrepState
    data class Failed(val message: String) : ModelPrepState
}

/** The captured + transcribed outcome for a single item attempt. */
data class ItemAttempt(
    val item: AssessmentItem,
    val soundFile: File,
    val status: MatchStatus,
    val transcript: String,
    val recordingStats: RecordingStats,
)

data class AssessmentUiState(
    val phase: AssessmentPhase = AssessmentPhase.HOME,
    val items: List<AssessmentItem> = AssessmentCatalog.items,
    val index: Int = 0,
    val results: List<ItemAttempt> = emptyList(),
    val pendingResult: ItemAttempt? = null,
    val modelPrep: ModelPrepState = ModelPrepState.Idle,
    val error: String? = null,
) {
    val currentItem: AssessmentItem? get() = items.getOrNull(index)
    val progressText: String
        get() = if (items.isEmpty()) "" else "${index + 1} of ${items.size}"
    val isLastItem: Boolean get() = index >= items.size - 1

    fun summary(): GroupSummary {
        val recognized = results.count { it.status == MatchStatus.RECOGNIZED }
        val notRecognized = results.count { it.status == MatchStatus.NOT_RECOGNIZED }
        val noSpeech = results.count { it.status == MatchStatus.NO_SPEECH }
        return GroupSummary(results.size, recognized, notRecognized, noSpeech)
    }
}

data class GroupSummary(
    val total: Int,
    val recognized: Int,
    val notRecognized: Int,
    val noSpeech: Int,
)