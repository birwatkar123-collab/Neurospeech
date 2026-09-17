package com.neurospeech.app.whisper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.neurospeech.app.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Makes the Whisper GGML model available on-device.
 *
 * Resolution order:
 *  1. a previously prepared copy in filesDir/models,
 *  2. a bundled asset at assets/models/<model>,
 *  3. a one-time download from the Hugging Face mirror.
 */
object ModelManager {

    private const val MIN_VALID_SIZE = 1_000_000L

    fun modelFileName(): String = "ggml-${BuildConfig.MODEL_NAME}.bin"

    fun modelFile(context: Context): File =
        File(File(context.filesDir, "models"), modelFileName())

    fun isModelReady(context: Context): Boolean {
        val f = modelFile(context)
        return f.exists() && f.length() > MIN_VALID_SIZE
    }

    suspend fun ensureModel(
        context: Context,
        onProgress: ((downloadedBytes: Long) -> Unit)? = null,
    ): File = withContext(Dispatchers.IO) {
        val file = modelFile(context)
        file.parentFile?.mkdirs()
        if (isModelReady(context)) return@withContext file

        copyFromAssetIfPresent(context, file)
        if (isModelReady(context)) return@withContext file

        download(context, file, onProgress)
        check(isModelReady(context)) { "Downloaded model file is invalid or incomplete" }
        file
    }

    private fun copyFromAssetIfPresent(context: Context, target: File) {
        try {
            val asset = "models/${modelFileName()}"
            context.assets.open(asset).use { src ->
                val tmp = File(target.parentFile, target.name + ".asset.tmp")
                tmp.outputStream().use { dst -> src.copyTo(dst) }
                if (tmp.length() > MIN_VALID_SIZE) {
                    target.delete()
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                } else {
                    tmp.delete()
                }
            }
        } catch (_: Exception) {
            // No bundled asset; fall through to download.
        }
    }

    private fun download(
        context: Context,
        target: File,
        onProgress: ((Long) -> Unit)?,
    ) {
        require(isOnline(context)) {
            "No internet connection and no local speech model is available. " +
                "Connect once to download the model, then it will work offline."
        }
        val url = URL(BuildConfig.MODEL_DOWNLOAD_URL)
        val part = File(target.parentFile, target.name + ".part")
        url.openConnection().getInputStream().use { input ->
            FileOutputStream(part).use { out ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    total += read
                    onProgress?.invoke(total)
                }
            }
        }
        part.renameTo(target)
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}