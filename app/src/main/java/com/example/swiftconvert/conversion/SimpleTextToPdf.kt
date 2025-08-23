package com.example.swiftconvert.conversion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.swiftconvert.util.FileUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal, dependency-free text → PDF. Renders plain text with simple wrapping.
 * Used for .txt, .html, .md inputs (rendered as plain text).
 */
class SimpleTextToPdf(private val context: Context) {

    fun textFileToPdf(input: Uri, outFile: File) {
        val text = FileUtils.readText(context, input)
        renderText(text, outFile)
    }

    private fun renderText(text: String, outFile: File) {
        val pdf = PdfDocument()
        val pageWidth = 612 // 8.5in * 72
        val pageHeight = 792 // 11in * 72
        val margin = 40
        val contentWidth = pageWidth - margin * 2

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.SANS_SERIF
        }
        val lineHeight = (paint.fontSpacing)

        var y = margin.toFloat()
        var pageNumber = 1

        fun newPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdf.startPage(pageInfo)
            y = margin.toFloat()
            return page
        }

        var page = newPage()
        val canvas: Canvas = page.canvas

        val words = text.replace("\r", "").split("\n")
        for ((idx, para) in words.withIndex()) {
            if (para.isEmpty()) {
                y += lineHeight
                if (y > pageHeight - margin) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = newPage()
                }
                continue
            }
            var line = ""
            for (word in para.split(" ")) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) > contentWidth) {
                    canvas.drawText(line, margin.toFloat(), y, paint)
                    y += lineHeight
                    line = word
                    if (y > pageHeight - margin) {
                        pdf.finishPage(page)
                        pageNumber++
                        page = newPage()
                    }
                } else {
                    line = test
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += lineHeight
                if (y > pageHeight - margin) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = newPage()
                }
            }
        }

        pdf.finishPage(page)
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
    }
}
