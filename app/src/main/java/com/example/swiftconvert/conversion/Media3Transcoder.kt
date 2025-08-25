package com.example.swiftconvert.conversion

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Lightweight fallback transcoder using Media3 Transformer.
 * - Works for many common audio/video transforms (e.g., mp4/h264, aac).
 * - Not as wide as FFmpeg, but avoids external deps.
 */
object Media3Transcoder {

    data class Result(
        val success: Boolean,
        val error: String? = null
    )

    suspend fun transcodeToMp4(context: Context, input: Uri, outputFile: File): Result {
        return transform(context, input, outputFile)
    }

    suspend fun transcodeAudioToAac(context: Context, input: Uri, outputFile: File): Result {
        // Media3 chooses defaults; output extension should be .m4a or .mp4 for AAC
        return transform(context, input, outputFile)
    }

    private suspend fun transform(context: Context, input: Uri, outFile: File): Result {
        // Ensure parent dirs exist
        outFile.parentFile?.mkdirs()

        val transformer = Transformer.Builder(context).build()
        val mediaItem = MediaItem.fromUri(input)

        return suspendCancellableCoroutine { cont ->
            val request = transformer.scheduleRequest(
                Transformer.Request.Builder(/* input */ mediaItem, /* outputPath */ outFile.absolutePath).build(),
                object : Transformer.Listener {
                    override fun onCompleted(composition: String, exportResult: Transformer.ExportResult) {
                        if (!cont.isCompleted) cont.resume(Result(true, null))
                    }

                    override fun onError(composition: String, exportResult: Transformer.ExportResult, exception: Exception) {
                        if (!cont.isCompleted) cont.resume(Result(false, exception.message))
                    }
                }
            )
            cont.invokeOnCancellation { request.cancel() }
        }
    }
}
