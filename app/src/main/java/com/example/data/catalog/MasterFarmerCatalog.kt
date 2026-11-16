package com.example.data.catalog

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class CatalogZoneSpec(
  val docId: String,
  val zoneCode: String,
  val zoneName: String,
  val region: String,
)

data class CatalogFarmerRow(
  val docId: String,
  val zoneDocId: String,
  val zoneCode: String,
  val zoneName: String,
  val region: String,
  val farmType: String,
  val farmerName: String,
  val farmName: String,
  val village: String,
  val tehsil: String,
  val phone: String,
  val totalSqft: Long,
  val shedCapacity: Long,
  val farmCode: String,
  val aadhaarNumber: String,
  val panNumber: String,
  val accountNumber: String,
  val bankName: String,
  val ifscCode: String,
  val accountType: String,
)

object MasterFarmerCatalog {
  private const val ASSET_FILE_NAME = "master_farmers_catalog.csv"

  fun loadFromAssets(context: Context): List<CatalogFarmerRow> {
    return context.assets.open(ASSET_FILE_NAME).use { input ->
      parse(input)
    }
  }

  fun parse(inputStream: InputStream): List<CatalogFarmerRow> {
    val lines = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readLines()
    if (lines.isEmpty()) return emptyList()

    val rows = mutableListOf<CatalogFarmerRow>()
    val seenPairs = mutableSetOf<String>()
    val codeCounts = mutableMapOf<String, Int>()

    lines.drop(1).forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty()) return@forEach
      val cols = line.split("|")
      if (cols.size < 16) return@forEach

      val region = cols[0].trim().uppercase().take(40)
      val zoneName = cols[1].trim().uppercase().take(80)
      val farmType = cols[2].trim().uppercase().take(40)
      val farmerName = cols[3].trim().take(80)
      val farmName = cols[4].trim().ifBlank { farmerName }.take(100)
      val village = cols[5].trim().take(100)
      val tehsil = cols[6].trim().take(100)
      val phone = cols[7].trim().take(30)
      val rawSqft = cols[8].trim().toLongOrNull()?.coerceIn(0L, 500000L) ?: 0L
      val farmCode = cols[9].trim().uppercase().take(30)
      val aadhaarNumber = cols[10].trim().take(30)
      val panNumber = cols[11].trim().uppercase().take(30)
      val accountNumber = cols[12].trim().take(40)
      val bankName = cols[13].trim().take(80)
      val ifscCode = cols[14].trim().uppercase().take(30)
      val accountType = cols[15].trim().uppercase().take(30)

      if (farmCode.isBlank() || farmerName.isBlank()) return@forEach

      val pairKey = "${farmCode.uppercase()}|${farmName.uppercase()}"
      if (!seenPairs.add(pairKey)) return@forEach

      val occurrence = (codeCounts[farmCode] ?: 0) + 1
      codeCounts[farmCode] = occurrence
      val cleanCode = sanitizeIdPart(farmCode)
      val docId = if (occurrence == 1) {
        "frm_cat_$cleanCode"
      } else {
        "frm_cat_${cleanCode}_$occurrence"
      }.take(120)

      val cleanZoneIdPart = sanitizeIdPart(zoneName.ifBlank { region })
      val zoneDocId = "zn_cat_$cleanZoneIdPart".take(120)
      val zoneCode = cleanZoneIdPart.replace("_", "-").take(30).ifBlank { "ZN" }
      val shedCapacity = rawSqft.coerceIn(100L, 500000L)

      rows += CatalogFarmerRow(
        docId = docId,
        zoneDocId = zoneDocId,
        zoneCode = zoneCode,
        zoneName = zoneName.ifBlank { region },
        region = region.ifBlank { zoneName },
        farmType = farmType,
        farmerName = farmerName,
        farmName = farmName,
        village = village,
        tehsil = tehsil,
        phone = phone,
        totalSqft = rawSqft,
        shedCapacity = shedCapacity,
        farmCode = farmCode,
        aadhaarNumber = aadhaarNumber,
        panNumber = panNumber,
        accountNumber = accountNumber,
        bankName = bankName,
        ifscCode = ifscCode,
        accountType = accountType,
      )
    }

    return rows
  }

  fun extractDistinctZones(rows: List<CatalogFarmerRow>): List<CatalogZoneSpec> {
    return rows
      .groupBy { it.zoneName.trim().uppercase() }
      .map { (_, group) ->
        val first = group.first()
        CatalogZoneSpec(
          docId = first.zoneDocId,
          zoneCode = first.zoneCode,
          zoneName = first.zoneName,
          region = first.region,
        )
      }
      .sortedBy { it.zoneName }
  }

  private fun sanitizeIdPart(raw: String): String {
    return raw.trim().uppercase()
      .replace(Regex("[^A-Z0-9_-]+"), "_")
      .trim('_')
      .ifBlank { "ITEM" }
  }
}
