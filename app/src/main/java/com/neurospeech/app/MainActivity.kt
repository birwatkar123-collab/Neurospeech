package com.neurospeech.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neurospeech.app.ui.NeuroSpeechApp
import com.neurospeech.app.ui.theme.NeuroSpeechTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeuroSpeechTheme {
                NeuroSpeechApp((application as NeuroSpeechApplication).container)
            }
        }
    }
}