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
import com.example.data.model.Farmer
import com.example.data.model.FlockBatch
import com.example.data.model.GcStatementDetails
import com.example.data.model.SettlementCalculator
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object GcStatementPdfGenerator {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 24f

  fun generatePdfBytes(
    context: Context,
    batch: FlockBatch,
    farmer: Farmer?,
    todayEpochDay: Long,
  ): ByteArray {
    val details = batch.gcDetails ?: GcStatementDetails(stdGcPerKg = batch.growingRatePerKg)
    val effectiveDay = batch.closedEpochDay.takeIf { it > 0L } ?: todayEpochDay
    val summary = SettlementCalculator.computeGcStatement(
      batch = batch,
      liftedBirds = batch.liftedBirds,
      liftedWeightKg = batch.liftedWeightKg,
      returnedFeedKg = batch.returnedFeedKg,
      details = details,
      todayEpochDay = effectiveDay,
    )

    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    drawStatement(
      context = context,
      canvas = canvas,
      batch = batch,
      farmer = farmer,
      details = details,
      summary = summary,
      effectiveDay = effectiveDay,
    )

    pdf.finishPage(page)
    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
  }

  private fun drawStatement(
    context: Context,
    canvas: Canvas,
    batch: FlockBatch,
    farmer: Farmer?,
    details: GcStatementDetails,
    summary: com.example.data.model.GcStatementSummary,
    effectiveDay: Long,
  ) {
    canvas.drawColor(Color.WHITE)

    val contentWidth = PAGE_WIDTH - (MARGIN * 2f)
    val halfWidth = contentWidth / 2f
    val leftX = MARGIN
    val midX = MARGIN + halfWidth
    val rightX = PAGE_WIDTH - MARGIN

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 0.9f
    }
    val heavyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 1.4f
    }
    val fillPaint = Paint().apply { style = Paint.Style.FILL }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 45, 92)
      textSize = 16.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(30, 30, 30)
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val sectionBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 7.8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }
    val valueCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val valueRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.RIGHT
    }

    // Color palette matching statement template
    val colBlueCell = Color.rgb(156, 184, 232)
    val colGreenCell = Color.rgb(146, 189, 102)
    val colBeigeCell = Color.rgb(252, 238, 214)
    val colCyanBanner = Color.rgb(92, 240, 255)
    val colCyanHighlight = Color.rgb(72, 242, 255)
    val colYellowHighlight = Color.rgb(255, 245, 18)
    val colMagentaHighlight = Color.rgb(245, 36, 238)
    val colWaiverBlue = Color.rgb(104, 152, 206)
    val colMiscGreen = Color.rgb(112, 168, 72)

    // 1. Top Header Box
    var y = MARGIN
    val headerHeight = 68f
    canvas.drawRect(leftX, y, rightX, y + headerHeight, heavyBorderPaint)

    val centerTextX = leftX + (contentWidth - 66f) / 2f
    canvas.drawText(
      details.companyName.ifBlank { "SPF POULTRY FARM" }.uppercase(),
      centerTextX,
      y + 22f,
      titlePaint,
    )
    if (details.companyAddress.isNotBlank()) {
      canvas.drawText(details.companyAddress.uppercase(), centerTextX, y + 36f, subHeaderPaint)
    }
    if (details.companyPhone.isNotBlank()) {
      canvas.drawText("PHONE: ${details.companyPhone}", centerTextX, y + 48f, subHeaderPaint)
    }
    if (details.companyPanGst.isNotBlank()) {
      canvas.drawText(details.companyPanGst.uppercase(), centerTextX, y + 60f, subHeaderPaint)
    }

    try {
      val markBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_spf_logo)
      if (markBitmap != null) {
        val destRect = RectF(rightX - 66f, y + 6f, rightX - 8f, y + 62f)
        canvas.drawBitmap(markBitmap, null, destRect, null)
      }
    } catch (_: Exception) {
    }

    y += headerHeight + 10f

    // Helper to draw a 2-cell half-row
    val rowH = 16.5f
    fun drawHalfRow(
      xStart: Float,
      labelW: Float,
      totalW: Float,
      rowY: Float,
      label: String,
      value: String,
      valueBgColor: Int? = null,
      rightAlignValue: Boolean = false,
    ) {
      val labelEnd = xStart + labelW
      val xEnd = xStart + totalW
      canvas.drawRect(xStart, rowY, labelEnd, rowY + rowH, borderPaint)
      if (valueBgColor != null) {
        fillPaint.color = valueBgColor
        canvas.drawRect(labelEnd, rowY, xEnd, rowY + rowH, fillPaint)
      }
      canvas.drawRect(labelEnd, rowY, xEnd, rowY + rowH, borderPaint)
      canvas.drawText(label, xStart + 5f, rowY + 11.5f, labelPaint)
      if (rightAlignValue) {
        canvas.drawText(value, xEnd - 6f, rowY + 11.5f, valueRightPaint)
      } else {
        canvas.drawText(value, labelEnd + (xEnd - labelEnd) / 2f, rowY + 11.5f, valueCenterPaint)
      }
    }

    val leftMetaRows = listOf(
      Triple("DATE", formatSlashDate(effectiveDay), null),
      Triple("FARMER NAME", (farmer?.farmerName ?: batch.farmerName).uppercase(), colBlueCell),
      Triple("FARM NAME", (farmer?.displayFarmName ?: batch.farmerName).uppercase(), colBlueCell),
      Triple("PAN NO", (farmer?.panNumber ?: "").uppercase().ifBlank { "—" }, colBlueCell),
      Triple("ACCOUNT NO", (farmer?.accountNumber ?: "").ifBlank { "—" }, colBlueCell),
      Triple("BANK", (farmer?.bankName ?: "").ifBlank { "—" }, colBlueCell),
      Triple("IFSC", (farmer?.ifscCode ?: "").uppercase().ifBlank { "—" }, colBlueCell),
      Triple("", "", null),
      Triple("MORTALITY %", "${fmt2(summary.mortalityPercent)} %", null),
      Triple("SHORTAGE FEED BAG", fmt2(details.shortageFeedBags), null),
      Triple("GRADE*", summary.grade, colBeigeCell),
      Triple("EEF", fmt2(summary.eef), null),
      Triple("M.AGE", fmt2(summary.meanAgeDays), null),
    )

    val rightMetaRows = listOf(
      Triple("FLOCK ID", batch.batchCode, colGreenCell),
      Triple("HATCH DATE", formatSlashDate(batch.placementEpochDay), colGreenCell),
      Triple("CHICKS", fmtInt(summary.chicksCount), colGreenCell),
      Triple("FREE CHICKS ${fmtInt(summary.freeChicksPercent.toLong())}%", fmtInt(summary.freeChicksCount), colGreenCell),
      Triple("MORTALITY", fmtInt(summary.mortalityCount), colGreenCell),
      Triple("BIRDS SOLD", fmtInt(summary.birdsSold), colGreenCell),
      Triple("SHORTAGE", fmtInt(summary.shortageBirds), colGreenCell),
      Triple("BIRDS WEIGHT", fmt2(summary.birdsWeightKg), colGreenCell),
      Triple("FEED USED", fmtInt(summary.feedUsedKg.toLong()), colGreenCell),
      Triple("ABW", fmt3(summary.abwKg), colGreenCell),
      Triple("FCR", fmt3(summary.fcr), colGreenCell),
      Triple("SALE RATE", fmt2(details.saleRatePerKg), colGreenCell),
      Triple("SALE RATE INCENTIVE", fmt2(summary.saleRateIncentive), colGreenCell),
    )

    val labelWLeft = 108f
    val labelWRight = 136f
    for (i in leftMetaRows.indices) {
      val rowY = y + i * rowH
      val (lLabel, lVal, lColor) = leftMetaRows[i]
      val (rLabel, rVal, rColor) = rightMetaRows[i]
      drawHalfRow(leftX, labelWLeft, halfWidth, rowY, lLabel, lVal, lColor)
      drawHalfRow(midX, labelWRight, halfWidth, rowY, rLabel, rVal, rColor)
    }

    y += leftMetaRows.size * rowH

    // 2. Cyan Section Banners: COST ANALYSIS | GROWING CHARGE (CG)
    fillPaint.color = colCyanBanner
    canvas.drawRect(leftX, y, rightX, y + 18f, fillPaint)
    canvas.drawRect(leftX, y, midX, y + 18f, borderPaint)
    canvas.drawRect(midX, y, rightX, y + 18f, borderPaint)
    canvas.drawText("COST ANALYSIS", leftX + halfWidth / 2f, y + 12.5f, sectionBannerPaint)
    canvas.drawText("GROWING CHARGE (CG)", midX + halfWidth / 2f, y + 12.5f, sectionBannerPaint)
    y += 18f

    // Cost Analysis columns: Item (92) | Qty (55) | Rate (50) | Amount (halfWidth - 197)
    val c1 = 92f
    val c2 = 55f
    val c3 = 48f
    val c4 = halfWidth - (c1 + c2 + c3)

    data class CostRow(val item: String, val qty: String, val rate: String, val amount: String, val amountBg: Int? = null)
    val costRows = listOf(
      CostRow("CHICKS", fmtInt(summary.chicksCount), fmt2(details.chickRate), fmt2(summary.chicksCost)),
      CostRow("FEED", fmtInt(summary.feedUsedKg.toLong()), fmt2(details.feedRatePerKg), fmt2(summary.feedCost)),
      CostRow("ADMIN", fmtInt(summary.chicksCount), fmt2(details.adminRatePerChick), fmt2(summary.adminCost)),
      CostRow("MEDICINE", fmtInt(summary.chicksCount), fmt2(summary.medicineRatePerChick), fmt2(summary.medicineCost)),
      CostRow("OTHER EXP", "", "", if (summary.otherExpCost != 0.0) fmt2(summary.otherExpCost) else ""),
      CostRow("TOTAL COST", "", "", fmt2(summary.totalCost)),
      CostRow("PRO.COST", "", "", fmt2(summary.productionCostPerKg), colCyanHighlight),
      CostRow("STD.PRO.COST", "", "", fmt2(summary.stdProductionCostPerKg)),
      CostRow("STD GC", "", "", fmt2(summary.stdGcPerKg)),
      CostRow("", "", "", ""),
      CostRow("FINAL GC PER KG", "", "", fmt2(summary.finalGcPerKg)),
      CostRow("FINAL GC PER CHICKS", "", "", fmt2(summary.finalGcPerChick)),
      CostRow("CFCR", "", "", fmt3(summary.cfcr)),
    )

    data class GcRow(val label: String, val amount: String, val bg: Int? = null)
    val gcRows = listOf(
      GcRow("STD GC AMOUNT", fmt2(summary.stdGcAmount), colYellowHighlight),
      GcRow("LOW COST INCENTIVE", fmt2(summary.lowCostIncentive), colYellowHighlight),
      GcRow("SALE RATE INCENTIVE", fmt2(summary.saleRateIncentive), colYellowHighlight),
      GcRow("FEED UNLOADING", fmt2(summary.feedUnloadingAmount), colYellowHighlight),
      GcRow("*MISC", if (summary.miscAmount != 0.0) fmt2(summary.miscAmount) else "", colMiscGreen),
      GcRow("C.FCR WAIVER", fmt2(summary.cfcrWaiverAmount), colWaiverBlue),
      GcRow("HIGH COST DEDUCTION", fmt2(summary.highCostDeduction), colMagentaHighlight),
      GcRow("BIRDS SHORTAGE DEDUCTION", fmt2(summary.birdsShortageDeduction), colMagentaHighlight),
      GcRow("** OTHER DEDUCTION", if (summary.otherDeduction != 0.0) fmt2(summary.otherDeduction) else "", colMagentaHighlight),
      GcRow("WAIVER IF ANY", if (summary.waiverIfAny != 0.0) fmt2(summary.waiverIfAny) else "", null),
      GcRow("TOTAL GC", fmt2(summary.totalGc), colCyanHighlight),
      GcRow("TDS", fmt2(summary.tdsAmount), null),
      GcRow("TOTAL PAYABLE", fmt2(summary.totalPayable), colYellowHighlight),
    )

    val gcLabelW = 136f
    for (i in costRows.indices) {
      val rowY = y + i * rowH
      val cost = costRows[i]
      val gc = gcRows[i]

      // Left 4-col table
      var cx = leftX
      canvas.drawRect(cx, rowY, cx + c1, rowY + rowH, borderPaint)
      canvas.drawText(cost.item, cx + 5f, rowY + 11.5f, labelPaint)
      cx += c1

      canvas.drawRect(cx, rowY, cx + c2, rowY + rowH, borderPaint)
      canvas.drawText(cost.qty, cx + c2 / 2f, rowY + 11.5f, valueCenterPaint)
      cx += c2

      canvas.drawRect(cx, rowY, cx + c3, rowY + rowH, borderPaint)
      canvas.drawText(cost.rate, cx + c3 / 2f, rowY + 11.5f, valueCenterPaint)
      cx += c3

      if (cost.amountBg != null) {
        fillPaint.color = cost.amountBg
        canvas.drawRect(cx, rowY, cx + c4, rowY + rowH, fillPaint)
      }
      canvas.drawRect(cx, rowY, cx + c4, rowY + rowH, borderPaint)
      canvas.drawText(cost.amount, cx + c4 / 2f, rowY + 11.5f, valueCenterPaint)

      // Right 2-col table
      drawHalfRow(midX, gcLabelW, halfWidth, rowY, gc.label, gc.amount, gc.bg)
    }

    y += costRows.size * rowH

    // 3. Signature Row
    val sigH = 40f
    canvas.drawRect(leftX, y, rightX, y + sigH, borderPaint)
    val sigColW = contentWidth / 4f
    val sigs = listOf(
      "CREATED BY :" to details.createdByName,
      "APPROVED BY :" to details.approvedByName,
      "ENTERED BY :" to details.enteredByName,
      "PAYMENT BY :" to details.paymentByName,
    )
    sigs.forEachIndexed { idx, (caption, person) ->
      val sx = leftX + idx * sigColW
      canvas.drawText(caption, sx + 8f, y + 14f, labelPaint)
      if (person.isNotBlank()) {
        canvas.drawText(person, sx + 8f, y + 29f, Paint(labelPaint).apply {
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        })
      }
    }
    y += sigH

    // 4. Bottom Notes Box
    val footH = 38f
    canvas.drawRect(leftX, y, rightX, y + footH, borderPaint)
    canvas.drawText("* MISC DETAIL : ${details.miscDetail}", leftX + 6f, y + 14f, labelPaint)
    canvas.drawText("** OTHER DEDUCTION : ${details.otherDeductionDetail}", leftX + 6f, y + 28f, labelPaint)

    // Outer thick frame around the entire table body
    canvas.drawRect(leftX, MARGIN + headerHeight + 10f, rightX, y + footH, heavyBorderPaint)
  }

  private fun formatSlashDate(epochDay: Long): String {
    if (epochDay <= 0L) return "—"
    return try {
      LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US))
    } catch (_: Exception) {
      epochDay.toString()
    }
  }

  private fun fmtInt(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value)

  private fun fmt2(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return nf.format(value)
  }

  private fun fmt3(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.minimumFractionDigits = 3
    nf.maximumFractionDigits = 3
    return nf.format(value)
  }
}
