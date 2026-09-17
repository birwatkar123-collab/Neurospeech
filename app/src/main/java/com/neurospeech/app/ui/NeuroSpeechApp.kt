package com.neurospeech.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neurospeech.app.AppContainer
import com.neurospeech.app.assessment.AssessmentPhase
import com.neurospeech.app.assessment.AssessmentUiState
import com.neurospeech.app.assessment.AssessmentViewModel
import com.neurospeech.app.assessment.ModelPrepState
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun NeuroSpeechApp(container: AppContainer) {
    val viewModel: AssessmentViewModel = viewModel(factory = assessmentViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startAssessment() else viewModel.micPermissionDenied()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.phase) {
            AssessmentPhase.HOME -> HomeScreen(
                state = state,
                onStart = {
                    if (hasMicrophonePermission(context)) {
                        viewModel.startAssessment()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
            AssessmentPhase.PREPARING -> PreparingScreen(state.modelPrep)
            AssessmentPhase.READY,
            AssessmentPhase.LISTENING,
            AssessmentPhase.PROCESSING,
            AssessmentPhase.RESULT,
            -> AssessmentScreen(state = state, viewModel = viewModel)
            AssessmentPhase.FINISHED -> SummaryScreen(state = state, onHome = viewModel::goHome)
        }

        state.error?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = viewModel::dismissError,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        }
    }
}

private fun hasMicrophonePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

fun assessmentViewModelFactory(container: AppContainer) = viewModelFactory {
    initializer {
        AssessmentViewModel(
            transcriber = container.transcriber,
            recordingStore = container.recordingStore,
            soundPlayer = container.soundPlayer,
            recorderFactory = container.recorderFactory,
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}