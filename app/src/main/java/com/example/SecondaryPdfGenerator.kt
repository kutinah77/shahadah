package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun generateSecondaryCertificatePdf(
    context: Context,
    name: String,
    seatNo: String,
    gov: String,
    subjectGrades: Map<String, Int>
): File? {
    val document = PdfDocument()

    // Page 1: A4 dimension (595 x 842)
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val pWidth = 595f
    val pHeight = 842f

    // 1. Draw classical borders & corners
    drawCertificateBorders(canvas, pWidth, pHeight)

    // 2. Draw official Republican emblem watermark seal (on top left)
    val cx = 75f
    val cy = 75f
    val radius = 25f
    val sealPaint = Paint().apply {
        color = 0xFFD97706.toInt() // Gold
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, radius, sealPaint)
    sealPaint.color = 0xFF1E3A8A.toInt() // Royal Blue concentric inner circle
    canvas.drawCircle(cx, cy, radius - 4f, sealPaint)

    // Gold Star Crest shape
    sealPaint.apply {
        color = 0xFFD97706.toInt()
        style = Paint.Style.FILL
    }
    val starPath = android.graphics.Path().apply {
        moveTo(cx, cy - 10f)
        lineTo(cx + 3f, cy - 3f)
        lineTo(cx + 10f, cy - 3f)
        lineTo(cx + 4f, cy + 2f)
        lineTo(cx + 7f, cy + 9f)
        lineTo(cx, cy + 5f)
        lineTo(cx - 7f, cy + 9f)
        lineTo(cx - 4f, cy + 2f)
        lineTo(cx - 10f, cy - 3f)
        lineTo(cx - 3f, cy - 3f)
        close()
    }
    canvas.drawPath(starPath, sealPaint)

    // 3. Draw Right Side Header labels
    val textR = 558f
    drawTextRight(canvas, context.getString(R.string.pdf_header_country), textR, 40f, 250, 11f, 0xFF0F172A.toInt(), isBold = true)
    drawTextRight(canvas, context.getString(R.string.pdf_header_ministry), textR, 56f, 250, 10f, 0xFF0F172A.toInt(), isBold = true)
    drawTextRight(canvas, context.getString(R.string.pdf_header_department), textR, 72f, 250, 9f, 0xFF64748B.toInt(), isBold = false)

    // 4. Document Heading
    val titleY = 115f
    val linePaint = Paint().apply {
        color = 0xFFD97706.toInt()
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
    }
    canvas.drawLine(140f, titleY + 14f, 455f, titleY + 14f, linePaint)
    drawTextCenter(canvas, context.getString(R.string.pdf_certificate_title_secondary), 297.5f, titleY - 5f, 13.5f, 0xFF1E3A8A.toInt(), isBold = true)

    // 5. Personal Details Card Box
    val infoY = 155f
    val infoHeight = 85f
    val infoWidth = 520f

    val infoBgPaint = Paint().apply {
        color = 0xFFF8FAFC.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(38f, infoY, 38f + infoWidth, infoY + infoHeight, 6f, 6f, infoBgPaint)

    val infoBorderPaint = Paint().apply {
        color = 0xFFE2E8F0.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    canvas.drawRoundRect(38f, infoY, 38f + infoWidth, infoY + infoHeight, 6f, 6f, infoBorderPaint)

    // Personal details layout (2-Column aligned)
    val colRightX = 38f + infoWidth - 15f
    val colLeftX = 38f + infoWidth / 2f - 15f

    drawTextRight(canvas, "اسم الطالب:   $name", colRightX, infoY + 12f, 240, 10.5f, 0xFF0F172A.toInt(), isBold = true)
    drawTextRight(canvas, "المحافظة:   $gov", colRightX, infoY + 36f, 240, 9.5f, 0xFF0F172A.toInt(), isBold = false)

    val arabicSeatNo = formatToArabicIndicDigits(seatNo)
    drawTextRight(canvas, "رقم الجلوس:   $arabicSeatNo", colLeftX, infoY + 12f, 240, 10.5f, 0xFF0F172A.toInt(), isBold = true)

    val arabicYear = formatToArabicIndicDigits("2025") + " / " + formatToArabicIndicDigits("2026") + " م"
    drawTextRight(canvas, "العام الدراسي:   $arabicYear", colLeftX, infoY + 36f, 240, 9.5f, 0xFF0F172A.toInt(), isBold = false)

    drawTextRight(canvas, "مصدر البيانات:   نظام كشوف الاستعلام لطلاب مرحلة التعليم الثانوي - العلمي المعتمد", colRightX, infoY + 60f, 490, 8.5f, 0xFF64748B.toInt(), isBold = false)

    // 6. Grades Table Grid (8 rows now!)
    val headers = listOf("م", "المادة الدراسية", "النهاية العظمى", "النهاية الصغرى", "الدرجة المحصلة", "الدرجة كتابةً", "النتيجة")
    val colWidths = listOf(30, 130, 60, 60, 70, 110, 60) // Sums to 520
    var tableX = 38f
    val tableY = 255f
    val headerHeight = 26f
    val rowHeight = 22f

    // Draw header row
    for (i in headers.indices) {
        drawTableCell(
            canvas = canvas,
            text = headers[i],
            x = tableX,
            y = tableY,
            width = colWidths[i],
            height = headerHeight,
            bgColor = 0xFF1E3A8A.toInt(), // Deep blue
            textColor = 0xFFFFFFFF.toInt(),
            isBold = true
        )
        tableX += colWidths[i]
    }

    // Subjects list for secondary education
    val subjects = listOf(
        Pair("quran", R.string.sub_quran),
        Pair("islamic", R.string.sub_islamic),
        Pair("arabic", R.string.sub_arabic),
        Pair("english", R.string.sub_english),
        Pair("math", R.string.sub_math),
        Pair("physics", R.string.sub_physics),
        Pair("chemistry", R.string.sub_chemistry),
        Pair("biology", R.string.sub_biology)
    )

    // Draw subjects rows
    var currentY = tableY + headerHeight
    val totalSum = subjectGrades.values.sum()
    val isPassedAll = subjectGrades.values.all { it >= 50 }

    subjects.forEachIndexed { index, pair ->
        val gradeId = pair.first
        val nameResId = pair.second
        val gradeValue = subjectGrades[gradeId] ?: 0
        val isFailing = gradeValue < 50

        // Alternating rows & failing indicator
        val rowBgColor = if (isFailing) 0xFFFEF2F2.toInt() else if (index % 2 == 0) 0xFFF8FAFC.toInt() else 0xFFFFFFFF.toInt()
        val cellTextColor = if (isFailing) 0xFFEF4444.toInt() else 0xFF0F172A.toInt()

        val arabicIndex = formatToArabicIndicDigits((index + 1).toString())
        val subjectName = context.getString(nameResId)
        val maxGr = formatToArabicIndicDigits("100")
        val minGr = formatToArabicIndicDigits("50")
        val gradeText = formatToArabicIndicDigits(gradeValue.toString())
        val gradeInWordsText = gradeToArabicWords(gradeValue)
        val resultText = if (isFailing) "راسب" else "ناجح"

        tableX = 38f

        // Cell 1: Index
        drawTableCell(canvas, arabicIndex, tableX, currentY, colWidths[0], rowHeight, rowBgColor, cellTextColor, align = Layout.Alignment.ALIGN_CENTER)
        tableX += colWidths[0]

        // Cell 2: Name
        drawTableCell(canvas, subjectName, tableX, currentY, colWidths[1], rowHeight, rowBgColor, cellTextColor, align = Layout.Alignment.ALIGN_OPPOSITE)
        tableX += colWidths[1]

        // Cell 3: Max
        drawTableCell(canvas, maxGr, tableX, currentY, colWidths[2], rowHeight, rowBgColor, cellTextColor, align = Layout.Alignment.ALIGN_CENTER)
        tableX += colWidths[2]

        // Cell 4: Min
        drawTableCell(canvas, minGr, tableX, currentY, colWidths[3], rowHeight, rowBgColor, cellTextColor, align = Layout.Alignment.ALIGN_CENTER)
        tableX += colWidths[3]

        // Cell 5: Grade
        drawTableCell(canvas, gradeText, tableX, currentY, colWidths[4], rowHeight, rowBgColor, cellTextColor, isBold = true, align = Layout.Alignment.ALIGN_CENTER)
        tableX += colWidths[4]

        // Cell 6: Grade words
        drawTableCell(canvas, gradeInWordsText, tableX, currentY, colWidths[5], rowHeight, rowBgColor, cellTextColor, align = Layout.Alignment.ALIGN_OPPOSITE)
        tableX += colWidths[5]

        // Cell 7: Result
        drawTableCell(canvas, resultText, tableX, currentY, colWidths[6], rowHeight, rowBgColor, cellTextColor, isBold = true, align = Layout.Alignment.ALIGN_CENTER)

        currentY += rowHeight
    }

    // 7. Results Dashboard Summary Box (Pale yellow background: "خلفية صفراء باهتة")
    val summaryY = currentY + 15f
    val summaryHeight = 65f
    val summaryWidth = 520f

    val summaryBgPaint = Paint().apply {
        color = 0xFFFFFBEB.toInt() // Pale warm amber/yellow
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(38f, summaryY, 38f + summaryWidth, summaryY + summaryHeight, 6f, 6f, summaryBgPaint)

    val summaryBorderPaint = Paint().apply {
        color = 0xFFFCD34D.toInt() // Mild gold border for the yellow box
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    canvas.drawRoundRect(38f, summaryY, 38f + summaryWidth, summaryY + summaryHeight, 6f, 6f, summaryBorderPaint)

    // Calculate averages & overall grade values
    val avgValDouble = totalSum / 8.0
    val formattedAvgStr = String.format("%.2f", avgValDouble)
    val arabicAvg = formatToArabicIndicDigits(formattedAvgStr) + "%"

    val overallGradeText = if (!isPassedAll) "راسب" else when {
        avgValDouble >= 90 -> "ممتاز"
        avgValDouble >= 80 -> "جيد جداً"
        avgValDouble >= 70 -> "جيد"
        else -> "مقبول"
    }
    val overallResultLabel = if (isPassedAll) "ناجح" else "راسب"
    val resultBadgeColor = if (isPassedAll) 0xFF047857.toInt() else 0xFFB91C1C.toInt()

    val secWidth = summaryWidth / 4f

    // Sec 1: Total
    drawTextCenter(canvas, "المجموع الكلي", 38f + secWidth * 3.5f, summaryY + 12f, 9f, 0xFF78350F.toInt())
    val arabicTotalValue = formatToArabicIndicDigits(totalSum.toString()) + " / " + formatToArabicIndicDigits("800")
    drawTextCenter(canvas, arabicTotalValue, 38f + secWidth * 3.5f, summaryY + 28f, 11.5f, 0xFF1E1B4B.toInt(), isBold = true)

    canvas.drawLine(38f + secWidth * 3f, summaryY + 8f, 38f + secWidth * 3f, summaryY + summaryHeight - 8f, summaryBorderPaint)

    // Sec 2: Average
    drawTextCenter(canvas, "المعدل العام", 38f + secWidth * 2.5f, summaryY + 12f, 9f, 0xFF78350F.toInt())
    drawTextCenter(canvas, arabicAvg, 38f + secWidth * 2.5f, summaryY + 28f, 11.5f, 0xFF1E1B4B.toInt(), isBold = true)

    canvas.drawLine(38f + secWidth * 2f, summaryY + 8f, 38f + secWidth * 2f, summaryY + summaryHeight - 8f, summaryBorderPaint)

    // Sec 3: Overall Grade
    drawTextCenter(canvas, "التقدير العام", 38f + secWidth * 1.5f, summaryY + 12f, 9f, 0xFF78350F.toInt())
    drawTextCenter(canvas, overallGradeText, 38f + secWidth * 1.5f, summaryY + 28f, 11.5f, 0xFF1E1B4B.toInt(), isBold = true)

    canvas.drawLine(38f + secWidth * 1f, summaryY + 8f, 38f + secWidth * 1f, summaryY + summaryHeight - 8f, summaryBorderPaint)

    // Sec 4: General Status Badge
    drawTextCenter(canvas, "النتيجة النهائية", 38f + secWidth * 0.5f, summaryY + 12f, 9f, 0xFF78350F.toInt())

    val badgePaint = Paint().apply {
        color = if (isPassedAll) 0xFFD1FAE5.toInt() else 0xFFFEE2E2.toInt()
        style = Paint.Style.FILL
    }
    val badgeX = 38f + secWidth * 0.5f
    canvas.drawRoundRect(badgeX - 32f, summaryY + 26f, badgeX + 32f, summaryY + 44f, 9f, 9f, badgePaint)
    drawTextCenter(canvas, overallResultLabel, badgeX, summaryY + 28f, 9.5f, resultBadgeColor, isBold = true)

    // 8. Signatures & Official dashed ink seal
    val sigY = summaryY + summaryHeight + 25f

    // Committee chairman
    drawTextCenter(canvas, "رئيس لجنة النظام والمراقبة", 130f, sigY, 10f, 0xFF0F172A.toInt(), isBold = true)
    drawTextCenter(canvas, "..........................................", 130f, sigY + 28f, 10f, 0xFF94A3B8.toInt())

    // Director of exams
    drawTextCenter(canvas, "مدير إدارة الامتحانات", 465f, sigY, 10f, 0xFF0F172A.toInt(), isBold = true)
    drawTextCenter(canvas, "..........................................", 465f, sigY + 28f, 10f, 0xFF94A3B8.toInt())

    // Center stamp concentric circles (dashed)
    val stampX = 297.5f
    val stampY = sigY + 30f
    val stampRadius = 28f

    val stampPaint = Paint().apply {
        color = 0xFF3B82F6.toInt() // Blue stamp
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(4f, 2f), 0f)
    }
    canvas.drawCircle(stampX, stampY, stampRadius, stampPaint)
    canvas.drawCircle(stampX, stampY, stampRadius - 4f, stampPaint)

    drawTextCenter(canvas, "وزارة التربية والتعليم", stampX, stampY - 10f, 5.5f, 0xFF3B82F6.toInt(), isBold = true)
    drawTextCenter(canvas, "الختم الرسمي", stampX, stampY, 5f, 0xFF3B82F6.toInt())
    drawTextCenter(canvas, "قطاع التوجيه", stampX, stampY + 9f, 5f, 0xFF3B82F6.toInt())

    // 9. Document Footer Informative Note
    val footerY = 785f
    val noteText = context.getString(R.string.pdf_footer_note)
    drawTextCenter(canvas, noteText, 297.5f, footerY, 8f, 0xFF94A3B8.toInt())

    document.finishPage(page)

    // 10. Write output file to Downloads or Cache storage securely
    val filename = "Shahadati_Secondary_${seatNo}.pdf"
    var pdfFile: File? = null
    var outStream: OutputStream? = null

    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outStream = resolver.openOutputStream(uri)
                pdfFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
            }
        }

        if (outStream == null) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            pdfFile = File(downloadsDir, filename)
            outStream = FileOutputStream(pdfFile)
        }

        document.writeTo(outStream)
        outStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val fallbackFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)
            val fallbackOut = FileOutputStream(fallbackFile)
            document.writeTo(fallbackOut)
            fallbackOut.flush()
            fallbackOut.close()
            pdfFile = fallbackFile
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    } finally {
        outStream?.close()
        document.close()
    }

    return pdfFile
}
