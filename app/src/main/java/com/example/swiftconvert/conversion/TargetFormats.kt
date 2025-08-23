package com.example.swiftconvert.conversion

enum class Category(val label: String) {
    AUDIO("Audio"),
    VIDEO("Video"),
    IMAGE("Image"),
    PDF("PDF"),
    OTHER("Other")
}

sealed class TargetFormat(val displayName: String, val extension: String) {

    // --- Audio (wide) ---
    data object MP3 : TargetFormat("mp3 (LAME)", "mp3")
    data object M4A_AAC : TargetFormat("m4a (AAC)", "m4a")
    data object M4B_AAC : TargetFormat("m4b (Audiobook/AAC)", "m4b")
    data object AAC_ADTS : TargetFormat("aac (ADTS)", "aac")
    data object OPUS : TargetFormat("opus (WebM/Opus)", "webm")
    data object OGG_VORBIS : TargetFormat("ogg (Vorbis)", "ogg")
    data object FLAC : TargetFormat("flac (lossless)", "flac")
    data object WAV : TargetFormat("wav (PCM 16-bit)", "wav")

    // --- Video (broad) ---
    data object MP4_H264_AAC : TargetFormat("mp4 (H.264/AAC)", "mp4")
    data object MP4_H265_AAC : TargetFormat("mp4 (H.265/AAC)", "mp4")
    data object WEBM_VP9_OPUS : TargetFormat("webm (VP9/Opus)", "webm")
    data object MKV_H264_AAC : TargetFormat("mkv (H.264/AAC)", "mkv")
    data object MOV_H264_AAC : TargetFormat("mov (H.264/AAC)", "mov")
    data object GIF_ANIMATED : TargetFormat("gif (animated)", "gif")

    // --- Images (broad) ---
    data object JPG : TargetFormat("jpg (JPEG)", "jpg")
    data object PNG : TargetFormat("png (PNG)", "png")
    data object WEBP : TargetFormat("webp (WebP)", "webp")
    data object BMP : TargetFormat("bmp (Bitmap)", "bmp")
    data object TIFF : TargetFormat("tiff (TIFF)", "tiff")
    data object HEIC : TargetFormat("heic (HEIF)", "heic")
    data object PDF : TargetFormat("pdf (PDF)", "pdf") // image -> pdf

    // --- PDF / Documents ---
    data object ZIP_IMAGES : TargetFormat("images.zip (PDF pages → PNG)", "zip")
    data object TXT_PDF : TargetFormat("pdf (from .txt)", "pdf")
    data object HTML_PDF : TargetFormat("pdf (from .html/markdown)", "pdf")

    companion object {
        fun defaultFor(category: Category): TargetFormat = when (category) {
            Category.AUDIO -> MP3
            Category.VIDEO -> MP4_H264_AAC
            Category.IMAGE -> JPG
            Category.PDF -> ZIP_IMAGES
            else -> JPG
        }

        fun optionsFor(category: Category): List<TargetFormat> = when (category) {
            Category.AUDIO -> listOf(MP3, M4A_AAC, M4B_AAC, AAC_ADTS, OPUS, OGG_VORBIS, FLAC, WAV)
            Category.VIDEO -> listOf(MP4_H264_AAC, MP4_H265_AAC, WEBM_VP9_OPUS, MKV_H264_AAC, MOV_H264_AAC, GIF_ANIMATED)
            Category.IMAGE -> listOf(JPG, PNG, WEBP, BMP, TIFF, HEIC, PDF)
            Category.PDF -> listOf(ZIP_IMAGES)
            Category.OTHER -> listOf(JPG, PNG, WEBP, BMP, TIFF, HEIC, PDF, TXT_PDF, HTML_PDF)
        }
    }
}
