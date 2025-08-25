package com.example.swiftconvert.conversion

import android.content.Context
import android.net.Uri
import com.example.swiftconvert.util.DocumentIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level orchestrator that prefers FFmpeg (when available),
 * with Media3 fallback for common formats.
 */
object Converter {

    data class Outcome(
        val success: Boolean,
        val outputFile: File? = null,
        val message: String
    )

    /**
     * Convert an input content Uri to the desired extension.
     * Supported examples:
     *  - .m4b -> .m4a (FFmpeg preferred; Media3 may also handle some)
     *  - .wav -> .m4a
     *  - .mov/.mkv -> .mp4
     */
    suspend fun convert(context: Context, inUri: Uri, outExt: String): Outcome = withContext(Dispatchers.IO) {
        val cacheIn = DocumentIo.copyUriToCache(context, inUri) ?: return@withContext Outcome(false, null, "Failed to read input")
        val outFile = File(context.cacheDir, "convert/${cacheIn.nameWithoutExtension}.${outExt.trimStart('.')}")
        outFile.parentFile?.mkdirs()

        // Build a basic ffmpeg command for common cases
        val extLower = outExt.lowercase()
        val cmd = when (extLower) {
            "m4a" -> " -y -i \"${cacheIn.absolutePath}\" -c:a aac -b:a 128k \"${outFile.absolutePath}\""
            "mp3" -> " -y -i \"${cacheIn.absolutePath}\" -c:a libmp3lame -qscale:a 2 \"${outFile.absolutePath}\""
            "mp4" -> " -y -i \"${cacheIn.absolutePath}\" -c:v libx264 -preset veryfast -crf 23 -c:a aac -b:a 128k \"${outFile.absolutePath}\""
            else  -> " -y -i \"${cacheIn.absolutePath}\" \"${outFile.absolutePath}\""
        }

        // Try FFmpeg first (if available)
        val ff = FFmpegTranscoder.execute(cmd)
        if (ff.success) {
            return@withContext Outcome(true, outFile, "FFmpeg success: ${ff.returnCodeLabel}")
        }

        // If FFmpeg unavailable, fallback to Media3 for common mappings
        val media3Result = when (extLower) {
            "mp4" -> Media3Transcoder.transcodeToMp4(context, inUri, outFile)
            "m4a" -> Media3Transcoder.transcodeAudioToAac(context, inUri, outFile)
            else  -> Media3Transcoder.transcodeToMp4(context, inUri, outFile) // best-effort
        }
        if (media3Result.success) {
            return@withContext Outcome(true, outFile, "Media3 success")
        }

        Outcome(false, null, "FFmpeg unavailable (${ff.error ?: "no AAR"}) and Media3 failed (${media3Result.error ?: "unknown"})")
    }
}
