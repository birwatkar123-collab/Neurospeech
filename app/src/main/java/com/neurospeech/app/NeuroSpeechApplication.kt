package com.neurospeech.app

import android.app.Application

class NeuroSpeechApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}