package com.example.swiftconvert.conversion

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PdfConverter(private val context: Context) {

    fun imageToPdf(input: Uri, outFile: File) {
        val bmp = context.contentResolver.openInputStream(input)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Unable to decode image")
        val pdf = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
            val page = pdf.startPage(pageInfo)
            page.canvas.drawColor(Color.WHITE)
            val src = Rect(0, 0, bmp.width, bmp.height)
            val dst = Rect(0, 0, bmp.width, bmp.height)
            page.canvas.drawBitmap(bmp, src, dst, null)
            pdf.finishPage(page)
            FileOutputStream(outFile).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
            bmp.recycle()
        }
    }

    fun pdfToImagesZip(input: Uri, outFile: File) {
        val tmpPdf = File.createTempFile("in_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(input)?.use { ins ->
            tmpPdf.outputStream().use { ins.copyTo(it) }
        } ?: error("Unable to open PDF")
        val pfd = ParcelFileDescriptor.open(tmpPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zip ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val width = page.width
                        val height = page.height
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val entry = ZipEntry("page_${i + 1}.png")
                        zip.putNextEntry(entry)
                        val bos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
                        zip.write(bos.toByteArray())
                        zip.closeEntry()
                        bmp.recycle()
                    }
                }
            }
        } finally {
            renderer.close()
            pfd.close()
            tmpPdf.delete()
        }
    }
}
