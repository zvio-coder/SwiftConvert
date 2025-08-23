package com.example.swiftconvert.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.swiftconvert.conversion.Category
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun displayName(cr: ContentResolver, uri: Uri): String? {
        var name: String? = null
        runCatching {
            cr.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1 && c.moveToFirst()) name = c.getString(idx)
            }
        }
        return name ?: uri.lastPathSegment
    }

    fun baseName(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    fun renameWithExt(name: String, ext: String): String {
        val base = baseName(name)
        val safeExt = if (ext.startsWith(".")) ext.drop(1) else ext
        return "$base.$safeExt"
    }

    fun mimeForExt(ext: String): String = when (ext.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "tif", "tiff" -> "image/tiff"
        "heic", "heif" -> "image/heic"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "gif" -> "image/gif"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "m4b" -> "audio/mp4" // many apps use audio/mp4; some use audio/x-m4b
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        else -> "application/octet-stream"
    }

    fun categoryFor(mime: String, name: String?): Category {
        val lower = mime.lowercase()
        val nameLower = name?.lowercase() ?: ""
        return when {
            lower.startsWith("audio/") -> Category.AUDIO
            lower.startsWith("video/") -> Category.VIDEO
            lower.startsWith("image/") -> Category.IMAGE
            lower == "application/pdf" || nameLower.endsWith(".pdf") -> Category.PDF
            // Heuristics for common audio by extension (helps when provider gives octet-stream)
            nameLower.endsWith(".m4b") || nameLower.endsWith(".mp3") || nameLower.endsWith(".m4a") ||
                    nameLower.endsWith(".aac") || nameLower.endsWith(".flac") || nameLower.endsWith(".wav") ||
                    nameLower.endsWith(".ogg") || nameLower.endsWith(".opus") -> Category.AUDIO
            else -> Category.OTHER
        }
    }

    fun copyToTemp(context: Context, uri: Uri, suffix: String): File {
        val name = displayName(context.contentResolver, uri) ?: "in"
        val ext = name.substringAfterLast('.', "")
        val temp = if (ext.isNotBlank()) {
            File.createTempFile("in_", ".$ext", context.cacheDir)
        } else {
            File.createTempFile("in_", suffix, context.cacheDir)
        }
        context.contentResolver.openInputStream(uri)?.use { ins ->
            FileOutputStream(temp).use { outs -> ins.copyTo(outs) }
        } ?: error("Unable to open input stream")
        return temp
    }

    fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: ""
}
