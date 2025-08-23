// Optional fallback using Media3 Transformer (kept for reference).
package com.example.swiftconvert.conversion

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Media3Transcoder(private val context: Context) {

    private fun transformerFor(request: TransformationRequest): Transformer {
        return Transformer.Builder(context)
            .setTransformationRequest(request)
            .build()
    }

    @Suppress("unused")
    suspend fun audioToAacM4a(input: Uri, outFile: File) {
        val request = TransformationRequest.Builder()
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()
        transform(input, outFile, request)
    }

    private suspend fun transform(input: Uri, outFile: File, request: TransformationRequest) =
        suspendCancellableCoroutine { cont ->
            val mediaItem = MediaItem.fromUri(input)
            val transformer = transformerFor(request)
            transformer.start(mediaItem, outFile.absolutePath)

            val progressHolder = ProgressHolder()
            val thread = Thread {
                try {
                    while (true) {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_COMPLETED) {
                            cont.resume(Unit); break
                        }
                        if (state == Transformer.PROGRESS_STATE_NO_TRANSFORMATION) {
                            throw ExportException(ExportException.ERROR_CODE_UNSPECIFIED, "No transformation applicable")
                        }
                        Thread.sleep(200)
                    }
                } catch (e: Throwable) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
            thread.start()
            cont.invokeOnCancellation { runCatching { thread.interrupt() } }
        }
}
