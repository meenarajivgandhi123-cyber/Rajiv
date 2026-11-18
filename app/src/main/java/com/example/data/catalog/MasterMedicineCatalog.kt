package com.example.data.catalog

import android.content.Context
import com.example.data.model.MedicineCategory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class CatalogMedicineRow(
  val docId: String,
  val medicineCode: String,
  val medicineName: String,
  val category: String,
  val packing: Double,
  val unit: String,
  val rate: Double,
  val perKgOrLtr: Double,
)

object MasterMedicineCatalog {
  private const val ASSET_FILE_NAME = "master_medicines_catalog.csv"

  fun loadFromAssets(context: Context): List<CatalogMedicineRow> {
    return context.assets.open(ASSET_FILE_NAME).use { input ->
      parse(input)
    }
  }

  fun parse(inputStream: InputStream): List<CatalogMedicineRow> {
    val lines = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readLines()
    if (lines.isEmpty()) return emptyList()

    val rows = mutableListOf<CatalogMedicineRow>()
    val seenCodes = mutableSetOf<String>()

    lines.drop(1).forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty()) return@forEach
      val cols = line.split("|")
      if (cols.size < 6) return@forEach

      val code = cols[0].trim().uppercase().take(30)
      val name = cols[1].trim().take(120)
      val packing = (cols[2].trim().toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100000.0)
      val unit = cols[3].trim().uppercase().take(20)
      val rate = (cols[4].trim().toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10000000.0)
      val perKgOrLtr = (cols[5].trim().toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10000000.0)

      if (code.isBlank() || name.isBlank()) return@forEach
      if (!seenCodes.add(code)) return@forEach

      val docId = "med_cat_${sanitizeIdPart(code)}".take(120)
      val category = inferCategory(code, name)

      rows += CatalogMedicineRow(
        docId = docId,
        medicineCode = code,
        medicineName = name,
        category = category,
        packing = packing,
        unit = unit,
        rate = rate,
        perKgOrLtr = perKgOrLtr,
      )
    }

    return rows
  }

  fun inferCategory(code: String, name: String): String {
    val upperCode = code.trim().uppercase()
    val upperName = name.trim().uppercase()
    return when {
      upperCode.startsWith("V") -> MedicineCategory.VACCINE.value
      upperName.contains("JERR CAN") || upperName.contains("JERRY CAN") || upperName.contains("TEMPERATURE METER") ->
        MedicineCategory.OTHER.value
      upperName.contains("VIRU CLEAN") ||
        upperName.contains("VIRCON") ||
        upperName.contains("H2O2") ||
        upperName.contains("FORMOLIN") ||
        upperName.contains("OMICIDE") ||
        upperName.contains("BIOSCIL") ||
        upperName.contains("SAFE GUARD") ||
        upperName.contains("LERACEPT") ||
        upperName.contains("B-904") ||
        upperName.contains("SOKRENA") ||
        upperName.contains("X-185") ||
        upperName.contains("I FORT") -> MedicineCategory.SANITIZER.value
      upperName.contains("VITA") ||
        upperName.contains("BECOMPLEX") ||
        upperName.contains("CALGOPHOS") ||
        upperName.contains("CALL D") ||
        upperName.contains("HERBAL C") ||
        upperName.contains("E CARE") ||
        upperName.contains("LIVOL") ||
        upperName.contains("AMLYCURE") ||
        upperName.contains("NEERI") ||
        upperName.contains("STRESS") ||
        upperName.contains("ELECRO") ||
        upperName.contains("GROVPLEX") ||
        upperName.contains("PROMIN") ||
        upperName.contains("IMMUTAS") -> MedicineCategory.SUPPLEMENT.value
      else -> MedicineCategory.ANTIBIOTIC.value
    }
  }

  private fun sanitizeIdPart(raw: String): String {
    return raw.trim().uppercase()
      .replace(Regex("[^A-Z0-9_-]+"), "_")
      .trim('_')
      .ifBlank { "MED" }
  }
}
