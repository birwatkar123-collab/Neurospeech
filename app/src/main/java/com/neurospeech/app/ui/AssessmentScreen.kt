package com.neurospeech.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neurospeech.app.assessment.AssessmentPhase
import com.neurospeech.app.assessment.AssessmentUiState
import com.neurospeech.app.assessment.AssessmentViewModel
import com.neurospeech.app.assessment.ItemAttempt
import com.neurospeech.app.core.Formatting
import com.neurospeech.app.core.MatchStatus

@Composable
fun AssessmentScreen(
    state: AssessmentUiState,
    viewModel: AssessmentViewModel,
    modifier: Modifier = Modifier,
) {
    val item = state.currentItem ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TopBar(
            progress = state.progressText,
            onExit = viewModel::goHome,
            onBack = { if (state.phase == AssessmentPhase.LISTENING) viewModel.onCancelListening() },
            showBack = state.phase == AssessmentPhase.LISTENING || state.phase == AssessmentPhase.PROCESSING,
        )

        Spacer(Modifier.height(12.dp))

        ItemCard(item.imageRes, item.expectedAnswer)

        Spacer(Modifier.height(20.dp))

        when (state.phase) {
            AssessmentPhase.READY -> StartListeningButton(viewModel::onStartListening)
            AssessmentPhase.LISTENING -> ListeningPanel(viewModel::onStopListening)
            AssessmentPhase.PROCESSING -> ProcessingPanel()
            AssessmentPhase.RESULT -> state.pendingResult?.let { result ->
                ResultPanel(
                    result = result,
                    nextLabel = if (state.isLastItem) "Finish" else "Next",
                    onReplay = viewModel::onReplay,
                    onRetry = viewModel::onRetry,
                    onNext = viewModel::onNext,
                )
            }
            else -> Unit
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "These timings describe app events and do not measure reaction time or speech onset.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TopBar(progress: String, onExit: () -> Unit, onBack: () -> Unit, showBack: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Stop listening")
            }
        }
        Text(
            text = progress,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit assessment")
        }
    }
}

@Composable
private fun ItemCard(imageRes: Int, expectedAnswer: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Picture to name",
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "What is this?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StartListeningButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null)
        Spacer(Modifier.size(12.dp))
        Text("Start Listening", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ListeningPanel(onStop: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PulseIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Listening…",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Say the name of the picture, then tap Stop.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(Modifier.size(12.dp))
            Text("Stop Recording", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PulseIndicator() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha(alpha)
                .scale(scale),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ProcessingPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Processing your answer…",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ResultPanel(
    result: ItemAttempt,
    nextLabel: String,
    onReplay: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StatusCard(result)

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onReplay,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Replay")
            }
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Try Again")
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(nextLabel, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(10.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun StatusCard(result: ItemAttempt) {
    val (statusText, statusColor, statusIcon) = when (result.status) {
        MatchStatus.RECOGNIZED -> Triple("Recognized", MaterialTheme.colorScheme.secondary, null)
        MatchStatus.NOT_RECOGNIZED -> Triple(
            "Response Not Automatically Recognized",
            MaterialTheme.colorScheme.tertiary,
            null,
        )
        MatchStatus.NO_SPEECH -> Triple("No Speech Detected", MaterialTheme.colorScheme.onSurfaceVariant, null)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    modifier = Modifier.weight(1f),
                )
            }

            Divider()

            InfoRow(label = "Expected answer", value = result.item.expectedAnswer)
            InfoRow(label = "What was heard", value = result.transcript.ifEmpty { "—" })
            Divider()
            InfoRow(
                label = "Time to start recording",
                value = Formatting.millis(result.recordingStats.tapToStartMs),
            )
            InfoRow(
                label = "Recording duration",
                value = Formatting.duration(result.recordingStats.recordingMs),
            )
            InfoRow(
                label = "Processing time",
                value = Formatting.millis(result.recordingStats.processingMs),
            )
        }
    }
}

@Composable
private fun Divider() {
    Spacer(Modifier.height(1.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}