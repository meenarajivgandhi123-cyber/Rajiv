package com.example.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.ZoneDailyEntryRow
import com.example.data.model.ZoneDailyReportSummary
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ZoneDailyReportPdfGenerator {

  private const val PAGE_WIDTH = 842
  private const val PAGE_HEIGHT = 595
  private const val MARGIN = 18f
  private const val ROWS_PER_PAGE = 22

  fun generatePdfBytes(summary: ZoneDailyReportSummary): ByteArray {
    val pdf = PdfDocument()
    val chunks = if (summary.rows.isEmpty()) listOf(emptyList()) else summary.rows.chunked(ROWS_PER_PAGE)

    chunks.forEachIndexed { pageIndex, pageRows ->
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
      val page = pdf.startPage(pageInfo)
      drawPage(
        canvas = page.canvas,
        summary = summary,
        rows = pageRows,
        isLastPage = pageIndex == chunks.lastIndex,
      )
      pdf.finishPage(page)
    }

    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
  }

  private fun drawPage(
    canvas: Canvas,
    summary: ZoneDailyReportSummary,
    rows: List<ZoneDailyEntryRow>,
    isLastPage: Boolean,
  ) {
    canvas.drawColor(Color.WHITE)

    val startX = MARGIN
    val endX = PAGE_WIDTH - MARGIN
    val totalWidth = endX - startX

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 0.7f
    }
    val heavyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 1.2f
    }
    val fillPaint = Paint().apply { style = Paint.Style.FILL }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 18f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 6.4f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val whiteHeaderPaint = Paint(headerPaint).apply { color = Color.WHITE }
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 6.7f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val leftCellPaint = Paint(cellPaint).apply { textAlign = Paint.Align.LEFT }

    // Palette matching HR ZONE-DAILY REPORT image
    val colGrayHeader = Color.rgb(218, 224, 232)
    val colRegionPeach = Color.rgb(248, 224, 212)
    val colGreenHeader = Color.rgb(198, 224, 180)
    val colDarkGreen = Color.rgb(56, 118, 29)
    val colOliveMort = Color.rgb(106, 168, 79)
    val colPeachHeader = Color.rgb(248, 203, 173)
    val colBlueRow = Color.rgb(217, 225, 242)
    val colRedCell = Color.rgb(255, 0, 0)
    val colAmberCell = Color.rgb(255, 192, 0)
    val colGreenCell = Color.rgb(56, 168, 50)
    val colTotalOrange = Color.rgb(244, 132, 36)

    // 1. Top Banner Block
    var y = MARGIN
    val topH = 44f
    val leftBoxW = 195f

    fillPaint.color = colGrayHeader
    canvas.drawRect(startX, y, endX, y + topH, fillPaint)
    fillPaint.color = colRegionPeach
    canvas.drawRect(startX + 68f, y + 14f, startX + leftBoxW, y + 30f, fillPaint)

    canvas.drawRect(startX, y, startX + 68f, y + 14f, borderPaint)
    canvas.drawRect(startX + 68f, y, startX + leftBoxW, y + 14f, borderPaint)
    canvas.drawRect(startX, y + 14f, startX + 68f, y + 30f, borderPaint)
    canvas.drawRect(startX + 68f, y + 14f, startX + leftBoxW, y + 30f, borderPaint)
    canvas.drawRect(startX, y + 30f, endX, y + topH, borderPaint)
    canvas.drawRect(startX, y, endX, y + topH, heavyBorderPaint)

    val dateStr = try {
      LocalDate.ofEpochDay(summary.reportEpochDay).format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US))
    } catch (_: Exception) {
      summary.reportEpochDay.toString()
    }
    canvas.drawText(dateStr, startX + 68f + (leftBoxW - 68f) / 2f, y + 10f, cellPaint)
    canvas.drawText("REGION", startX + 34f, y + 25f, cellPaint)
    canvas.drawText(summary.region.uppercase(), startX + 68f + (leftBoxW - 68f) / 2f, y + 25f, cellPaint)

    val bannerTitle = "${summary.zoneName.uppercase()}-DAILY REPORT"
    canvas.drawText(bannerTitle, startX + leftBoxW + (totalWidth - leftBoxW) / 2f, y + 26f, titlePaint)
    y += topH

    // 21 Columns scaled cleanly to 806pt width
    // Weights sum = 806
    val colWidths = floatArrayOf(
      22f, // 0: SR
      34f, // 1: FARM CODE
      112f, // 2: FARM NAME
      38f, // 3: STRENGTH
      24f, // 4: AGE
      28f, // 5: MORT
      34f, // 6: MORT%
      34f, // 7: C.MORT
      28f, // 8: SALES
      42f, // 9: BALANCE BIRDS
      46f, // 10: RECEIVED FEED(BAG)
      34f, // 11: FEED INTAKE
      44f, // 12: T.CON (KG)
      36f, // 13: B.FEED(BAG)
      38f, // 14: FEED GRAM
      34f, // 15: DAY GAIN
      34f, // 16: B.WEIGHT
      28f, // 17: FCR
      34f, // 18: LOOSE BIRDS
      40f, // 19: FEEDER STOCK
    )
    // Scale proportionally to exact totalWidth
    val sumW = colWidths.sum()
    val widths = FloatArray(colWidths.size) { i -> colWidths[i] * (totalWidth / sumW) }
    val xPos = FloatArray(widths.size + 1)
    xPos[0] = startX
    for (i in widths.indices) {
      xPos[i + 1] = xPos[i] + widths[i]
    }

    // 2. Group Header + Column Header (2 tiers: 14pt + 24pt = 38pt)
    val gH = 14f
    val cH = 24f

    // Group row fills: MORTALITY DETAILS (cols 5..7) and FEED DETAILS (cols 10..13)
    fillPaint.color = colDarkGreen
    canvas.drawRect(xPos[5], y, xPos[8], y + gH, fillPaint)
    canvas.drawRect(xPos[5], y, xPos[8], y + gH, borderPaint)
    canvas.drawText("MORTALITY DETAILS", (xPos[5] + xPos[8]) / 2f, y + 10f, whiteHeaderPaint)

    fillPaint.color = colPeachHeader
    canvas.drawRect(xPos[10], y, xPos[14], y + gH, fillPaint)
    canvas.drawRect(xPos[10], y, xPos[14], y + gH, borderPaint)
    canvas.drawText("FEED DETAILS", (xPos[10] + xPos[14]) / 2f, y + 10f, headerPaint)

    val headers = listOf(
      "DATE" to "",
      "FARM" to "CODE",
      "FARM NAME" to "",
      "STRENGTH" to "",
      "AGE" to "",
      "MORT" to "",
      "MORT%" to "",
      "C.MORT" to "",
      "SALES" to "",
      "BALANCE" to "BIRDS",
      "RECEIVED" to "FEED(BAG)",
      "FEED" to "INTAKE",
      "T.CON" to "(KG)",
      "B.FEED" to "(BAG)",
      "FEED GRAM" to "",
      "DAY GAIN" to "",
      "B.WEIGHT" to "",
      "FCR" to "",
      "LOOSE" to "BIRDS",
      "FEEDER" to "STOCK",
    )

    headers.forEachIndexed { idx, (line1, line2) ->
      val bg = when (idx) {
        in 5..7 -> colDarkGreen
        in 10..13 -> colPeachHeader
        else -> colGreenHeader
      }
      val topY = if (idx in 5..7 || idx in 10..13) y + gH else y
      val h = if (idx in 5..7 || idx in 10..13) cH else gH + cH
      fillPaint.color = bg
      canvas.drawRect(xPos[idx], topY, xPos[idx + 1], topY + h, fillPaint)
      canvas.drawRect(xPos[idx], topY, xPos[idx + 1], topY + h, borderPaint)

      val tp = if (idx in 5..7) whiteHeaderPaint else headerPaint
      val cx = (xPos[idx] + xPos[idx + 1]) / 2f
      if (line2.isEmpty()) {
        canvas.drawText(line1, cx, topY + h / 2f + 2.5f, tp)
      } else {
        canvas.drawText(line1, cx, topY + h / 2f - 2f, tp)
        canvas.drawText(line2, cx, topY + h / 2f + 7.5f, tp)
      }
    }
    y += gH + cH

    // 3. Data Rows
    val rowH = 16f
    rows.forEach { r ->
      val rowValues = listOf(
        r.serialNo.toString(),
        r.farmCode,
        r.farmName,
        r.strength.toString(),
        r.ageDays.toString(),
        r.dailyMort.toString(),
        String.format(Locale.US, "%.2f%%", r.dailyMortPercent),
        r.cumulativeMort.toString(),
        r.salesBirds.toString(),
        r.balanceBirds.toString(),
        r.receivedFeedBags.toString(),
        String.format(Locale.US, "%.0f", r.feedIntakeBags),
        String.format(Locale.US, "%.0f", r.totalConsumedKg),
        String.format(Locale.US, "%.0f", r.balanceFeedBags),
        String.format(Locale.US, "%.3f", r.feedGramKgPerBird),
        String.format(Locale.US, "%.2f", r.dayGainKg),
        String.format(Locale.US, "%.2f", r.bodyWeightKg),
        if (r.fcr > 0.0) String.format(Locale.US, "%.2f", r.fcr) else "0",
        r.looseBirds.toString(),
        r.feederStock.toString(),
      )

      rowValues.forEachIndexed { idx, txt ->
        val cellBg: Int? = when (idx) {
          in 0..2 -> colBlueRow
          5 -> colOliveMort
          6 -> when {
            r.dailyMortPercent > 0.18 -> colRedCell
            r.dailyMortPercent >= 0.10 -> colAmberCell
            else -> colGreenCell
          }
          7 -> colOliveMort
          in 10..13 -> colPeachHeader.let { Color.rgb(252, 224, 204) }
          else -> null
        }
        if (cellBg != null) {
          fillPaint.color = cellBg
          canvas.drawRect(xPos[idx], y, xPos[idx + 1], y + rowH, fillPaint)
        }
        canvas.drawRect(xPos[idx], y, xPos[idx + 1], y + rowH, borderPaint)

        val tp = when {
          idx == 2 -> leftCellPaint
          idx == 6 && r.dailyMortPercent > 0.18 -> Paint(cellPaint).apply { color = Color.WHITE }
          else -> cellPaint
        }
        if (idx == 2) {
          val maxChars = 23
          val clipped = if (txt.length > maxChars) txt.take(maxChars - 1) + "…" else txt
          canvas.drawText(clipped, xPos[idx] + 4f, y + 10.5f, tp)
        } else {
          canvas.drawText(txt, (xPos[idx] + xPos[idx + 1]) / 2f, y + 10.5f, tp)
        }
      }
      y += rowH
    }

    // 4. Orange TOTAL Footer Row on last page
    if (isLastPage) {
      fillPaint.color = colTotalOrange
      canvas.drawRect(startX, y, endX, y + 18f, fillPaint)
      val totals = listOf(
        "",
        "",
        "TOTAL",
        summary.totalStrength.toString(),
        "",
        summary.totalDailyMort.toString(),
        String.format(Locale.US, "%.2f%%", summary.overallDailyMortPercent),
        summary.totalCumulativeMort.toString(),
        summary.totalSales.toString(),
        summary.totalBalanceBirds.toString(),
        summary.totalReceivedFeedBags.toString(),
        String.format(Locale.US, "%.0f", summary.totalFeedIntakeBags),
        String.format(Locale.US, "%.0f", summary.totalConsumedKg),
        String.format(Locale.US, "%.0f", summary.totalBalanceFeedBags),
        "",
        "",
        "",
        "",
        summary.totalLooseBirds.toString(),
        summary.totalFeederStock.toString(),
      )
      totals.forEachIndexed { idx, txt ->
        canvas.drawRect(xPos[idx], y, xPos[idx + 1], y + 18f, borderPaint)
        if (txt.isNotEmpty()) {
          canvas.drawText(txt, (xPos[idx] + xPos[idx + 1]) / 2f, y + 12f, cellPaint)
        }
      }
      y += 18f
    }

    canvas.drawRect(startX, MARGIN + topH, endX, y, heavyBorderPaint)
  }
}
