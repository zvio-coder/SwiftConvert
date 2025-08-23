package com.example.swiftconvert.conversion

import android.content.Context
import android.net.Uri
import com.example.swiftconvert.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ConversionResult(
    val outputFile: File,
    val suggestedMime: String,
    val suggestedName: String
)

class ConversionManager(private val context: Context) {

    private val media3 by lazy { Media3Transcoder(context) } // kept for potential fallback
    private val ff by lazy { FFmpegTranscoder(context) }
    private val pdf by lazy { PdfConverter(context) }
    private val textPdf by lazy { SimpleTextToPdf(context) }

    suspend fun convert(input: Uri, inputName: String, category: Category, target: TargetFormat): ConversionResult =
        withContext(Dispatchers.IO) {
            when (category) {
                Category.AUDIO -> convertAudio(input, inputName, target)
                Category.VIDEO -> convertVideo(input, inputName, target)
                Category.IMAGE -> convertImage(input, inputName, target)
                Category.PDF -> convertPdf(input, inputName, target)
                Category.OTHER -> convertOther(input, inputName, target)
            }
        }

    private fun res(out: File, ext: String, name: String, mimeOverride: String? = null) =
        ConversionResult(out, mimeOverride ?: FileUtils.mimeForExt(ext), FileUtils.renameWithExt(name, ext))

    // ---- Audio ----
    private suspend fun convertAudio(input: Uri, inputName: String, target: TargetFormat): ConversionResult {
        val out = File.createTempFile("audio_", ".${target.extension}", context.cacheDir)
        when (target) {
            TargetFormat.MP3 -> ff.audioToMp3(input, out)
            TargetFormat.M4A_AAC -> ff.audioToAacM4a(input, out)
            TargetFormat.M4B_AAC -> ff.audioToM4b(input, out)
            TargetFormat.AAC_ADTS -> ff.audioToAdtsAac(input, out)
            TargetFormat.OPUS -> ff.audioToOpusWebm(input, out)
            TargetFormat.OGG_VORBIS -> ff.audioToOggVorbis(input, out)
            TargetFormat.FLAC -> ff.audioToFlac(input, out)
            TargetFormat.WAV -> ff.audioToWav(input, out)
            else -> error("Unsupported audio target")
        }
        return res(out, target.extension, inputName)
    }

    // ---- Video ----
    private suspend fun convertVideo(input: Uri, inputName: String, target: TargetFormat): ConversionResult {
        val out = File.createTempFile("video_", ".${target.extension}", context.cacheDir)
        when (target) {
            TargetFormat.MP4_H264_AAC -> ff.videoToMp4H264Aac(input, out)
            TargetFormat.MP4_H265_AAC -> ff.videoToMp4H265Aac(input, out)
            TargetFormat.WEBM_VP9_OPUS -> ff.videoToWebmVp9Opus(input, out)
            TargetFormat.MKV_H264_AAC -> ff.videoToMkvH264Aac(input, out)
            TargetFormat.MOV_H264_AAC -> ff.videoToMovH264Aac(input, out)
            TargetFormat.GIF_ANIMATED -> ff.videoToGif(input, out)
            else -> error("Unsupported video target")
        }
        return res(out, target.extension, inputName)
    }

    // ---- Images ----
    private suspend fun convertImage(input: Uri, inputName: String, target: TargetFormat): ConversionResult {
        return when (target) {
            TargetFormat.JPG, TargetFormat.PNG, TargetFormat.WEBP,
            TargetFormat.BMP, TargetFormat.TIFF, TargetFormat.HEIC -> {
                val out = File.createTempFile("img_", ".${target.extension}", context.cacheDir)
                ff.imageReencode(input, out, target.extension)
                res(out, target.extension, inputName)
            }
            TargetFormat.PDF -> {
                val out = File.createTempFile("pdf_", ".pdf", context.cacheDir)
                pdf.imageToPdf(input, out)
                ConversionResult(out, "application/pdf", FileUtils.renameWithExt(inputName, "pdf"))
            }
            else -> error("Unsupported image target")
        }
    }

    // ---- PDF ----
    private suspend fun convertPdf(input: Uri, inputName: String, target: TargetFormat): ConversionResult {
        return when (target) {
            TargetFormat.ZIP_IMAGES -> {
                val out = File.createTempFile("pages_", ".zip", context.cacheDir)
                pdf.pdfToImagesZip(input, out)
                ConversionResult(out, "application/zip", FileUtils.baseName(inputName) + "_images.zip")
            }
            else -> error("Unsupported PDF target")
        }
    }

    // ---- Other / Documents ----
    private suspend fun convertOther(input: Uri, inputName: String, target: TargetFormat): ConversionResult {
        val lower = inputName.lowercase()
        return when (target) {
            TargetFormat.TXT_PDF, TargetFormat.HTML_PDF -> {
                val out = File.createTempFile("doc_", ".pdf", context.cacheDir)
                textPdf.textFileToPdf(input, out) // simple text rendering
                ConversionResult(out, "application/pdf", FileUtils.renameWithExt(inputName, "pdf"))
            }
            else -> {
                // Try image path first; otherwise guess using mime
                try {
                    convertImage(input, inputName, target)
                } catch (_: Exception) {
                    val mime = context.contentResolver.getType(input) ?: ""
                    val cat = FileUtils.categoryFor(mime, inputName)
                    when (cat) {
                        Category.AUDIO -> convertAudio(input, inputName, target)
                        Category.VIDEO -> convertVideo(input, inputName, target)
                        Category.PDF -> convertPdf(input, inputName, target)
                        else -> throw IllegalArgumentException("Unsupported file type or target")
                    }
                }
            }
        }
    }
}
