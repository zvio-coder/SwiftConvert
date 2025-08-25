package com.example.swiftconvert.conversion

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Fallback transcoder using Media3 Transformer.
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
        return transform(context, input, outputFile)
    }

    private suspend fun transform(context: Context, input: Uri, outFile: File): Result {
        outFile.parentFile?.mkdirs()

        val transformer = Transformer.Builder(context).build()
        val mediaItem = MediaItem.fromUri(input)

        return suspendCancellableCoroutine { cont ->
            transformer.start(
                mediaItem,
                outFile.absolutePath,
                object : Transformer.Listener {
                    override fun onCompleted(result: ExportResult) {
                        if (!cont.isCompleted) cont.resume(Result(true, null))
                    }

                    override fun onError(error: ExportException, result: ExportResult) {
                        if (!cont.isCompleted) cont.resume(Result(false, error.message))
                    }
                }
            )
        }
    }
}
