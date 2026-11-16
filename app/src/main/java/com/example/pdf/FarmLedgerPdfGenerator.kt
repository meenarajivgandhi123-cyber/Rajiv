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
import com.example.data.model.BatchStatus
import com.example.data.model.FarmLedgerData
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object FarmLedgerPdfGenerator {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 22f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)
  private const val FOOTER_Y = PAGE_HEIGHT - 22f
  private const val MAX_BODY_Y = PAGE_HEIGHT - 36f
  private const val MILLIS_PER_DAY = 86_400_000L

  fun generatePdfBytes(
    context: Context,
    ledger: FarmLedgerData,
    todayEpochDay: Long,
  ): ByteArray {
    val pdf = PdfDocument()
    val cursor = PdfCursor(context, pdf, ledger, todayEpochDay)
    cursor.startFirstPage()

    drawFarmProfileAndSummary(cursor, ledger, todayEpochDay)

    val batchCodeById = ledger.batches.associate { it.id to it.batchCode }
    val placementByBatchId = ledger.batches.associate { it.id to it.placementEpochDay }

    // 1. Batch Summary Table
    drawTableSection(
      cursor = cursor,
      title = "1. BATCH PLACEMENTS & GC SUMMARY (${ledger.batches.size})",
      columns = listOf(
        ColSpec("BATCH", 0.11f, Paint.Align.LEFT),
        ColSpec("WK / CHALLAN", 0.13f, Paint.Align.LEFT),
        ColSpec("HATCH", 0.10f, Paint.Align.CENTER),
        ColSpec("STATUS/AGE", 0.13f, Paint.Align.CENTER),
        ColSpec("PLACED", 0.10f, Paint.Align.RIGHT),
        ColSpec("MORT", 0.07f, Paint.Align.RIGHT),
        ColSpec("SOLD WT", 0.09f, Paint.Align.RIGHT),
        ColSpec("FEED (KG)", 0.10f, Paint.Align.RIGHT),
        ColSpec("FCR", 0.06f, Paint.Align.RIGHT),
        ColSpec("PAYABLE", 0.11f, Paint.Align.RIGHT),
      ),
      rows = ledger.batches.map { b ->
        val ageOrClose = when {
          b.statusEnum == BatchStatus.CLOSED && b.closedEpochDay <= 0L -> "Closed"
          b.closedEpochDay > 0L -> "Closed (${b.currentAgeDays(todayEpochDay)}d)"
          else -> "Age ${b.currentAgeDays(todayEpochDay)}d"
        }
        val wkChallan = listOfNotNull(
          b.weekNumber.takeIf { it.isNotBlank() },
          b.challanNo.takeIf { it.isNotBlank() },
        ).joinToString(" / ").ifBlank { "—" }
        listOf(
          b.batchCode,
          wkChallan,
          formatDate(b.placementEpochDay),
          ageOrClose,
          fmtInt(b.totalBirdsStarted),
          fmtInt(b.cumulativeMortality),
          fmt1(b.liftedWeightKg),
          fmt0(b.netFeedConsumedKg),
          if (b.effectiveFcr > 0.0) fmt2(b.effectiveFcr) else "—",
          if (b.settlementPayout > 0.0) fmtRupee(b.settlementPayout) else "—",
        )
      },
      emptyMessage = "No batches placed on this farm yet.",
    )

    // 2. Daily Performance Logs
    drawTableSection(
      cursor = cursor,
      title = "2. DAILY PERFORMANCE LOGS (${ledger.dailyLogs.size})",
      columns = listOf(
        ColSpec("BATCH", 0.12f, Paint.Align.LEFT),
        ColSpec("DATE", 0.12f, Paint.Align.CENTER),
        ColSpec("AGE", 0.07f, Paint.Align.CENTER),
        ColSpec("MORT/CULL", 0.11f, Paint.Align.RIGHT),
        ColSpec("FEED (KG)", 0.11f, Paint.Align.RIGHT),
        ColSpec("WT (G)", 0.09f, Paint.Align.RIGHT),
        ColSpec("TEMP", 0.08f, Paint.Align.RIGHT),
        ColSpec("LOOSE/STK", 0.12f, Paint.Align.RIGHT),
        ColSpec("NOTES", 0.18f, Paint.Align.LEFT),
      ),
      rows = ledger.dailyLogs.map { log ->
        listOf(
          batchCodeById[log.batchId] ?: "—",
          formatDate(log.logEpochDay),
          "D${log.dayNumber}",
          "${log.mortalityCount}/${log.cullCount}",
          fmt1(log.feedConsumedKg),
          log.avgWeightGrams.toInt().toString(),
          fmt1(log.shedTempCelsius),
          "${log.looseBirds}/${log.feederStock}",
          log.notes.ifBlank { "—" },
        )
      },
      emptyMessage = "No daily performance logs recorded.",
    )

    // 3. Feed Dispatches
    drawTableSection(
      cursor = cursor,
      title = "3. FEED ENTRY / DISPATCHES (${ledger.feedDispatches.size} • ${fmtInt(ledger.totalFeedDispatchedBags)} bags)",
      columns = listOf(
        ColSpec("BATCH", 0.15f, Paint.Align.LEFT),
        ColSpec("DATE", 0.14f, Paint.Align.CENTER),
        ColSpec("FEED PHASE", 0.18f, Paint.Align.LEFT),
        ColSpec("CHALLAN NO", 0.21f, Paint.Align.LEFT),
        ColSpec("BAGS", 0.14f, Paint.Align.RIGHT),
        ColSpec("WEIGHT (KG)", 0.18f, Paint.Align.RIGHT),
      ),
      rows = ledger.feedDispatches.map { d ->
        val challanVeh = listOfNotNull(
          d.challanNo.takeIf { it.isNotBlank() },
          d.vehicleNo.takeIf { it.isNotBlank() },
        ).joinToString(" / ").ifBlank { "—" }
        listOf(
          d.batchCode,
          formatDate(d.dispatchEpochDay),
          d.feedType,
          challanVeh,
          fmtInt(d.bagsCount),
          fmt0(d.weightKg),
        )
      },
      emptyMessage = "No feed dispatches recorded.",
    )

    // 4. Medicine & Vaccine Entries
    drawTableSection(
      cursor = cursor,
      title = "4. MEDICINE & VACCINE ENTRIES (${ledger.medicineEntries.size} • Total ${fmtRupee(ledger.totalMedicineCost)})",
      columns = listOf(
        ColSpec("BATCH", 0.13f, Paint.Align.LEFT),
        ColSpec("DATE", 0.12f, Paint.Align.CENTER),
        ColSpec("MEDICINE NAME", 0.23f, Paint.Align.LEFT),
        ColSpec("CATEGORY", 0.16f, Paint.Align.LEFT),
        ColSpec("DOSAGE", 0.12f, Paint.Align.LEFT),
        ColSpec("SLIP", 0.11f, Paint.Align.LEFT),
        ColSpec("COST (RS)", 0.13f, Paint.Align.RIGHT),
      ),
      rows = ledger.medicineEntries.map { m ->
        listOf(
          m.batchCode,
          formatDate(m.entryEpochDay),
          m.medicineName,
          m.category,
          m.dosageOrQty.ifBlank { "—" },
          m.invoiceOrSlipNo.ifBlank { "—" },
          fmtRupee(m.totalCost),
        )
      },
      emptyMessage = "No medicine or vaccine entries recorded.",
    )

    // 5. Inter-Feed Transfers
    drawTableSection(
      cursor = cursor,
      title = "5. INTER-FEED TRANSFERS (${ledger.feedTransfers.size} • IN ${ledger.totalFeedTransferredInBags}b / OUT ${ledger.totalFeedTransferredOutBags}b)",
      columns = listOf(
        ColSpec("DIR", 0.08f, Paint.Align.CENTER),
        ColSpec("DATE", 0.13f, Paint.Align.CENTER),
        ColSpec("SLIP REF", 0.15f, Paint.Align.LEFT),
        ColSpec("FROM -> TO", 0.30f, Paint.Align.LEFT),
        ColSpec("PHASE", 0.14f, Paint.Align.LEFT),
        ColSpec("BAGS", 0.09f, Paint.Align.RIGHT),
        ColSpec("WT (KG)", 0.11f, Paint.Align.RIGHT),
      ),
      rows = ledger.feedTransfers.map { tr ->
        val isIncoming = tr.toFarmerId == ledger.farmer.id || ledger.batches.any { it.id == tr.toBatchId }
        val sign = if (isIncoming) "+" else "-"
        listOf(
          if (isIncoming) "IN" else "OUT",
          formatDate(tr.transferEpochDay),
          tr.transferRef,
          "${tr.fromFarmCode}(${tr.fromBatchCode}) -> ${tr.toFarmCode}(${tr.toBatchCode})",
          tr.feedType,
          "$sign${tr.bagsCount}",
          "$sign${fmt0(tr.weightKg)}",
        )
      },
      emptyMessage = "No inter-feed transfers recorded.",
    )

    // 6. Birds Sales Entries
    drawTableSection(
      cursor = cursor,
      title = "6. BIRDS SALES ENTRIES (${ledger.birdSales.size} • Total ${fmtRupee(ledger.totalBirdsSaleAmount)})",
      columns = listOf(
        ColSpec("BATCH", 0.10f, Paint.Align.LEFT),
        ColSpec("DATE", 0.10f, Paint.Align.CENTER),
        ColSpec("AGE", 0.06f, Paint.Align.CENTER),
        ColSpec("BUYER / TRADER", 0.17f, Paint.Align.LEFT),
        ColSpec("CHALLAN / VEH", 0.14f, Paint.Align.LEFT),
        ColSpec("BIRDS", 0.08f, Paint.Align.RIGHT),
        ColSpec("WT (KG)", 0.09f, Paint.Align.RIGHT),
        ColSpec("AVG WT", 0.08f, Paint.Align.RIGHT),
        ColSpec("RATE", 0.07f, Paint.Align.RIGHT),
        ColSpec("AMOUNT", 0.11f, Paint.Align.RIGHT),
      ),
      rows = ledger.birdSales.map { s ->
        val ref = listOfNotNull(
          s.challanNo.takeIf { it.isNotBlank() },
          s.vehicleNo.takeIf { it.isNotBlank() },
        ).joinToString(" / ").ifBlank { "—" }
        val customerLabel = listOfNotNull(
          s.buyerName,
          s.buyerPhone.takeIf { it.isNotBlank() },
        ).joinToString(" • ")
        val resolvedAge = s.resolvedAgeDays(placementByBatchId[s.batchId])
        val resolvedAvgWt = s.resolvedAvgWeightKg()
        listOf(
          s.batchCode,
          formatDate(s.saleEpochDay),
          "D$resolvedAge",
          customerLabel,
          ref,
          fmtInt(s.birdsSold),
          fmt1(s.totalWeightKg),
          if (resolvedAvgWt > 0.0) fmt3(resolvedAvgWt) else "—",
          fmt2(s.ratePerKg),
          fmtRupee(s.saleAmount),
        )
      },
      emptyMessage = "No birds sales recorded.",
    )

    cursor.finish()
    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
  }

  private fun drawFarmProfileAndSummary(
    cursor: PdfCursor,
    ledger: FarmLedgerData,
    todayEpochDay: Long,
  ) {
    val farmer = ledger.farmer
    val canvas = cursor.canvas
    val startX = MARGIN
    val endX = PAGE_WIDTH - MARGIN

    // Profile metadata table (6 rows x 2 halves)
    val rowH = 16f
    val halfW = CONTENT_WIDTH / 2f
    val leftLabelW = 78f
    val rightLabelW = 82f

    val scopeText = if (ledger.selectedBatch != null) {
      "BATCH ${ledger.selectedBatch.batchCode} (${ledger.selectedBatch.statusEnum.label})"
    } else {
      "ALL BATCHES ON FARM (${ledger.batches.size})"
    }
    val bankSummary = listOfNotNull(
      farmer.bankName.takeIf { it.isNotBlank() },
      farmer.accountNumber.takeIf { it.isNotBlank() },
      farmer.ifscCode.takeIf { it.isNotBlank() },
      farmer.accountType.takeIf { it.isNotBlank() },
    ).joinToString(" • ").ifBlank { "—" }

    val zoneRegionSummary = listOfNotNull(
      farmer.region.takeIf { it.isNotBlank() },
      farmer.zoneName.ifBlank { "Unassigned" },
    ).joinToString(" • ")

    val kycSummary = listOfNotNull(
      farmer.panNumber.takeIf { it.isNotBlank() }?.let { "PAN:$it" },
      farmer.aadhaarNumber.takeIf { it.isNotBlank() }?.let { "AADHAAR:$it" },
    ).joinToString(" / ").ifBlank { "—" }

    val profileRows = listOf(
      Pair("FARM CODE" to farmer.farmCode, "SCOPE" to scopeText),
      Pair("FARM NAME" to farmer.displayFarmName.uppercase(), "REGION / ZONE" to zoneRegionSummary),
      Pair("FARMER" to farmer.farmerName, "FARM TYPE" to farmer.farmType.ifBlank { "OWN FARM" }),
      Pair("VILLAGE / TEHSIL" to listOfNotNull(farmer.village.takeIf { it.isNotBlank() }, farmer.tehsil.takeIf { it.isNotBlank() }).joinToString(" / ").ifBlank { "—" }, "PHONE" to farmer.phone.ifBlank { "—" }),
      Pair("TOTAL SQ.FT" to "${fmtInt(farmer.effectiveSqft)} sq.ft (${farmer.shedType})", "RATE / KG" to fmtRupee(farmer.growingRatePerKg)),
      Pair("PAN / AADHAAR" to kycSummary, "STATUS" to farmer.statusEnum.label),
      Pair("BANK / A/C TYPE" to bankSummary, "CAP (BIRDS)" to "${fmtInt(farmer.shedCapacity)} birds"),
    )

    profileRows.forEach { (leftPair, rightPair) ->
      cursor.ensureSpace(rowH)
      val y = cursor.y
      val c = cursor.canvas
      // Left cell pair
      cursor.fillPaint.color = cursor.colLabelTint
      c.drawRect(startX, y, startX + leftLabelW, y + rowH, cursor.fillPaint)
      c.drawRect(startX, y, startX + leftLabelW, y + rowH, cursor.borderPaint)
      c.drawRect(startX + leftLabelW, y, startX + halfW, y + rowH, cursor.borderPaint)
      c.drawText(leftPair.first, startX + 4f, y + 11f, cursor.labelPaint)
      c.drawText(
        clipText(leftPair.second, cursor.cellLeftPaint, halfW - leftLabelW - 8f),
        startX + leftLabelW + 4f,
        y + 11f,
        cursor.cellLeftPaint,
      )

      // Right cell pair
      val midX = startX + halfW
      cursor.fillPaint.color = cursor.colLabelTint
      c.drawRect(midX, y, midX + rightLabelW, y + rowH, cursor.fillPaint)
      c.drawRect(midX, y, midX + rightLabelW, y + rowH, cursor.borderPaint)
      c.drawRect(midX + rightLabelW, y, endX, y + rowH, cursor.borderPaint)
      c.drawText(rightPair.first, midX + 4f, y + 11f, cursor.labelPaint)
      c.drawText(
        clipText(rightPair.second, cursor.cellLeftPaint, halfW - rightLabelW - 8f),
        midX + rightLabelW + 4f,
        y + 11f,
        cursor.cellLeftPaint,
      )
      cursor.y += rowH
    }

    cursor.y += 8f

    // Cumulative KPI grid (2 rows x 4 boxes)
    val kpiBoxH = 34f
    val boxW = CONTENT_WIDTH / 4f
    val kpis = listOf(
      "CHICKS PLACED (+FREE)" to "${fmtInt(ledger.totalChicksPlaced)} (+${fmtInt(ledger.totalFreeChicks)})",
      "MORTALITY / SURVIVING" to "${fmtInt(ledger.totalMortality)} / ${fmtInt(ledger.totalSurvivingBirds)}",
      "FEED RECD / CONSUMED" to "${fmtInt(ledger.totalFeedDispatchedBags)}b / ${fmt0(ledger.totalFeedConsumedKg)}kg",
      "INTER-FEED (IN / OUT)" to "+${ledger.totalFeedTransferredInBags}b / -${ledger.totalFeedTransferredOutBags}b",
      "BIRDS SOLD / WT" to "${fmtInt(ledger.totalBirdsSold)} (${fmt1(ledger.totalBirdsSoldWeightKg)}kg)",
      "BIRD SALES VALUE" to fmtRupee(ledger.totalBirdsSaleAmount),
      "MEDICINE TOTAL COST" to fmtRupee(ledger.totalMedicineCost),
      "GC PAYABLE TOTAL" to fmtRupee(ledger.totalSettlementPayable),
    )

    kpis.chunked(4).forEach { rowItems ->
      cursor.ensureSpace(kpiBoxH)
      val y = cursor.y
      val c = cursor.canvas
      rowItems.forEachIndexed { i, (label, value) ->
        val bx = startX + i * boxW
        cursor.fillPaint.color = cursor.colKpiBg
        c.drawRect(bx, y, bx + boxW, y + kpiBoxH, cursor.fillPaint)
        c.drawRect(bx, y, bx + boxW, y + kpiBoxH, cursor.borderPaint)
        c.drawText(
          clipText(label, cursor.kpiLabelPaint, boxW - 8f),
          bx + 5f,
          y + 12f,
          cursor.kpiLabelPaint,
        )
        c.drawText(
          clipText(value, cursor.kpiValuePaint, boxW - 8f),
          bx + 5f,
          y + 26f,
          cursor.kpiValuePaint,
        )
      }
      cursor.y += kpiBoxH
    }
    cursor.y += 10f
  }

  private data class ColSpec(
    val title: String,
    val weight: Float,
    val align: Paint.Align,
  )

  private fun drawTableSection(
    cursor: PdfCursor,
    title: String,
    columns: List<ColSpec>,
    rows: List<List<String>>,
    emptyMessage: String,
  ) {
    val startX = MARGIN
    val sectionTitleH = 18f
    val headerH = 17f
    val rowH = 15f

    val colWidths = FloatArray(columns.size) { i -> columns[i].weight * CONTENT_WIDTH }
    val xPos = FloatArray(columns.size + 1)
    xPos[0] = startX
    for (i in colWidths.indices) {
      xPos[i + 1] = xPos[i] + colWidths[i]
    }

    fun drawSectionTitle() {
      val c = cursor.canvas
      cursor.fillPaint.color = cursor.colNavyHeader
      c.drawRect(startX, cursor.y, startX + CONTENT_WIDTH, cursor.y + sectionTitleH, cursor.fillPaint)
      c.drawRect(startX, cursor.y, startX + CONTENT_WIDTH, cursor.y + sectionTitleH, cursor.borderPaint)
      c.drawText(title, startX + 6f, cursor.y + 12.5f, cursor.sectionHeaderPaint)
      cursor.y += sectionTitleH
    }

    fun drawColumnHeader() {
      val c = cursor.canvas
      cursor.fillPaint.color = cursor.colColHeaderBg
      c.drawRect(startX, cursor.y, startX + CONTENT_WIDTH, cursor.y + headerH, cursor.fillPaint)
      columns.forEachIndexed { i, col ->
        c.drawRect(xPos[i], cursor.y, xPos[i + 1], cursor.y + headerH, cursor.borderPaint)
        val cx = when (col.align) {
          Paint.Align.LEFT -> xPos[i] + 3f
          Paint.Align.CENTER -> (xPos[i] + xPos[i + 1]) / 2f
          Paint.Align.RIGHT -> xPos[i + 1] - 3f
        }
        val paint = cursor.colHeaderPaint(col.align)
        c.drawText(clipText(col.title, paint, colWidths[i] - 5f), cx, cursor.y + 11.5f, paint)
      }
      cursor.y += headerH
    }

    // Ensure enough room for title + column header + at least 1 row
    cursor.ensureSpace(sectionTitleH + headerH + rowH)
    drawSectionTitle()

    if (rows.isEmpty()) {
      cursor.ensureSpace(rowH)
      val c = cursor.canvas
      c.drawRect(startX, cursor.y, startX + CONTENT_WIDTH, cursor.y + rowH, cursor.borderPaint)
      c.drawText(emptyMessage, startX + 6f, cursor.y + 11f, cursor.emptyPaint)
      cursor.y += rowH + 10f
      return
    }

    drawColumnHeader()

    rows.forEachIndexed { rowIdx, cells ->
      if (cursor.needsPageBreak(rowH)) {
        cursor.newPage()
        drawColumnHeader()
      }
      val c = cursor.canvas
      if (rowIdx % 2 == 1) {
        cursor.fillPaint.color = cursor.colAltRow
        c.drawRect(startX, cursor.y, startX + CONTENT_WIDTH, cursor.y + rowH, cursor.fillPaint)
      }
      cells.forEachIndexed { i, rawVal ->
        val col = columns[i]
        c.drawRect(xPos[i], cursor.y, xPos[i + 1], cursor.y + rowH, cursor.borderPaint)
        val cx = when (col.align) {
          Paint.Align.LEFT -> xPos[i] + 3f
          Paint.Align.CENTER -> (xPos[i] + xPos[i + 1]) / 2f
          Paint.Align.RIGHT -> xPos[i + 1] - 3f
        }
        val paint = cursor.cellPaint(col.align)
        c.drawText(clipText(rawVal, paint, colWidths[i] - 5f), cx, cursor.y + 10.5f, paint)
      }
      cursor.y += rowH
    }

    cursor.y += 10f
  }

  private class PdfCursor(
    private val context: Context,
    private val pdf: PdfDocument,
    private val ledger: FarmLedgerData,
    private val todayEpochDay: Long,
  ) {
    private var currentPage: PdfDocument.Page? = null
    private var pageNumber: Int = 0
    var y: Float = MARGIN

    val canvas: Canvas
      get() = currentPage!!.canvas

    val fillPaint = Paint().apply { style = Paint.Style.FILL }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(190, 198, 212)
      style = Paint.Style.STROKE
      strokeWidth = 0.6f
    }
    val heavyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(22, 48, 92)
      style = Paint.Style.STROKE
      strokeWidth = 1.2f
    }

    val colNavyHeader = Color.rgb(22, 48, 92)
    val colLabelTint = Color.rgb(236, 241, 249)
    val colKpiBg = Color.rgb(244, 248, 255)
    val colColHeaderBg = Color.rgb(224, 232, 245)
    val colAltRow = Color.rgb(248, 250, 253)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(22, 48, 92)
      textSize = 14f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(68, 76, 92)
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      textAlign = Paint.Align.LEFT
    }
    val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 8.2f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(32, 40, 56)
      textSize = 7f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }
    val cellLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 6.9f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      textAlign = Paint.Align.LEFT
    }
    private val cellCenterPaint = Paint(cellLeftPaint).apply { textAlign = Paint.Align.CENTER }
    private val cellRightPaint = Paint(cellLeftPaint).apply { textAlign = Paint.Align.RIGHT }

    private val colHdrLeft = Paint(labelPaint).apply { textSize = 6.7f; textAlign = Paint.Align.LEFT }
    private val colHdrCenter = Paint(colHdrLeft).apply { textAlign = Paint.Align.CENTER }
    private val colHdrRight = Paint(colHdrLeft).apply { textAlign = Paint.Align.RIGHT }

    val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(75, 86, 108)
      textSize = 6.3f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }
    val kpiValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      textSize = 8.6f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }
    val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(110, 118, 132)
      textSize = 7f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
      textAlign = Paint.Align.LEFT
    }
    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(100, 108, 122)
      textSize = 7f
      textAlign = Paint.Align.CENTER
    }

    fun colHeaderPaint(align: Paint.Align): Paint = when (align) {
      Paint.Align.LEFT -> colHdrLeft
      Paint.Align.CENTER -> colHdrCenter
      Paint.Align.RIGHT -> colHdrRight
    }

    fun cellPaint(align: Paint.Align): Paint = when (align) {
      Paint.Align.LEFT -> cellLeftPaint
      Paint.Align.CENTER -> cellCenterPaint
      Paint.Align.RIGHT -> cellRightPaint
    }

    fun startFirstPage() {
      pageNumber = 1
      val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      currentPage = pdf.startPage(info)
      val c = canvas
      c.drawColor(Color.WHITE)

      // Top banner with SPF Logo
      val bannerH = 52f
      c.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + bannerH, heavyBorderPaint)
      c.drawText("SPF POULTRY • FARM-WISE LEDGER", MARGIN + 10f, MARGIN + 22f, titlePaint)
      val scopeTag = ledger.selectedBatch?.batchCode?.let { "Batch $it" } ?: "Full Farm History"
      c.drawText(
        "Farm ${ledger.farmer.farmCode} (${ledger.farmer.displayFarmName}) • $scopeTag • Date ${formatDate(todayEpochDay)}",
        MARGIN + 10f,
        MARGIN + 38f,
        subtitlePaint,
      )
      try {
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_spf_logo)
        if (logoBitmap != null) {
          val logoRect = RectF(PAGE_WIDTH - MARGIN - 54f, MARGIN + 4f, PAGE_WIDTH - MARGIN - 6f, MARGIN + 48f)
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
      c.drawText(
        "SPF Farm Ledger • ${ledger.farmer.farmCode} (${ledger.farmer.displayFarmName}) — Contd.",
        MARGIN,
        MARGIN + 12f,
        labelPaint,
      )
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
        "SPF Poultry Integration • Farm Ledger (${ledger.farmer.farmCode}) • Page $pageNumber",
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

  private fun fmtInt(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)
  private fun fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
  private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
  private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)
  private fun fmt3(value: Double): String = String.format(Locale.US, "%.3f", value)
  private fun fmtRupee(value: Double): String = "₹${NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
  }.format(value)}"
}
