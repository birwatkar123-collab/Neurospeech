package com.neurospeech.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neurospeech.app.assessment.AssessmentUiState
import com.neurospeech.app.assessment.ItemAttempt
import com.neurospeech.app.core.MatchStatus
import com.neurospeech.app.core.Formatting

@Composable
fun SummaryScreen(state: AssessmentUiState, onHome: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Practice Complete",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        val summary = state.summary()
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryCount("Recognized", summary.recognized, MaterialTheme.colorScheme.secondary)
                SummaryCount("Not recognized", summary.notRecognized, MaterialTheme.colorScheme.tertiary)
                SummaryCount("No speech", summary.noSpeech, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Item results", style = MaterialTheme.typography.titleMedium)

        state.results.forEach { attempt ->
            Spacer(Modifier.height(8.dp))
            AttemptRow(attempt)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "A machine result is not a diagnosis. Recognition errors do not indicate " +
                "any difficulty with your speech.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Back to Home", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", style = MaterialTheme.typography.headlineMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun statusDotColor(status: MatchStatus): Color = when (status) {
    MatchStatus.RECOGNIZED -> Color(0xFF2E7D32)
    MatchStatus.NOT_RECOGNIZED -> Color(0xFFB26A00)
    MatchStatus.NO_SPEECH -> Color(0xFF757575)
}

private fun statusText(status: MatchStatus): String = when (status) {
    MatchStatus.RECOGNIZED -> "Recognized"
    MatchStatus.NOT_RECOGNIZED -> "Not automatically recognized"
    MatchStatus.NO_SPEECH -> "No speech detected"
}

@Composable
private fun AttemptRow(attempt: ItemAttempt) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusDotColor(attempt.status)),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.item.expectedAnswer,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = statusText(attempt.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Formatting.duration(attempt.recordingStats.recordingMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}