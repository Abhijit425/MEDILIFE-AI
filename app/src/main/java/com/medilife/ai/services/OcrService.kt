package com.medilife.ai.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** On-device OCR with ML Kit (bundled model — no network). */
object OcrService {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun fromBitmap(bmp: Bitmap): String {
        val result = recognizer.process(InputImage.fromBitmap(bmp, 0)).await()
        return toRows(result)
    }

    suspend fun fromImageUri(ctx: Context, uri: Uri): String {
        val bmp = withContext(Dispatchers.IO) {
            val src = ImageDecoder.createSource(ctx.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { d, _, _ ->
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                d.isMutableRequired = false
            }
        }
        return fromBitmap(bmp)
    }

    suspend fun fromPdfUri(ctx: Context, uri: Uri, maxPages: Int = 8): String {
        val pages = withContext(Dispatchers.IO) {
            val out = mutableListOf<Bitmap>()
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { r ->
                    for (i in 0 until minOf(r.pageCount, maxPages)) {
                        r.openPage(i).use { p ->
                            val scale = 2
                            val bmp = Bitmap.createBitmap(p.width * scale, p.height * scale, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            out += bmp
                        }
                    }
                }
            }
            out
        }
        return pages.mapIndexed { i, b -> "--- Page ${i + 1} ---\n" + fromBitmap(b) }.joinToString("\n")
    }

    /** Re-assembles table rows: groups OCR lines whose vertical centres align, then sorts left→right. */
    private fun toRows(text: Text): String {
        data class L(val s: String, val cx: Int, val cy: Int, val h: Int)
        val lines = text.textBlocks.flatMap { b -> b.lines }.mapNotNull { l ->
            val box = l.boundingBox ?: return@mapNotNull null
            L(l.text, box.left, box.centerY(), box.height())
        }.sortedBy { it.cy }
        if (lines.isEmpty()) return text.text
        val rows = mutableListOf<MutableList<L>>()
        for (l in lines) {
            val row = rows.lastOrNull()
            if (row != null && kotlin.math.abs(row.first().cy - l.cy) < maxOf(8, l.h / 2)) row += l else rows += mutableListOf(l)
        }
        return rows.joinToString("\n") { r -> r.sortedBy { it.cx }.joinToString("   ") { it.s } }
    }
}
