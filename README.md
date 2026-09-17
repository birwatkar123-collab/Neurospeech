# NeuroSpeech

Android app for speech-rehabilitation assessment: a clinician-guided picture-naming task where the client names an object and **on-device Whisper** transcribes their speech, scores it against the target word, and records timing — fully offline after the first model download.

## Why on-device Whisper?

Speech assessment can involve sensitive audio. NeuroSpeech keeps recordings and transcription on the phone:

- **Privacy** — audio never leaves the device; no backend, no account.
- **Offline** — after the Whisper model is downloaded once, the whole session works without internet.
- **Deterministic scoring** — matching is exact (normalized word comparison), not fuzzy or LLM-based, so results are reproducible.

## Features

- 5-word picture-naming assessment (apple, banana, cup, chair, book)
- Tap-to-start speech capture (16 kHz mono WAV via `AudioRecord`)
- On-device Whisper transcription (`ggml-base.en` via `dev.ffmpegkit-maintained:whisper-android`)
- Per-word result: transcript, match status (Recognized / Not recognized / No speech / Error), and timing
- Item audio playback, per-word retry, and a summary review at the end
- Timing surfaces `tapToStartMs / recordingMs / processingMs` — labeled honestly as *app-event* times, **not** reaction-time or speech-onset measures

## Scoring

- Equivalences: case and punctuation ignored, whitespace collapsed, simple contractions expanded (`it's` → `it is`)
- Matching is **exact whole-word token match** only — no fuzzy, spelling, or semantic matching
- `mug` is accepted as a valid synonym for `cup`; the target `cup` remains the canonical answer
- Empty/undecipherable audio → `NO_SPEECH` (hint shown to the client)

## Model handling

The Whisper GGML model is resolved in this order:

1. a previously prepared copy in `filesDir/models`, else
2. a bundled asset at `assets/models/<model>` (relying on Play Asset Delivery in a full build), else
3. a one-time download from Hugging Face (~142 MB), gated by a network check.

Model name/URL are injected via `BuildConfig.MODEL_NAME` / `MODEL_DOWNLOAD_URL`. Once present locally, everything works offline.

## Tech stack

- Kotlin + Jetpack Compose (Material 3), MVVM via `ViewModel` + `StateFlow`
- AGP 8.11.1, Kotlin 2.1.21, Compose BOM 2025.09.00, Gradle 8.14.3
- `minSdk 26`, `targetSdk/compileSdk 36`
- Whisper: `dev.ffmpegkit-maintained:whisper-android` (**arm64-v8a only**)
- Scoped Storage for WAV files (no permissions beyond `RECORD_AUDIO`)

## Architecture

Layers are deliberately separated so the session logic is JVM-testable without Android dependencies:

```
ui/            Compose screens (Home, Preparing, Assessment, Summary)
assessment/    AssessmentViewModel — session state machine, owns all flows
core/          Scoring + timing model (pure Kotlin)
audio/         RecordingControl/RecordingStore/SoundPlayer interfaces
               + AudioRecord-based recorder, WAV writer, playback
whisper/       Transcriber interface + WhisperTranscriber + ModelManager
```

`AssessmentViewModel` depends only on interfaces (`Transcriber`, `RecordingStore`, `SoundPlayer`) and a clock, so unit tests drive the full session with fakes.

## Build & test

```bash
# Build APK
gradlew assembleDebug

# Unit tests (scoring, WAV writer, session state machine — no device needed)
gradlew testDebugUnitTest

# Lint
gradlew :app:lintDebug

# Instrumented tests (requires a physical ARM64 phone; installs via USB)
gradlew connectedDebugAndroidTest
```

> Note: the Whisper native library ships only for `arm64-v8a`, so emulators are not supported.

Android Studio: `Open project` → select the root folder. `local.properties` (machine-specific SDK path) is generated as needed and is not tracked.

## Status

Phase 1A — core picture-naming assessment completed and verified on device (Xiaomi, Android 15):

- `assembleDebug` builds clean; lint clean
- 27 unit tests pass
- 3 instrumented tests pass on device (Home→Start flow + on-device Whisper transcription)
- On the roadmap: target-extraction timing, bundled model variants, shared vocabulary with a companion web app, clinician onboarding, dry-run mode.

## License

No license is specified yet — contact the repository owner before reusing the code.