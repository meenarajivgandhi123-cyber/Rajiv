package com.example.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.model.ClaimStatus
import com.example.data.model.KmClaimReportData
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object KmClaimPdfGenerator {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 24f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)
  private const val FOOTER_Y = PAGE_HEIGHT - 22f
  private const val MAX_BODY_Y = PAGE_HEIGHT - 38f
  private const val MILLIS_PER_DAY = 86_400_000L

  fun generateClaimDetailPdfBytes(
    context: Context,
    report: KmClaimReportData,
    todayEpochDay: Long,
  ): ByteArray {
    val claim = report.claim
    val pdf = PdfDocument()
    val cursor = PdfCursor(
      context = context,
      pdf = pdf,
      headerTitle = "SPF POULTRY • MONTHLY KM CLAIM DETAIL",
      headerSubtitle = "${claim.empCode} (${claim.operatorName}) • ${claim.resolvedMonthLabel} • Ref ${claim.id.ifBlank { "MONTHLY" }}",
      contdHeader = "SPF KM Claim Detail • ${claim.empCode} (${claim.resolvedMonthLabel}) — Contd.",
      footerPrefix = "SPF Poultry Integration • KM Claim Statement (${claim.empCode} • ${claim.resolvedMonthLabel})",
    )
    cursor.startFirstPage()

    drawSingleClaimProfile(cursor, report, todayEpochDay)
    drawDailyOdometerTable(cursor, report)
    drawSignOffBox(cursor, claim.operatorName, claim.empCode, claim.reviewedByName, claim.reviewedByEmpCode)

    cursor.finish()
    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
  }

  fun generateAllClaimsPdfBytes(
    context: Context,
    reports: List<KmClaimReportData>,
    filterLabel: String,
    todayEpochDay: Long,
  ): ByteArray {
    val pdf = PdfDocument()
    val cursor = PdfCursor(
      context = context,
      pdf = pdf,
      headerTitle = "SPF POULTRY • KM CLAIMS REGISTER & DETAIL",
      headerSubtitle = "Filter: $filterLabel • ${reports.size} Monthly Claim(s) • Generated ${formatDate(todayEpochDay)}",
      contdHeader = "SPF KM Claims Register ($filterLabel) — Contd.",
      footerPrefix = "SPF Poultry Integration • KM Claims Register ($filterLabel)",
    )
    cursor.startFirstPage()

    drawRegisterSummaryStrip(cursor, reports)
    drawClaimsSummaryTable(cursor, reports)

    reports.forEachIndexed { idx, rep ->
      cursor.ensureSpace(78f)
      val claim = rep.claim
      cursor.canvas.drawText(
        "${idx + 1}. ${claim.empCode} • ${claim.operatorName} (${claim.resolvedMonthLabel}) — ${claim.statusEnum.label.uppercase()}",
        MARGIN,
        cursor.y + 11f,
        cursor.sectionTitlePaint,
      )
      cursor.y += 15f
      drawDailyOdometerTable(cursor, rep)
    }

    cursor.finish()
    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
  }

  private fun drawSingleClaimProfile(
    cursor: PdfCursor,
    report: KmClaimReportData,
    todayEpochDay: Long,
  ) {
    val claim = report.claim
    val c = cursor.canvas
    val startY = cursor.y

    // Employee & Claim Metadata Box
    val boxH = 54f
    c.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + boxH, cursor.borderPaint)
    val midX = MARGIN + CONTENT_WIDTH * 0.52f
    c.drawLine(midX, startY, midX, startY + boxH, cursor.thinBorderPaint)

    c.drawText("EMPLOYEE : ${claim.empCode} — ${claim.operatorName}", MARGIN + 8f, startY + 14f, cursor.boldPaint)
    c.drawText("CLAIM MONTH : ${claim.resolvedMonthLabel} (${claim.resolvedMonthKey})", MARGIN + 8f, startY + 28f, cursor.valuePaint)
    c.drawText(
      clipText("ROUTE / NOTE : ${claim.visitedFarms.ifBlank { "Monthly Odometer Travel Claim" }}", cursor.valuePaint, (midX - MARGIN) - 16f),
      MARGIN + 8f,
      startY + 42f,
      cursor.valuePaint,
    )

    val statusText = claim.statusEnum.label.uppercase()
    val (pillBg, pillFg) = when (claim.statusEnum) {
      ClaimStatus.APPROVED -> Color.rgb(220, 245, 228) to Color.rgb(18, 108, 48)
      ClaimStatus.REJECTED -> Color.rgb(252, 228, 228) to Color.rgb(168, 28, 28)
      ClaimStatus.PENDING -> Color.rgb(255, 243, 214) to Color.rgb(156, 98, 0)
    }
    val pillPaint = Paint().apply { color = pillBg; style = Paint.Style.FILL }
    val pillW = 78f
    val pillRect = RectF(PAGE_WIDTH - MARGIN - pillW - 8f, startY + 6f, PAGE_WIDTH - MARGIN - 8f, startY + 21f)
    c.drawRoundRect(pillRect, 4f, 4f, pillPaint)
    val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = pillFg
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    c.drawText(statusText, pillRect.centerX(), pillRect.top + 11f, pillTextPaint)

    c.drawText("SUBMITTED : ${formatDate(claim.claimEpochDay)}", midX + 8f, startY + 15f, cursor.valuePaint)
    val reviewText = if (claim.statusEnum != ClaimStatus.PENDING && claim.reviewedByEmpCode.isNotBlank()) {
      "REVIEWED : ${claim.reviewedByName} (${claim.reviewedByEmpCode})"
    } else {
      "STATEMENT DATE : ${formatDate(todayEpochDay)}"
    }
    c.drawText(clipText(reviewText, cursor.valuePaint, (PAGE_WIDTH - MARGIN - midX) - 16f), midX + 8f, startY + 29f, cursor.valuePaint)
    if (claim.reviewRemarks.isNotBlank()) {
      c.drawText(
        clipText("REMARKS : ${claim.reviewRemarks}", cursor.valuePaint, (PAGE_WIDTH - MARGIN - midX) - 16f),
        midX + 8f,
        startY + 43f,
        cursor.valuePaint,
      )
    }
    cursor.y = startY + boxH + 10f

    // 5-cell KPI Strip
    val kpis = listOf(
      "DAILY LOGS" to "${claim.dailyEntriesCount} (${report.totalDaysLogged} matched)",
      "ODOMETER SPAN" to if (claim.endKm > claim.startKm) "${fmt0(claim.startKm)} → ${fmt0(claim.endKm)}" else "Monthly Sum",
      "CLAIMED KM" to "${fmt1(claim.totalKm)} km",
      "RATE / KM" to "₹${fmt2(claim.ratePerKm)}",
      "CLAIM AMOUNT" to fmtRupee(claim.claimAmount),
    )
    val cellW = CONTENT_WIDTH / kpis.size
    val cardH = 34f
    kpis.forEachIndexed { idx, (label, value) ->
      val x0 = MARGIN + idx * cellW
      val x1 = x0 + cellW
      cursor.fillPaint.color = if (idx == kpis.lastIndex) Color.rgb(232, 240, 254) else Color.rgb(245, 247, 250)
      c.drawRect(x0, cursor.y, x1, cursor.y + cardH, cursor.fillPaint)
      c.drawRect(x0, cursor.y, x1, cursor.y + cardH, cursor.thinBorderPaint)
      c.drawText(label, x0 + 6f, cursor.y + 11f, cursor.kpiLabelPaint)
      c.drawText(
        clipText(value, cursor.kpiValPaint, cellW - 10f),
        x0 + 6f,
        cursor.y + 26f,
        cursor.kpiValPaint,
      )
    }
    cursor.y += cardH + 12f
  }

  private fun drawRegisterSummaryStrip(cursor: PdfCursor, reports: List<KmClaimReportData>) {
    val c = cursor.canvas
    val totalKm = reports.sumOf { it.claim.totalKm }
    val totalAmt = reports.sumOf { it.claim.claimAmount }
    val approvedCount = reports.count { it.claim.statusEnum == ClaimStatus.APPROVED }
    val pendingCount = reports.count { it.claim.statusEnum == ClaimStatus.PENDING }
    val kpis = listOf(
      "TOTAL CLAIMS" to "${reports.size}",
      "APPROVED" to "$approvedCount",
      "PENDING" to "$pendingCount",
      "TOTAL KM" to "${fmt1(totalKm)} km",
      "TOTAL AMOUNT" to fmtRupee(totalAmt),
    )
    val cellW = CONTENT_WIDTH / kpis.size
    val cardH = 34f
    kpis.forEachIndexed { idx, (label, value) ->
      val x0 = MARGIN + idx * cellW
      val x1 = x0 + cellW
      cursor.fillPaint.color = if (idx == kpis.lastIndex) Color.rgb(232, 240, 254) else Color.rgb(245, 247, 250)
      c.drawRect(x0, cursor.y, x1, cursor.y + cardH, cursor.fillPaint)
      c.drawRect(x0, cursor.y, x1, cursor.y + cardH, cursor.thinBorderPaint)
      c.drawText(label, x0 + 6f, cursor.y + 11f, cursor.kpiLabelPaint)
      c.drawText(clipText(value, cursor.kpiValPaint, cellW - 10f), x0 + 6f, cursor.y + 26f, cursor.kpiValPaint)
    }
    cursor.y += cardH + 12f
  }

  private fun drawClaimsSummaryTable(cursor: PdfCursor, reports: List<KmClaimReportData>) {
    val columns = listOf(
      ColSpec("EMP CODE", 0.12f, Paint.Align.LEFT),
      ColSpec("STAFF NAME", 0.19f, Paint.Align.LEFT),
      ColSpec("MONTH", 0.12f, Paint.Align.CENTER),
      ColSpec("LOGS", 0.08f, Paint.Align.RIGHT),
      ColSpec("KM", 0.12f, Paint.Align.RIGHT),
      ColSpec("RATE", 0.09f, Paint.Align.RIGHT),
      ColSpec("AMOUNT", 0.16f, Paint.Align.RIGHT),
      ColSpec("STATUS", 0.12f, Paint.Align.CENTER),
    )
    val rows = reports.map { rep ->
      val c = rep.claim
      listOf(
        c.empCode,
        c.operatorName,
        c.resolvedMonthLabel,
        c.dailyEntriesCount.toString(),
        fmt1(c.totalKm),
        "₹${fmt1(c.ratePerKm)}",
        fmtRupee(c.claimAmount),
        c.statusEnum.label,
      )
    }
    drawTableSection(
      cursor = cursor,
      title = "MONTHLY KM CLAIMS REGISTER (${reports.size})",
      columns = columns,
      rows = rows,
      emptyMessage = "No monthly KM claims match the active filter.",
    )
  }

  private fun drawDailyOdometerTable(cursor: PdfCursor, report: KmClaimReportData) {
    val claim = report.claim
    val columns = listOf(
      ColSpec("#", 0.06f, Paint.Align.CENTER),
      ColSpec("DATE", 0.14f, Paint.Align.CENTER),
      ColSpec("START KM", 0.14f, Paint.Align.RIGHT),
      ColSpec("END KM", 0.14f, Paint.Align.RIGHT),
      ColSpec("DISTANCE (KM)", 0.15f, Paint.Align.RIGHT),
      ColSpec("VISITED FARMS / ROUTE NOTES", 0.37f, Paint.Align.LEFT),
    )
    val rows = report.dailyEntries.mapIndexed { idx, entry ->
      listOf(
        (idx + 1).toString(),
        formatDate(entry.entryEpochDay),
        fmt1(entry.startKm),
        fmt1(entry.endKm),
        fmt1(entry.totalKm),
        entry.visitedFarms.ifBlank { "—" },
      )
    }
    val footerRow = listOf(
      "—",
      "TOTAL (${report.totalDaysLogged} days)",
      "—",
      "—",
      "${fmt1(report.entriesTotalKm)} km",
      "Claimed: ${fmt1(claim.totalKm)} km = ${fmtRupee(claim.claimAmount)}",
    )
    drawTableSection(
      cursor = cursor,
      title = "DAILY ODOMETER LOG BREAKDOWN (${report.dailyEntries.size} entries)",
      columns = columns,
      rows = rows,
      footerRow = footerRow,
      emptyMessage = "No daily odometer entries linked to this employee & month.",
    )
  }

  private fun drawSignOffBox(
    cursor: PdfCursor,
    operatorName: String,
    empCode: String,
    reviewedByName: String,
    reviewedByEmpCode: String,
  ) {
    cursor.ensureSpace(46f)
    val c = cursor.canvas
    val top = cursor.y + 4f
    val boxH = 38f
    c.drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + boxH, cursor.thinBorderPaint)
    val colW = CONTENT_WIDTH / 3f
    c.drawLine(MARGIN + colW, top, MARGIN + colW, top + boxH, cursor.thinBorderPaint)
    c.drawLine(MARGIN + colW * 2f, top, MARGIN + colW * 2f, top + boxH, cursor.thinBorderPaint)

    c.drawText("SUBMITTED BY", MARGIN + 8f, top + 12f, cursor.kpiLabelPaint)
    c.drawText(clipText("$empCode • $operatorName", cursor.valuePaint, colW - 14f), MARGIN + 8f, top + 28f, cursor.valuePaint)

    c.drawText("VERIFIED BY (ADMIN)", MARGIN + colW + 8f, top + 12f, cursor.kpiLabelPaint)
    val reviewer = if (reviewedByEmpCode.isNotBlank()) "$reviewedByEmpCode • $reviewedByName" else "Pending Review"
    c.drawText(clipText(reviewer, cursor.valuePaint, colW - 14f), MARGIN + colW + 8f, top + 28f, cursor.valuePaint)

    c.drawText("ACCOUNTS / SUPER ADMIN SIGN", MARGIN + colW * 2f + 8f, top + 12f, cursor.kpiLabelPaint)
    c.drawLine(MARGIN + colW * 2f + 10f, top + 30f, PAGE_WIDTH - MARGIN - 10f, top + 30f, cursor.thinBorderPaint)
    cursor.y = top + boxH + 8f
  }

  private data class ColSpec(
    val header: String,
    val weight: Float,
    val align: Paint.Align,
  )

  private fun drawTableSection(
    cursor: PdfCursor,
    title: String,
    columns: List<ColSpec>,
    rows: List<List<String>>,
    footerRow: List<String>? = null,
    emptyMessage: String,
  ) {
    val headerH = 16f
    val rowH = 14.5f
    cursor.ensureSpace(24f + headerH + rowH)

    cursor.canvas.drawText(title, MARGIN, cursor.y + 10f, cursor.sectionTitlePaint)
    cursor.y += 14f

    if (rows.isEmpty()) {
      cursor.canvas.drawRect(MARGIN, cursor.y, PAGE_WIDTH - MARGIN, cursor.y + 18f, cursor.thinBorderPaint)
      cursor.canvas.drawText(emptyMessage, MARGIN + 8f, cursor.y + 12f, cursor.mutedPaint)
      cursor.y += 26f
      return
    }

    drawTableHeader(cursor, columns, headerH)
    rows.forEachIndexed { idx, row ->
      if (cursor.needsPageBreak(rowH + 4f)) {
        cursor.newPage()
        cursor.canvas.drawText("$title (Contd.)", MARGIN, cursor.y + 10f, cursor.sectionTitlePaint)
        cursor.y += 14f
        drawTableHeader(cursor, columns, headerH)
      }
      drawTableRow(cursor, columns, row, rowH, shaded = idx % 2 == 1, bold = false)
    }

    if (footerRow != null) {
      if (cursor.needsPageBreak(rowH + 4f)) {
        cursor.newPage()
        drawTableHeader(cursor, columns, headerH)
      }
      drawTableRow(cursor, columns, footerRow, rowH, shaded = true, bold = true)
    }
    cursor.y += 10f
  }

  private fun drawTableHeader(
    cursor: PdfCursor,
    columns: List<ColSpec>,
    headerH: Float,
  ) {
    val c = cursor.canvas
    cursor.fillPaint.color = Color.rgb(22, 54, 108)
    c.drawRect(MARGIN, cursor.y, PAGE_WIDTH - MARGIN, cursor.y + headerH, cursor.fillPaint)
    c.drawRect(MARGIN, cursor.y, PAGE_WIDTH - MARGIN, cursor.y + headerH, cursor.borderPaint)

    var x = MARGIN
    columns.forEach { col ->
      val colW = CONTENT_WIDTH * col.weight
      cursor.thPaint.textAlign = col.align
      val tx = when (col.align) {
        Paint.Align.LEFT -> x + 4f
        Paint.Align.CENTER -> x + colW / 2f
        Paint.Align.RIGHT -> x + colW - 4f
      }
      c.drawText(clipText(col.header, cursor.thPaint, colW - 6f), tx, cursor.y + 11f, cursor.thPaint)
      x += colW
      if (x < PAGE_WIDTH - MARGIN - 1f) {
        c.drawLine(x, cursor.y, x, cursor.y + headerH, cursor.whiteDividerPaint)
      }
    }
    cursor.y += headerH
  }

  private fun drawTableRow(
    cursor: PdfCursor,
    columns: List<ColSpec>,
    values: List<String>,
    rowH: Float,
    shaded: Boolean,
    bold: Boolean,
  ) {
    val c = cursor.canvas
    if (shaded) {
      cursor.fillPaint.color = if (bold) Color.rgb(232, 240, 254) else Color.rgb(248, 249, 252)
      c.drawRect(MARGIN, cursor.y, PAGE_WIDTH - MARGIN, cursor.y + rowH, cursor.fillPaint)
    }
    c.drawRect(MARGIN, cursor.y, PAGE_WIDTH - MARGIN, cursor.y + rowH, cursor.thinBorderPaint)

    val textPaint = if (bold) cursor.tdBoldPaint else cursor.tdPaint
    var x = MARGIN
    columns.forEachIndexed { idx, col ->
      val colW = CONTENT_WIDTH * col.weight
      textPaint.textAlign = col.align
      val tx = when (col.align) {
        Paint.Align.LEFT -> x + 4f
        Paint.Align.CENTER -> x + colW / 2f
        Paint.Align.RIGHT -> x + colW - 4f
      }
      val raw = values.getOrElse(idx) { "—" }
      c.drawText(clipText(raw, textPaint, colW - 6f), tx, cursor.y + 10.5f, textPaint)
      x += colW
      if (x < PAGE_WIDTH - MARGIN - 1f) {
        c.drawLine(x, cursor.y, x, cursor.y + rowH, cursor.thinBorderPaint)
      }
    }
    cursor.y += rowH
  }

  private class PdfCursor(
    private val context: Context,
    private val pdf: PdfDocument,
    private val headerTitle: String,
    private val headerSubtitle: String,
    private val contdHeader: String,
    private val footerPrefix: String,
  ) {
    var pageNumber = 0
    var y = MARGIN
    private var currentPage: PdfDocument.Page? = null

    val canvas: Canvas
      get() = currentPage!!.canvas

    val fillPaint = Paint().apply { style = Paint.Style.FILL }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(32, 42, 68)
      style = Paint.Style.STROKE
      strokeWidth = 0.85f
    }
    val heavyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      style = Paint.Style.STROKE
      strokeWidth = 1.4f
    }
    val thinBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(205, 212, 224)
      style = Paint.Style.STROKE
      strokeWidth = 0.55f
    }
    val whiteDividerPaint = Paint().apply {
      color = Color.argb(90, 255, 255, 255)
      strokeWidth = 0.6f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      textSize = 14f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(75, 85, 105)
      textSize = 8f
    }
    val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      textSize = 9.2f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(20, 24, 34)
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(80, 90, 110)
      textSize = 7.4f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(20, 24, 34)
      textSize = 7.8f
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(110, 120, 138)
      textSize = 7.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }
    val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(85, 95, 115)
      textSize = 6.7f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val kpiValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      textSize = 9f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 6.9f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val tdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(24, 28, 38)
      textSize = 7.2f
    }
    val tdBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      textSize = 7.3f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(100, 110, 125)
      textSize = 7.2f
      textAlign = Paint.Align.CENTER
    }

    fun startFirstPage() {
      pageNumber = 1
      val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      currentPage = pdf.startPage(info)
      val c = canvas
      c.drawColor(Color.WHITE)

      val bannerH = 50f
      c.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + bannerH, heavyBorderPaint)
      c.drawText(headerTitle, MARGIN + 10f, MARGIN + 21f, titlePaint)
      c.drawText(clipText(headerSubtitle, subtitlePaint, CONTENT_WIDTH - 70f), MARGIN + 10f, MARGIN + 37f, subtitlePaint)
      try {
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_spf_logo)
        if (logoBitmap != null) {
          val logoRect = RectF(PAGE_WIDTH - MARGIN - 54f, MARGIN + 4f, PAGE_WIDTH - MARGIN - 6f, MARGIN + 46f)
          c.drawBitmap(logoBitmap, null, logoRect, null)
        }
      } catch (_: Exception) {
      }
      y = MARGIN + bannerH + 10f
    }

    fun needsPageBreak(heightNeeded: Float): Boolean = (y + heightNeeded) > MAX_BODY_Y

    fun ensureSpace(heightNeeded: Float) {
      if (needsPageBreak(heightNeeded)) {
        newPage()
      }
    }

    fun newPage() {
      drawFooter()
      currentPage?.let { pdf.finishPage(it) }
      pageNumber += 1
      val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      currentPage = pdf.startPage(info)
      val c = canvas
      c.drawColor(Color.WHITE)
      c.drawText(contdHeader, MARGIN, MARGIN + 12f, labelPaint)
      c.drawLine(MARGIN, MARGIN + 16f, PAGE_WIDTH - MARGIN, MARGIN + 16f, heavyBorderPaint)
      y = MARGIN + 24f
    }

    fun finish() {
      drawFooter()
      currentPage?.let { pdf.finishPage(it) }
      currentPage = null
    }

    private fun drawFooter() {
      canvas.drawText(
        "$footerPrefix • Page $pageNumber",
        PAGE_WIDTH / 2f,
        FOOTER_Y,
        footerPaint,
      )
    }
  }

  private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
    if (text.isEmpty() || maxWidth <= 2f) return ""
    if (paint.measureText(text) <= maxWidth) return text
    val count = paint.breakText(text, true, (maxWidth - paint.measureText("…")).coerceAtLeast(1f), null)
    return if (count <= 0) "" else text.take(count) + "…"
  }

  private fun formatDate(epochDay: Long): String {
    if (epochDay <= 0L) return "—"
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = epochDay * MILLIS_PER_DAY
    }
    val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(cal.time)
  }

  private fun fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
  private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
  private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)
  private fun fmtRupee(value: Double): String = "₹${NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
  }.format(value)}"
}
