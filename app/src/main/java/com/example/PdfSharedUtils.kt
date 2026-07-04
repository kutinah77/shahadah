package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Converts English digits inside a string to formal Arabic-Indic digits (e.g. 1 -> ١, 2 -> ٢)
 */
fun formatToArabicIndicDigits(input: String): String {
    val arabicIndicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return input.map { char ->
        if (char.isDigit()) {
            arabicIndicDigits[char - '0']
        } else {
            char
        }
    }.joinToString("")
}

/**
 * Formats a grade number (0-100) into literary Arabic words.
 */
fun gradeToArabicWords(grade: Int): String {
    if (grade == 0) return "صفر"
    if (grade == 100) return "مائة"

    val units = listOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val tens = listOf("", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")

    val u = grade % 10
    val t = grade / 10

    return when {
        t == 1 && u == 0 -> "عشرة"
        t == 1 && u == 1 -> "أحد عشر"
        t == 1 && u == 2 -> "إثنا عشر"
        t == 1 && u > 2 -> "${units[u]} عشر"
        u == 0 -> tens[t]
        t == 0 -> units[u]
        else -> "${units[u]} و${tens[t]}"
    }
}

/**
 * Text drawing helpers for PDF Canvas.
 * Solves Arabic bidirectional text and character shaping by using Android's StaticLayout.
 */
fun drawTextCenter(canvas: Canvas, text: String, cx: Float, y: Float, textSize: Float, color: Int, isBold: Boolean = false) {
    val paint = TextPaint().apply {
        this.color = color
        this.textSize = textSize
        this.isAntiAlias = true
        if (isBold) {
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 500)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .build()

    canvas.save()
    canvas.translate(cx - 250f, y)
    layout.draw(canvas)
    canvas.restore()
}

fun drawTextRight(canvas: Canvas, text: String, rx: Float, y: Float, width: Int, textSize: Float, color: Int, isBold: Boolean = false) {
    val paint = TextPaint().apply {
        this.color = color
        this.textSize = textSize
        this.isAntiAlias = true
        if (isBold) {
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
        .build()

    canvas.save()
    canvas.translate(rx - width, y)
    layout.draw(canvas)
    canvas.restore()
}

/**
 * Individual cell drawing helper for the PDF Transcript table.
 */
fun drawTableCell(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Int,
    height: Float,
    bgColor: Int?,
    textColor: Int,
    isBold: Boolean = false,
    align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
) {
    val paint = Paint().apply {
        style = Paint.Style.FILL
    }
    if (bgColor != null) {
        paint.color = bgColor
        canvas.drawRect(x, y, x + width, y + height, paint)
    }

    // Border line
    paint.color = 0xFFCBD5E1.toInt()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.75f
    canvas.drawRect(x, y, x + width, y + height, paint)

    // Text rendering inside cell
    val textPaint = TextPaint().apply {
        this.color = textColor
        this.textSize = 9.5f
        this.isAntiAlias = true
        if (isBold) {
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width - 6)
        .setAlignment(align)
        .build()

    val textHeight = layout.height
    val textY = y + (height - textHeight) / 2f

    canvas.save()
    canvas.translate(x + 3f, textY)
    layout.draw(canvas)
    canvas.restore()
}

/**
 * Draws the background board and borders of an official certificate on the canvas
 */
fun drawCertificateBorders(canvas: Canvas, width: Float, height: Float) {
    // No frames or borders drawn as requested
}

/**
 * Standard utility to safely print a PDF document on Android.
 */
fun printPDF(context: Context, file: File) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "${context.getString(R.string.app_name)} Document"
    try {
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder("shahadati.pdf")
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var input: java.io.FileInputStream? = null
                var output: java.io.FileOutputStream? = null
                try {
                    input = java.io.FileInputStream(file)
                    output = java.io.FileOutputStream(destination?.fileDescriptor)
                    val buf = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } > 0) {
                        output.write(buf, 0, bytesRead)
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    input?.close()
                    output?.close()
                }
            }
        }
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    } catch (e: Exception) {
        Toast.makeText(context, "فشلت عملية الطباعة: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Shares the generated PDF document via WhatsApp or standard system chooser.
 */
fun sharePdfViaWhatsApp(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.shahadati.qpxlmv.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // WhatsApp not installed, open generic system chooser
            val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "مشاركة الشهادة الكترونياً")
            context.startActivity(chooserIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "فشلت مشاركة الملف: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
