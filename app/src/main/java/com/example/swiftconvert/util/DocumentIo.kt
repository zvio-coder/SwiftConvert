package com.example.swiftconvert.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object DocumentIo {

    fun getDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    fun copyUriToCache(context: Context, uri: Uri): File? {
        val name = getDisplayName(context.contentResolver, uri) ?: "input.bin"
        val dest = File(context.cacheDir, "input/$name").apply { parentFile?.mkdirs() }
        return try {
            context.contentResolver.openInputStream(uri)?.use { inS ->
                dest.outputStream().use { outS -> inS.copyTo(outS) }
            }
            dest
        } catch (_: Throwable) {
            null
        }
    }
}
