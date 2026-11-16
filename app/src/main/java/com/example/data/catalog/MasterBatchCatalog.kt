package com.example.data.catalog

import android.content.Context
import com.example.data.model.BatchStatus
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

data class CatalogBatchRow(
  val docId: String,
  val batchCode: String,
  val region: String,
  val zoneName: String,
  val farmName: String,
  val farmCode: String,
  val normalizedFarmCode: String,
  val placementEpochDay: Long,
  val housedChicks: Long,
  val chicksPlaced: Long,
  val freeChicks: Long,
  val weekNumber: String,
  val challanNo: String,
  val status: BatchStatus,
  val closedEpochDay: Long,
)

object MasterBatchCatalog {
  private const val ASSET_FILE_NAME = "master_batches_catalog.csv"
  private const val MILLIS_PER_DAY = 86_400_000L

  fun loadFromAssets(context: Context): List<CatalogBatchRow> {
    return context.assets.open(ASSET_FILE_NAME).use { input ->
      parse(input)
    }
  }

  fun parse(inputStream: InputStream): List<CatalogBatchRow> {
    val lines = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readLines()
    if (lines.isEmpty()) return emptyList()

    val rows = mutableListOf<CatalogBatchRow>()
    val seenCodes = mutableSetOf<String>()

    lines.drop(1).forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty()) return@forEach
      val cols = line.split("|")
      if (cols.size < 13) return@forEach

      val batchCode = cols[0].trim().uppercase().take(40)
      val region = cols[1].trim().uppercase().take(40)
      val zoneName = cols[2].trim().uppercase().take(80)
      val farmName = cols[3].trim().take(100)
      val farmCode = cols[4].trim().uppercase().take(30)
      val placementEpochDay = parseDdMmYyyyEpochDay(cols[5].trim()) ?: 20300L
      val housed = cols[6].trim().toLongOrNull()?.coerceIn(0L, 500000L) ?: 0L
      val weekNumber = cols[7].trim().take(40)
      val challanNo = cols[8].trim().take(40)
      val doldParsed = cols[9].trim().toLongOrNull()?.coerceIn(0L, 500000L)
      val freeChicks = (cols[10].trim().toLongOrNull() ?: 0L).coerceIn(0L, 50000L)
      val chicksPlaced = (doldParsed ?: max(0L, housed - freeChicks)).coerceIn(0L, 500000L)
      val statusRaw = cols[11].trim().uppercase()
      val status = when (statusRaw) {
        "ACTIVE", "RUNNING" -> BatchStatus.ACTIVE
        "CLOSING" -> BatchStatus.CLOSING
        else -> BatchStatus.CLOSED
      }
      val closedEpochDay = parseStatusDateMmDdYyyyEpochDay(cols[12].trim()) ?: 0L

      if (batchCode.isBlank() || farmCode.isBlank()) return@forEach
      if (!seenCodes.add(batchCode)) return@forEach

      val docId = "bch_cat_${sanitizeIdPart(batchCode)}".take(120)
      val normalizedFarmCode = farmCode.replace(" ", "").uppercase()

      rows += CatalogBatchRow(
        docId = docId,
        batchCode = batchCode,
        region = region,
        zoneName = zoneName,
        farmName = farmName.ifBlank { farmCode },
        farmCode = farmCode,
        normalizedFarmCode = normalizedFarmCode,
        placementEpochDay = placementEpochDay.coerceIn(18000L, 40000L),
        housedChicks = housed,
        chicksPlaced = chicksPlaced,
        freeChicks = freeChicks,
        weekNumber = weekNumber,
        challanNo = challanNo,
        status = status,
        closedEpochDay = if (status == BatchStatus.CLOSED) closedEpochDay.coerceIn(0L, 40000L) else 0L,
      )
    }

    return rows
  }

  private fun parseDdMmYyyyEpochDay(raw: String): Long? {
    val parts = raw.trim().split("-", "/")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return toEpochDay(year, month, day)
  }

  private fun parseStatusDateMmDdYyyyEpochDay(raw: String): Long? {
    if (raw.isBlank()) return null
    val parts = raw.trim().split("-", "/")
    if (parts.size != 3) return null
    val first = parts[0].toIntOrNull() ?: return null
    val second = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return if (first > 12) {
      toEpochDay(year, second, first)
    } else {
      toEpochDay(year, first, second)
    }
  }

  private fun toEpochDay(year: Int, month: Int, day: Int): Long? {
    if (year !in 2020..2035 || month !in 1..12 || day !in 1..31) return null
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      isLenient = false
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
    }
    return try {
      cal.timeInMillis / MILLIS_PER_DAY
    } catch (_: IllegalArgumentException) {
      null
    }
  }

  private fun sanitizeIdPart(raw: String): String {
    return raw.trim().uppercase()
      .replace(Regex("[^A-Z0-9_-]+"), "_")
      .trim('_')
      .ifBlank { "BATCH" }
  }
}
