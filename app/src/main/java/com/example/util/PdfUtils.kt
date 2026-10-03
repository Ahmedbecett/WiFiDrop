package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object PdfUtils {
    private const val TAG = "WiFiDrop_PdfUtils"

    /**
     * Copies a content URI to a temporary cache file so PdfRenderer can open it.
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val tempFile = File(context.cacheResolverDir(), "temp_view_${System.currentTimeMillis()}.pdf")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving PDF from URI", e)
            null
        }
    }

    private fun Context.cacheResolverDir(): File {
        val dir = File(cacheDir, "pdf_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Renders a specific page of a PDF file to a Bitmap.
     */
    fun renderPageToBitmap(
        file: File,
        pageIndex: Int,
        densityDpi: Int = 300
    ): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex in 0 until renderer.pageCount) {
                page = renderer.openPage(pageIndex)
                // Calculate scale for sharp rendering (approx 2x standard points)
                val scale = 2.0f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF page $pageIndex", e)
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Gets the total number of pages in a PDF file.
     */
    fun getPageCount(file: File): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            Log.e(TAG, "Error reading page count", e)
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Creates a sample Welcome & Quickstart PDF guide inside cache/sample directory.
     */
    fun createSamplePdf(context: Context): File {
        val sampleFile = File(context.cacheResolverDir(), "WiFiDrop_User_Guide.pdf")
        if (sampleFile.exists() && sampleFile.length() > 0) {
            return sampleFile
        }

        val pdfDocument = PdfDocument()
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#0B0F19") }
        canvas1.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Accent Header Card
        val cardPaint = Paint().apply { color = Color.parseColor("#111827") }
        canvas1.drawRoundRect(30f, 30f, 565f, 210f, 16f, 16f, cardPaint)

        val cyanPaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText("WiFiDrop — Ultimate User Guide", 50f, 80f, cyanPaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 14f
            isAntiAlias = true
        }
        canvas1.drawText("Ultra-fast Local Wi-Fi File Sharing & Built-in PDF Suite", 50f, 115f, subPaint)
        canvas1.drawText("Version 1.1 • No Internet Required • 100% Private", 50f, 140f, subPaint)

        // Content
        val sectionTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            textSize = 13f
            isAntiAlias = true
        }

        var y = 250f
        canvas1.drawText("1. Sending Files to PC or Phone", 50f, y, sectionTitlePaint)
        y += 24f
        canvas1.drawText("• Tap 'Send Files' and choose photos, 4K videos, documents, or apps.", 50f, y, bodyPaint)
        y += 20f
        canvas1.drawText("• Scan the QR code or enter the receiver's IP address.", 50f, y, bodyPaint)
        y += 20f
        canvas1.drawText("• Watch live transfer speeds reaching up to 40 MB/s!", 50f, y, bodyPaint)

        y += 40f
        canvas1.drawText("2. Receiving Files & PC Web Drop", 50f, y, sectionTitlePaint)
        y += 24f
        canvas1.drawText("• Tap 'Receive Files' to start the local embedded HTTP server.", 50f, y, bodyPaint)
        y += 20f
        canvas1.drawText("• Open the displayed IP on any PC/Mac browser (e.g. 192.168.1.5:8080).", 50f, y, bodyPaint)
        y += 20f
        canvas1.drawText("• Drag and drop files from your computer directly into your phone.", 50f, y, bodyPaint)

        y += 40f
        canvas1.drawText("3. Built-in PDF Reader & Manager", 50f, y, sectionTitlePaint)
        y += 24f
        canvas1.drawText("• Open any PDF document instantly without external viewer apps.", 50f, y, bodyPaint)
        y += 20f
        canvas1.drawText("• Switch between Light, Dark (Night Mode), and Sepia reading themes.", 50f, y, bodyPaint)
        y += 20f
        canvas1.drawText("• Direct print, share, and jump to any page with smooth navigation.", 50f, y, bodyPaint)

        y += 50f
        val footerPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 11f
            isAntiAlias = true
        }
        canvas1.drawText("Generated by WiFiDrop Android Application • Developed by Ahmed Becetti", 50f, 800f, footerPaint)

        pdfDocument.finishPage(page1)

        // Page 2
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas
        canvas2.drawRect(0f, 0f, 595f, 842f, bgPaint)

        canvas2.drawRoundRect(30f, 30f, 565f, 130f, 16f, 16f, cardPaint)
        canvas2.drawText("Security, Privacy & Best Practices", 50f, 80f, cyanPaint)
        canvas2.drawText("Everything stays on your local Wi-Fi. Zero data uploaded to cloud.", 50f, 110f, subPaint)

        y = 180f
        canvas2.drawText("PIN Code Protection", 50f, y, sectionTitlePaint)
        y += 24f
        canvas2.drawText("When enabled in Settings, the web interface prompts the user for a 6-digit", 50f, y, bodyPaint)
        y += 20f
        canvas2.drawText("PIN before allowing any file upload or download.", 50f, y, bodyPaint)

        y += 40f
        canvas2.drawText("Hotspot Mode for Offline Sharing", 50f, y, sectionTitlePaint)
        y += 24f
        canvas2.drawText("No Wi-Fi router nearby? Simply enable your Portable Hotspot and connect", 50f, y, bodyPaint)
        y += 20f
        canvas2.drawText("the second device to transfer files at maximum Wi-Fi Direct speed.", 50f, y, bodyPaint)

        canvas2.drawText("WiFiDrop Document Suite — Page 2 of 2", 50f, 800f, footerPaint)

        pdfDocument.finishPage(page2)

        try {
            FileOutputStream(sampleFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sample PDF", e)
        } finally {
            pdfDocument.close()
        }

        return sampleFile
    }

    /**
     * Converts a list of Images (Bitmaps) to a single PDF Document.
     */
    fun convertImagesToPdf(bitmaps: List<Bitmap>, outputFile: File): Boolean {
        if (bitmaps.isEmpty()) return false
        val document = PdfDocument()
        return try {
            for ((index, bitmap) in bitmaps.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
            }
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error converting images to PDF", e)
            false
        } finally {
            document.close()
        }
    }
}
