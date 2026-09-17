# Keep the Whisper AAR model loader / JNI surface reachable through shrinking.
-keep class dev.ffmpegkit.whisper.** { *; }
-keepclasseswithmembers class dev.ffmpegkit.whisper.** {
    native <methods>;
}

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep default enum/serialized state used by the assessment session.
-keepclassmembers enum com.neurospeech.app.** { *; }