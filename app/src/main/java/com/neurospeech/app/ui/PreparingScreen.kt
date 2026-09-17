package com.neurospeech.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neurospeech.app.assessment.ModelPrepState
import com.neurospeech.app.core.Formatting

@Composable
fun PreparingScreen(prep: ModelPrepState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Preparing the speech engine…",
            style = MaterialTheme.typography.titleMedium,
        )
        when (prep) {
            is ModelPrepState.Downloading -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Downloading the speech model once (${Formatting.bytes(prep.bytes)}). " +
                        "Audio is never sent anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "This can take a moment the first time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}