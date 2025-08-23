package com.example.swiftconvert.conversion

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.swiftconvert.util.FileUtils
import java.io.File

/**
 * FFmpeg-based conversions to cover MANY formats (including .m4b audiobooks).
 * We copy the content Uri to a temp file before invoking ffmpeg for reliability.
 */
class FFmpegTranscoder(private val context: Context) {

    private fun assertSuccess(sessionId: Long) {
        val session = FFmpegKit.getSession(sessionId)
        val rc = session?.returnCode
        if (rc == null || !ReturnCode.isSuccess(rc)) {
            val log = session?.allLogsAsString ?: "no ffmpeg logs"
            throw IllegalStateException("FFmpeg failed (${rc?.value}): $log")
        }
    }

    private fun run(cmd: String): Long = FFmpegKit.execute(cmd).sessionId

    // ---------- Audio ----------
    fun audioToMp3(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a libmp3lame -q:a 2 ${out.absolutePath}"))
    }

    fun audioToAacM4a(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a aac -b:a 192k ${out.absolutePath}"))
    }

    fun audioToM4b(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        // M4B is typically AAC in MP4 container with .m4b extension; chapters optional.
        // You can map chapters with additional -map_metadata if source has them.
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a aac -b:a 160k -f mp4 ${out.absolutePath}"))
    }

    fun audioToAdtsAac(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a aac -b:a 192k ${out.absolutePath}"))
    }

    fun audioToOpusWebm(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a libopus -b:a 128k ${out.absolutePath}"))
    }

    fun audioToOggVorbis(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a libvorbis -q:a 5 ${out.absolutePath}"))
    }

    fun audioToFlac(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a flac ${out.absolutePath}"))
    }

    fun audioToWav(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -vn -c:a pcm_s16le ${out.absolutePath}"))
    }

    // ---------- Video ----------
    fun videoToMp4H264Aac(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -c:v libx264 -pix_fmt yuv420p -crf 21 -preset veryfast -c:a aac -b:a 192k ${out.absolutePath}"))
    }

    fun videoToMp4H265Aac(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -c:v libx265 -pix_fmt yuv420p -crf 26 -preset medium -c:a aac -b:a 160k ${out.absolutePath}"))
    }

    fun videoToWebmVp9Opus(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -c:v libvpx-vp9 -b:v 0 -crf 32 -row-mt 1 -c:a libopus -b:a 128k ${out.absolutePath}"))
    }

    fun videoToMkvH264Aac(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -c:v libx264 -pix_fmt yuv420p -crf 21 -preset veryfast -c:a aac -b:a 192k ${out.absolutePath}"))
    }

    fun videoToMovH264Aac(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        assertSuccess(run("-y -i ${inFile.absolutePath} -c:v libx264 -pix_fmt yuv420p -crf 21 -preset veryfast -c:a aac -b:a 192k ${out.absolutePath}"))
    }

    fun videoToGif(input: Uri, out: File) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        val palette = File.createTempFile("pal_", ".png", context.cacheDir)
        assertSuccess(run("-y -i ${inFile.absolutePath} -vf fps=12,scale=iw:-1:flags=lanczos,palettegen ${palette.absolutePath}"))
        assertSuccess(run("-y -i ${inFile.absolutePath} -i ${palette.absolutePath} -lavfi fps=12,scale=iw:-1:flags=lanczos[x];[x][1:v]paletteuse ${out.absolutePath}"))
        palette.delete()
    }

    // ---------- Images ----------
    fun imageReencode(input: Uri, out: File, targetExt: String) {
        val inFile = FileUtils.copyToTemp(context, input, ".in")
        val extra = when (targetExt.lowercase()) {
            "jpg", "jpeg" -> "-vf format=rgb24 -q:v 2"
            "png" -> ""
            "webp" -> "-qscale 80"
            "bmp" -> ""
            "tiff", "tif" -> ""
            "heic", "heif" -> "" // encoding requires libheif in build
            else -> ""
        }
        assertSuccess(run("-y -i ${inFile.absolutePath} $extra ${out.absolutePath}"))
    }
}
