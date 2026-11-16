package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.round

enum class OperatorRole(val value: String, val label: String, val subtitle: String) {
  SUPER_ADMIN("super_admin", "Super Admin", "Full system, staff & settlement control"),
  ADMIN("admin", "Admin", "Farms, batches, closing & settlements"),
  USER("user", "User (Supervisor)", "Daily farm logs & feed dispatches");

  companion object {
    fun fromValue(value: String): OperatorRole =
      entries.firstOrNull { it.value == value } ?: USER
  }
}

@IgnoreExtraProperties
data class OperatorAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val empCode: String = "",
  val fullName: String = "",
  val role: String = OperatorRole.USER.value,
  val passwordHash: String = "",
  val isActive: Boolean = true,
  val allowPreviousDateEntry: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val roleEnum: OperatorRole
    get() = OperatorRole.fromValue(role)

  val canSelectPreviousDate: Boolean
    get() = roleEnum == OperatorRole.SUPER_ADMIN || allowPreviousDateEntry
}

object PasswordHasher {
  private val random = SecureRandom()

  fun hashPassword(password: String): String {
    val saltBytes = ByteArray(8)
    random.nextBytes(saltBytes)
    val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
    val digestHex = sha256Hex("$saltHex:$password")
    return "$saltHex:$digestHex"
  }

  fun verifyPassword(password: String, storedHash: String): Boolean {
    val parts = storedHash.split(":", limit = 2)
    if (parts.size != 2) return false
    val expected = sha256Hex("${parts[0]}:$password")
    return MessageDigest.isEqual(expected.toByteArray(), parts[1].toByteArray())
  }

  private fun sha256Hex(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}

@IgnoreExtraProperties
data class IntegrationZone(
  @DocumentId val id: String = "",
  val userId: String = "",
  val zoneCode: String = "",
  val zoneName: String = "",
  val region: String = "",
  val supervisorName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class FarmStatus(val value: String, val label: String) {
  ACTIVE("active", "Active Farm"),
  CLOSING("closing", "Closing Farm"),
  EMPTY("empty", "Empty Farm"),
  CANCELLED("cancelled", "Cancelled");

  companion object {
    fun fromValue(value: String): FarmStatus =
      entries.firstOrNull { it.value == value } ?: EMPTY
  }
}

enum class BatchStatus(val value: String, val label: String) {
  ACTIVE("active", "Active"),
  CLOSING("closing", "Closing / Lifting"),
  CLOSED("closed", "GC Closed");

  companion object {
    fun fromValue(value: String): BatchStatus =
      entries.firstOrNull { it.value == value } ?: ACTIVE
  }
}

enum class ShedType(val value: String) {
  EC("EC"),
  OPEN_SIDED("Open-Sided"),
  SEMI_EC("Semi-EC");

  companion object {
    val options: List<String> = entries.map { it.value }
  }
}

enum class FarmTypeOption(val value: String) {
  OWN_FARM("OWN FARM"),
  RENTED_FARM("RENTED FARM"),
  OWNED_FARM("OWNED FARM");

  companion object {
    val options: List<String> = entries.map { it.value }
  }
}

enum class AccountTypeOption(val value: String) {
  SAVING("SAVING"),
  CURRENT("CURRENT");

  companion object {
    val options: List<String> = entries.map { it.value }
  }
}

enum class FeedType(val value: String) {
  PRE_STARTER("Pre-Starter"),
  STARTER("Starter"),
  GROWER("Grower"),
  FINISHER("Finisher");

  companion object {
    val options: List<String> = entries.map { it.value }
  }
}

@IgnoreExtraProperties
data class Farmer(
  @DocumentId val id: String = "",
  val userId: String = "",
  val farmCode: String = "",
  val farmerName: String = "",
  val farmName: String = "",
  val phone: String = "",
  val village: String = "",
  val tehsil: String = "",
  val region: String = "",
  val farmType: String = "",
  val zoneId: String = "",
  val zoneName: String = "",
  val panNumber: String = "",
  val aadhaarNumber: String = "",
  val accountNumber: String = "",
  val bankName: String = "",
  val ifscCode: String = "",
  val accountType: String = "",
  val shedType: String = ShedType.OPEN_SIDED.value,
  val shedCapacity: Long = 5000L,
  val totalSqft: Long = 0L,
  val growingRatePerKg: Double = 9.0,
  val farmStatus: String = FarmStatus.EMPTY.value,
  val currentBatchId: String = "",
  val currentBatchCode: String = "",
  val cancelReason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val statusEnum: FarmStatus
    get() = FarmStatus.fromValue(farmStatus)

  val displayFarmName: String
    get() = farmName.ifBlank { farmerName }

  val effectiveSqft: Long
    get() = if (totalSqft > 0L) totalSqft else shedCapacity
}

@IgnoreExtraProperties
data class GcStatementDetails(
  val companyName: String = "BROILER INTEGRATOR FARM",
  val companyAddress: String = "",
  val companyPhone: String = "",
  val companyPanGst: String = "",
  val chickRate: Double = 35.0,
  val feedRatePerKg: Double = 40.0,
  val adminRatePerChick: Double = 2.0,
  val medicineCost: Double = 0.0,
  val otherExpCost: Double = 0.0,
  val stdProductionCost: Double = 82.0,
  val stdGcPerKg: Double = 9.0,
  val saleRatePerKg: Double = 85.0,
  val saleRateIncentive: Double = 0.0,
  val lowCostIncentive: Double = 0.0,
  val feedUnloadingAmount: Double = 0.0,
  val miscAmount: Double = 0.0,
  val cfcrWaiverAmount: Double = 0.0,
  val highCostDeduction: Double = 0.0,
  val birdsShortageDeduction: Double = 0.0,
  val otherDeduction: Double = 0.0,
  val waiverIfAny: Double = 0.0,
  val tdsPercent: Double = 1.0,
  val shortageBirds: Long = 0L,
  val shortageFeedBags: Double = 0.0,
  val grade: String = "A",
  val meanAgeDays: Double = 0.0,
  val miscDetail: String = "",
  val otherDeductionDetail: String = "",
  val createdByName: String = "",
  val approvedByName: String = "",
  val enteredByName: String = "",
  val paymentByName: String = "",
)

@IgnoreExtraProperties
data class FlockBatch(
  @DocumentId val id: String = "",
  val userId: String = "",
  val batchCode: String = "",
  val farmerId: String = "",
  val farmerName: String = "",
  val farmName: String = "",
  val farmCode: String = "",
  val region: String = "",
  val zoneName: String = "",
  val weekNumber: String = "",
  val challanNo: String = "",
  val breed: String = "Cobb 500",
  val chicksPlaced: Long = 0L,
  val freeChicks: Long = 0L,
  val placementEpochDay: Long = 20700L,
  val targetMarketAgeDays: Long = 38L,
  val targetAvgWeightKg: Double = 2.2,
  val standardFcr: Double = 1.62,
  val growingRatePerKg: Double = 9.0,
  val status: String = BatchStatus.ACTIVE.value,
  val cumulativeMortality: Long = 0L,
  val cumulativeFeedKg: Double = 0.0,
  val latestAvgWeightGrams: Double = 42.0,
  val liftedBirds: Long = 0L,
  val liftedWeightKg: Double = 0.0,
  val returnedFeedKg: Double = 0.0,
  val settlementPayout: Double = 0.0,
  val closingNotes: String = "",
  val closedEpochDay: Long = 0L,
  val gcDetails: GcStatementDetails? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val statusEnum: BatchStatus
    get() = BatchStatus.fromValue(status)

  val displayFarmName: String
    get() = farmName.ifBlank { farmerName }

  val totalBirdsStarted: Long
    get() = chicksPlaced + freeChicks

  val survivingBirds: Long
    get() = max(0L, totalBirdsStarted - cumulativeMortality - liftedBirds)

  val mortalityPercent: Double
    get() = if (totalBirdsStarted > 0L) (cumulativeMortality.toDouble() / totalBirdsStarted.toDouble()) * 100.0 else 0.0

  val livabilityPercent: Double
    get() = max(0.0, 100.0 - mortalityPercent)

  fun currentAgeDays(todayEpochDay: Long): Long {
    val endDay = if (closedEpochDay > 0L) closedEpochDay else todayEpochDay
    return max(0L, endDay - placementEpochDay)
  }

  fun displayAgeLabel(todayEpochDay: Long): String {
    return if (statusEnum == BatchStatus.CLOSED && closedEpochDay <= 0L) {
      "Closed"
    } else {
      "Day ${currentAgeDays(todayEpochDay)}"
    }
  }

  val liveBiomassKg: Double
    get() = if (liftedWeightKg > 0.0) {
      liftedWeightKg
    } else {
      (survivingBirds.toDouble() * latestAvgWeightGrams) / 1000.0
    }

  val netFeedConsumedKg: Double
    get() = max(0.0, cumulativeFeedKg - returnedFeedKg)

  val effectiveFcr: Double
    get() = if (liveBiomassKg > 1.0) netFeedConsumedKg / liveBiomassKg else 0.0

  fun computeEpef(todayEpochDay: Long): Double {
    val fcr = effectiveFcr
    val age = currentAgeDays(todayEpochDay).toDouble()
    val avgWeightKg = if (liftedBirds > 0L && liftedWeightKg > 0.0) {
      liftedWeightKg / liftedBirds.toDouble()
    } else {
      latestAvgWeightGrams / 1000.0
    }
    if (fcr <= 0.1 || age <= 0.0 || avgWeightKg <= 0.0) return 0.0
    return (livabilityPercent * avgWeightKg * 100.0) / (age * fcr)
  }
}

@IgnoreExtraProperties
data class DailyLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val batchId: String = "",
  val farmerId: String = "",
  val dayNumber: Long = 0L,
  val logEpochDay: Long = 20700L,
  val mortalityCount: Long = 0L,
  val cullCount: Long = 0L,
  val feedConsumedKg: Double = 0.0,
  val avgWeightGrams: Double = 0.0,
  val shedTempCelsius: Double = 30.0,
  val looseBirds: Long = 0L,
  val feederStock: Long = 0L,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FeedDispatch(
  @DocumentId val id: String = "",
  val userId: String = "",
  val batchId: String = "",
  val farmerId: String = "",
  val farmerName: String = "",
  val farmCode: String = "",
  val farmName: String = "",
  val batchCode: String = "",
  val feedType: String = FeedType.STARTER.value,
  val bagsCount: Long = 0L,
  val weightKg: Double = 0.0,
  val challanNo: String = "",
  val vehicleNo: String = "",
  val remarks: String = "",
  val dispatchEpochDay: Long = 20700L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val resolvedDestinationFarm: String
    get() = listOfNotNull(
      farmCode.takeIf { it.isNotBlank() },
      farmName.ifBlank { farmerName }.takeIf { it.isNotBlank() },
    ).joinToString(" • ")
}

@IgnoreExtraProperties
data class BirdSale(
  @DocumentId val id: String = "",
  val userId: String = "",
  val batchId: String = "",
  val farmerId: String = "",
  val farmerName: String = "",
  val farmCode: String = "",
  val batchCode: String = "",
  val buyerName: String = "",
  val buyerPhone: String = "",
  val vehicleNo: String = "",
  val challanNo: String = "",
  val remarks: String = "",
  val birdsSold: Long = 0L,
  val totalWeightKg: Double = 0.0,
  val ageDays: Long = 0L,
  val avgWeightKg: Double = 0.0,
  val ratePerKg: Double = 0.0,
  val saleAmount: Double = 0.0,
  val saleEpochDay: Long = 20700L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedAvgWeightKg(): Double =
    when {
      avgWeightKg > 0.0 -> avgWeightKg
      birdsSold > 0L && totalWeightKg > 0.0 -> totalWeightKg / birdsSold.toDouble()
      else -> 0.0
    }

  fun resolvedAgeDays(placementEpochDay: Long? = null): Long =
    when {
      placementEpochDay != null -> max(0L, saleEpochDay - placementEpochDay)
      ageDays > 0L -> ageDays
      else -> 0L
    }
}

@IgnoreExtraProperties
data class FeedTransfer(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fromBatchId: String = "",
  val fromFarmerId: String = "",
  val fromFarmerName: String = "",
  val fromFarmCode: String = "",
  val fromBatchCode: String = "",
  val toBatchId: String = "",
  val toFarmerId: String = "",
  val toFarmerName: String = "",
  val toFarmCode: String = "",
  val toBatchCode: String = "",
  val feedType: String = FeedType.GROWER.value,
  val bagsCount: Long = 0L,
  val weightKg: Double = 0.0,
  val transferRef: String = "",
  val remarks: String = "",
  val transferEpochDay: Long = 20700L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class MedicineCategory(val value: String) {
  VACCINE("Vaccine"),
  ANTIBIOTIC("Antibiotic"),
  SUPPLEMENT("Supplement / Vitamin"),
  SANITIZER("Sanitizer"),
  OTHER("Other");

  companion object {
    val options: List<String> = entries.map { it.value }
  }
}

@IgnoreExtraProperties
data class MedicineEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val batchId: String = "",
  val farmerId: String = "",
  val farmerName: String = "",
  val farmCode: String = "",
  val batchCode: String = "",
  val medicineName: String = "",
  val category: String = MedicineCategory.VACCINE.value,
  val dosageOrQty: String = "",
  val totalCost: Double = 0.0,
  val invoiceOrSlipNo: String = "",
  val remarks: String = "",
  val entryEpochDay: Long = 20700L,
  val enteredByEmpCode: String = "",
  val enteredByName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class ClaimStatus(val value: String, val label: String) {
  PENDING("pending", "Pending"),
  APPROVED("approved", "Approved"),
  REJECTED("rejected", "Rejected");

  companion object {
    fun fromValue(value: String): ClaimStatus =
      entries.firstOrNull { it.value == value } ?: PENDING
  }
}

object KmMonthHelper {
  private const val MILLIS_PER_DAY = 86_400_000L

  fun monthKeyFromEpochDay(epochDay: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = epochDay * MILLIS_PER_DAY
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    return String.format(Locale.US, "%04d-%02d", year, month)
  }

  fun monthLabelFromKey(monthKey: String): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
    val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(year, month - 1, 1)
    }
    val fmt = SimpleDateFormat("MMM yyyy", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(cal.time)
  }

  fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
    val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(year, month - 1, 1)
      add(Calendar.MONTH, deltaMonths)
    }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
  }

  fun deterministicMonthlyClaimId(empCode: String, monthKey: String): String {
    val sanitizedEmp = empCode.trim().uppercase().replace(Regex("[^A-Za-z0-9_-]"), "_")
    return "kmc_${sanitizedEmp}_$monthKey"
  }
}

@IgnoreExtraProperties
data class KmDailyEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val operatorId: String = "",
  val empCode: String = "",
  val operatorName: String = "",
  val entryEpochDay: Long = 20700L,
  val monthKey: String = "",
  val startKm: Double = 0.0,
  val endKm: Double = 0.0,
  val totalKm: Double = 0.0,
  val visitedFarms: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val resolvedMonthKey: String
    get() = monthKey.ifBlank { KmMonthHelper.monthKeyFromEpochDay(entryEpochDay) }
}

@IgnoreExtraProperties
data class KmClaim(
  @DocumentId val id: String = "",
  val userId: String = "",
  val operatorId: String = "",
  val empCode: String = "",
  val operatorName: String = "",
  val claimMonthKey: String = "",
  val claimMonthLabel: String = "",
  val claimEpochDay: Long = 20700L,
  val dailyEntriesCount: Long = 1L,
  val startKm: Double = 0.0,
  val endKm: Double = 0.0,
  val totalKm: Double = 0.0,
  val ratePerKm: Double = 4.0,
  val claimAmount: Double = 0.0,
  val visitedFarms: String = "",
  val status: String = ClaimStatus.PENDING.value,
  val reviewedByEmpCode: String = "",
  val reviewedByName: String = "",
  val reviewRemarks: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val statusEnum: ClaimStatus
    get() = ClaimStatus.fromValue(status)

  val resolvedMonthKey: String
    get() = claimMonthKey.ifBlank { KmMonthHelper.monthKeyFromEpochDay(claimEpochDay) }

  val resolvedMonthLabel: String
    get() = claimMonthLabel.ifBlank { KmMonthHelper.monthLabelFromKey(resolvedMonthKey) }
}

data class GcStatementSummary(
  val chicksCount: Long,
  val freeChicksCount: Long,
  val freeChicksPercent: Double,
  val mortalityCount: Long,
  val mortalityPercent: Double,
  val birdsSold: Long,
  val shortageBirds: Long,
  val birdsWeightKg: Double,
  val feedUsedKg: Double,
  val abwKg: Double,
  val fcr: Double,
  val cfcr: Double,
  val meanAgeDays: Double,
  val eef: Double,
  val chicksCost: Double,
  val feedCost: Double,
  val adminCost: Double,
  val medicineCost: Double,
  val medicineRatePerChick: Double,
  val otherExpCost: Double,
  val totalCost: Double,
  val productionCostPerKg: Double,
  val stdProductionCostPerKg: Double,
  val stdGcPerKg: Double,
  val stdGcAmount: Double,
  val lowCostIncentive: Double,
  val saleRateIncentive: Double,
  val feedUnloadingAmount: Double,
  val miscAmount: Double,
  val cfcrWaiverAmount: Double,
  val highCostDeduction: Double,
  val birdsShortageDeduction: Double,
  val otherDeduction: Double,
  val waiverIfAny: Double,
  val totalGc: Double,
  val tdsAmount: Double,
  val totalPayable: Double,
  val finalGcPerKg: Double,
  val finalGcPerChick: Double,
  val grade: String,
)

data class ZoneDailyEntryRow(
  val serialNo: Int,
  val farmCode: String,
  val farmName: String,
  val batchCode: String,
  val strength: Long,
  val ageDays: Long,
  val dailyMort: Long,
  val dailyMortPercent: Double,
  val cumulativeMort: Long,
  val salesBirds: Long,
  val balanceBirds: Long,
  val receivedFeedBags: Long,
  val feedIntakeBags: Double,
  val totalConsumedKg: Double,
  val balanceFeedBags: Double,
  val feedGramKgPerBird: Double,
  val dayGainKg: Double,
  val bodyWeightKg: Double,
  val fcr: Double,
  val looseBirds: Long,
  val feederStock: Long,
)

data class ZoneDailyReportSummary(
  val zoneName: String,
  val region: String,
  val reportEpochDay: Long,
  val rows: List<ZoneDailyEntryRow>,
  val totalStrength: Long,
  val totalDailyMort: Long,
  val overallDailyMortPercent: Double,
  val totalCumulativeMort: Long,
  val totalSales: Long,
  val totalBalanceBirds: Long,
  val totalReceivedFeedBags: Long,
  val totalFeedIntakeBags: Double,
  val totalConsumedKg: Double,
  val totalBalanceFeedBags: Double,
  val totalLooseBirds: Long,
  val totalFeederStock: Long,
)

object ZoneReportCalculator {
  fun buildZoneDailyReport(
    zone: IntegrationZone?,
    zoneLabelFallback: String,
    regionFallback: String,
    farmers: List<Farmer>,
    batches: List<FlockBatch>,
    dailyLogs: List<DailyLog>,
    feedDispatches: List<FeedDispatch>,
    feedTransfers: List<FeedTransfer>,
    reportEpochDay: Long,
  ): ZoneDailyReportSummary {
    val activeFarmers = farmers.filter { farmer ->
      val inZone = if (zone != null) {
        farmer.zoneId == zone.id || farmer.zoneName.equals(zone.zoneName, ignoreCase = true)
      } else {
        true
      }
      inZone && (farmer.statusEnum == FarmStatus.ACTIVE || farmer.statusEnum == FarmStatus.CLOSING)
    }.sortedBy { it.farmCode }

    val batchById = batches.associateBy { it.id }
    val rows = activeFarmers.mapIndexedNotNull { idx, farmer ->
      val batch = batchById[farmer.currentBatchId]
        ?: batches.firstOrNull { it.farmerId == farmer.id && it.statusEnum != BatchStatus.CLOSED }
        ?: return@mapIndexedNotNull null

      val batchLogs = dailyLogs.filter { it.batchId == batch.id }
      val targetLog = batchLogs.firstOrNull { it.logEpochDay == reportEpochDay }
        ?: batchLogs.maxByOrNull { it.dayNumber }

      val strength = batch.totalBirdsStarted
      val ageDays = batch.currentAgeDays(reportEpochDay)
      val dailyMort = (targetLog?.mortalityCount ?: 0L) + (targetLog?.cullCount ?: 0L)
      val dailyMortPct = if (strength > 0L) (dailyMort.toDouble() / strength.toDouble()) * 100.0 else 0.0
      val cMort = batch.cumulativeMortality
      val sales = batch.liftedBirds
      val balanceBirds = batch.survivingBirds

      val dispatchedBags = feedDispatches.filter { it.batchId == batch.id }.sumOf { it.bagsCount }
      val transferredInBags = feedTransfers.filter { it.toBatchId == batch.id }.sumOf { it.bagsCount }
      val transferredOutBags = feedTransfers.filter { it.fromBatchId == batch.id }.sumOf { it.bagsCount }
      val receivedBags = max(0L, dispatchedBags + transferredInBags - transferredOutBags)

      val dailyFeedKg = targetLog?.feedConsumedKg ?: 0.0
      val feedIntakeBags = dailyFeedKg / 50.0
      val totalConsumedKg = batch.cumulativeFeedKg
      val balanceFeedBags = max(0.0, receivedBags.toDouble() - (totalConsumedKg / 50.0))
      val feedGramKg = if (balanceBirds > 0L) dailyFeedKg / balanceBirds.toDouble() else 0.0

      val bodyWeightKg = (targetLog?.avgWeightGrams?.takeIf { it > 0.0 } ?: batch.latestAvgWeightGrams) / 1000.0
      val dayGainKg = if (ageDays > 0L) bodyWeightKg / ageDays.toDouble() else 0.0
      val fcr = batch.effectiveFcr

      ZoneDailyEntryRow(
        serialNo = idx + 1,
        farmCode = farmer.farmCode,
        farmName = farmer.displayFarmName.uppercase(),
        batchCode = batch.batchCode,
        strength = strength,
        ageDays = ageDays,
        dailyMort = dailyMort,
        dailyMortPercent = dailyMortPct,
        cumulativeMort = cMort,
        salesBirds = sales,
        balanceBirds = balanceBirds,
        receivedFeedBags = receivedBags,
        feedIntakeBags = feedIntakeBags,
        totalConsumedKg = totalConsumedKg,
        balanceFeedBags = balanceFeedBags,
        feedGramKgPerBird = feedGramKg,
        dayGainKg = dayGainKg,
        bodyWeightKg = bodyWeightKg,
        fcr = fcr,
        looseBirds = targetLog?.looseBirds ?: 0L,
        feederStock = targetLog?.feederStock ?: 0L,
      )
    }

    val totalStrength = rows.sumOf { it.strength }
    val totalDailyMort = rows.sumOf { it.dailyMort }
    val overallMortPct = if (totalStrength > 0L) (totalDailyMort.toDouble() / totalStrength.toDouble()) * 100.0 else 0.0

    return ZoneDailyReportSummary(
      zoneName = zone?.zoneName?.ifBlank { zoneLabelFallback } ?: zoneLabelFallback,
      region = zone?.region?.ifBlank { regionFallback } ?: regionFallback,
      reportEpochDay = reportEpochDay,
      rows = rows,
      totalStrength = totalStrength,
      totalDailyMort = totalDailyMort,
      overallDailyMortPercent = overallMortPct,
      totalCumulativeMort = rows.sumOf { it.cumulativeMort },
      totalSales = rows.sumOf { it.salesBirds },
      totalBalanceBirds = rows.sumOf { it.balanceBirds },
      totalReceivedFeedBags = rows.sumOf { it.receivedFeedBags },
      totalFeedIntakeBags = rows.sumOf { it.feedIntakeBags },
      totalConsumedKg = rows.sumOf { it.totalConsumedKg },
      totalBalanceFeedBags = rows.sumOf { it.balanceFeedBags },
      totalLooseBirds = rows.sumOf { it.looseBirds },
      totalFeederStock = rows.sumOf { it.feederStock },
    )
  }
}

object SettlementCalculator {
  private fun round2(v: Double): Double = round(v * 100.0) / 100.0

  fun computeGcStatement(
    batch: FlockBatch,
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    details: GcStatementDetails,
    todayEpochDay: Long,
  ): GcStatementSummary {
    val chicks = batch.chicksPlaced.coerceAtLeast(0L)
    val freeChicks = batch.freeChicks.coerceAtLeast(0L)
    val totalStarted = chicks + freeChicks
    val freePct = if (chicks > 0L) (freeChicks.toDouble() / chicks.toDouble()) * 100.0 else 0.0
    val mortality = batch.cumulativeMortality.coerceAtLeast(0L)
    val mortPct = if (totalStarted > 0L) (mortality.toDouble() / totalStarted.toDouble()) * 100.0 else 0.0
    val livabilityPct = max(0.0, 100.0 - mortPct)

    val effectiveBirdsSold = if (liftedBirds > 0L) liftedBirds else max(0L, totalStarted - mortality)
    val effectiveWeightKg = if (liftedWeightKg > 0.0) liftedWeightKg else batch.liveBiomassKg
    val netFeedKg = max(0.0, batch.cumulativeFeedKg - returnedFeedKg)

    val abw = if (effectiveBirdsSold > 0L) effectiveWeightKg / effectiveBirdsSold.toDouble() else 0.0
    val fcr = if (effectiveWeightKg > 0.1) netFeedKg / effectiveWeightKg else 0.0
    val cfcr = if (fcr > 0.0 && abw > 0.0) max(0.8, fcr - ((abw - 2.0) * 0.315)) else fcr
    val mAge = if (details.meanAgeDays > 0.0) details.meanAgeDays else batch.currentAgeDays(todayEpochDay).toDouble()
    val eef = if (fcr > 0.1 && mAge > 0.0 && abw > 0.0) {
      (livabilityPct * abw * 100.0) / (mAge * fcr)
    } else 0.0

    val chicksCost = round2(chicks * details.chickRate)
    val feedCost = round2(netFeedKg * details.feedRatePerKg)
    val adminCost = round2(chicks * details.adminRatePerChick)
    val medCost = round2(details.medicineCost)
    val medRate = if (chicks > 0L) medCost / chicks.toDouble() else 0.0
    val otherCost = round2(details.otherExpCost)
    val totalCost = round2(chicksCost + feedCost + adminCost + medCost + otherCost)

    val proCost = if (effectiveWeightKg > 0.1) round2(totalCost / effectiveWeightKg) else 0.0
    val stdProCost = details.stdProductionCost
    val stdGcRate = if (details.stdGcPerKg > 0.0) details.stdGcPerKg else batch.growingRatePerKg
    val stdGcAmount = round2(effectiveWeightKg * stdGcRate)

    val grossBeforeTds = round2(
      stdGcAmount +
        details.lowCostIncentive +
        details.saleRateIncentive +
        details.feedUnloadingAmount +
        details.miscAmount +
        details.cfcrWaiverAmount +
        details.highCostDeduction +
        details.birdsShortageDeduction +
        details.otherDeduction +
        details.waiverIfAny
    )
    val totalGc = max(0.0, grossBeforeTds)
    val tdsAmount = round2(totalGc * (details.tdsPercent.coerceIn(0.0, 30.0) / 100.0))
    val totalPayable = round2(max(0.0, totalGc - tdsAmount))

    val finalGcPerKg = if (effectiveWeightKg > 0.1) round2(totalGc / effectiveWeightKg) else 0.0
    val finalGcPerChick = if (effectiveBirdsSold > 0L) round2(totalGc / effectiveBirdsSold.toDouble()) else 0.0

    val computedGrade = details.grade.ifBlank {
      when {
        proCost <= stdProCost -> "A"
        proCost <= stdProCost + 6.0 -> "B"
        proCost <= stdProCost + 12.0 -> "C"
        else -> "D"
      }
    }

    return GcStatementSummary(
      chicksCount = chicks,
      freeChicksCount = freeChicks,
      freeChicksPercent = freePct,
      mortalityCount = mortality,
      mortalityPercent = mortPct,
      birdsSold = effectiveBirdsSold,
      shortageBirds = details.shortageBirds,
      birdsWeightKg = effectiveWeightKg,
      feedUsedKg = netFeedKg,
      abwKg = abw,
      fcr = fcr,
      cfcr = cfcr,
      meanAgeDays = mAge,
      eef = eef,
      chicksCost = chicksCost,
      feedCost = feedCost,
      adminCost = adminCost,
      medicineCost = medCost,
      medicineRatePerChick = medRate,
      otherExpCost = otherCost,
      totalCost = totalCost,
      productionCostPerKg = proCost,
      stdProductionCostPerKg = stdProCost,
      stdGcPerKg = stdGcRate,
      stdGcAmount = stdGcAmount,
      lowCostIncentive = details.lowCostIncentive,
      saleRateIncentive = details.saleRateIncentive,
      feedUnloadingAmount = details.feedUnloadingAmount,
      miscAmount = details.miscAmount,
      cfcrWaiverAmount = details.cfcrWaiverAmount,
      highCostDeduction = details.highCostDeduction,
      birdsShortageDeduction = details.birdsShortageDeduction,
      otherDeduction = details.otherDeduction,
      waiverIfAny = details.waiverIfAny,
      totalGc = totalGc,
      tdsAmount = tdsAmount,
      totalPayable = totalPayable,
      finalGcPerKg = finalGcPerKg,
      finalGcPerChick = finalGcPerChick,
      grade = computedGrade,
    )
  }

  fun calculate(
    batch: FlockBatch,
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
  ): SettlementBreakdown {
    val details = batch.gcDetails ?: GcStatementDetails(stdGcPerKg = batch.growingRatePerKg)
    val summary = computeGcStatement(
      batch = batch,
      liftedBirds = liftedBirds,
      liftedWeightKg = liftedWeightKg,
      returnedFeedKg = returnedFeedKg,
      details = details,
      todayEpochDay = batch.closedEpochDay.takeIf { it > 0L } ?: (batch.placementEpochDay + batch.targetMarketAgeDays),
    )
    return SettlementBreakdown(
      totalLiveWeightKg = summary.birdsWeightKg,
      netFeedKg = summary.feedUsedKg,
      actualFcr = summary.fcr,
      standardFcr = batch.standardFcr,
      baseGrowingCharge = summary.stdGcAmount,
      fcrBonusOrPenalty = summary.lowCostIncentive + summary.saleRateIncentive + summary.cfcrWaiverAmount + summary.highCostDeduction,
      mortalityAdjustment = summary.birdsShortageDeduction + summary.otherDeduction,
      netPayable = summary.totalPayable,
    )
  }
}

data class SettlementBreakdown(
  val totalLiveWeightKg: Double,
  val netFeedKg: Double,
  val actualFcr: Double,
  val standardFcr: Double,
  val baseGrowingCharge: Double,
  val fcrBonusOrPenalty: Double,
  val mortalityAdjustment: Double,
  val netPayable: Double,
)

data class FarmLedgerData(
  val farmer: Farmer,
  val selectedBatch: FlockBatch? = null,
  val batches: List<FlockBatch>,
  val dailyLogs: List<DailyLog>,
  val feedDispatches: List<FeedDispatch>,
  val medicineEntries: List<MedicineEntry>,
  val feedTransfers: List<FeedTransfer>,
  val birdSales: List<BirdSale>,
  val totalChicksPlaced: Long,
  val totalFreeChicks: Long,
  val totalMortality: Long,
  val totalSurvivingBirds: Long,
  val totalFeedDispatchedBags: Long,
  val totalFeedDispatchedKg: Double,
  val totalFeedTransferredInBags: Long,
  val totalFeedTransferredOutBags: Long,
  val totalFeedConsumedKg: Double,
  val totalFeedReturnedKg: Double,
  val totalMedicineCost: Double,
  val totalBirdsSold: Long,
  val totalBirdsSoldWeightKg: Double,
  val totalBirdsSaleAmount: Double,
  val totalSettlementPayable: Double,
)

object FarmLedgerCalculator {
  fun build(
    farmer: Farmer,
    allBatches: List<FlockBatch>,
    dailyLogs: List<DailyLog>,
    feedDispatches: List<FeedDispatch>,
    medicineEntries: List<MedicineEntry>,
    feedTransfers: List<FeedTransfer>,
    birdSales: List<BirdSale>,
    specificBatch: FlockBatch? = null,
  ): FarmLedgerData {
    val farmBatches = if (specificBatch != null) {
      listOf(specificBatch)
    } else {
      allBatches.filter { it.farmerId == farmer.id }
        .sortedByDescending { it.placementEpochDay }
    }
    val batchIds = farmBatches.map { it.id }.toSet()

    val logs = dailyLogs.filter { it.batchId in batchIds || (specificBatch == null && it.farmerId == farmer.id) }
      .sortedWith(compareByDescending<DailyLog> { it.logEpochDay }.thenByDescending { it.dayNumber })
    val dispatches = feedDispatches.filter { it.batchId in batchIds || (specificBatch == null && it.farmerId == farmer.id) }
      .sortedByDescending { it.dispatchEpochDay }
    val medicines = medicineEntries.filter { it.batchId in batchIds || (specificBatch == null && it.farmerId == farmer.id) }
      .sortedByDescending { it.entryEpochDay }
    val transfers = feedTransfers.filter {
      it.fromBatchId in batchIds ||
        it.toBatchId in batchIds ||
        (specificBatch == null && (it.fromFarmerId == farmer.id || it.toFarmerId == farmer.id))
    }.sortedByDescending { it.transferEpochDay }
    val sales = birdSales.filter { it.batchId in batchIds || (specificBatch == null && it.farmerId == farmer.id) }
      .sortedByDescending { it.saleEpochDay }

    val inBags = transfers.filter { it.toBatchId in batchIds || (specificBatch == null && it.toFarmerId == farmer.id) }
      .sumOf { it.bagsCount }
    val outBags = transfers.filter { it.fromBatchId in batchIds || (specificBatch == null && it.fromFarmerId == farmer.id) }
      .sumOf { it.bagsCount }

    return FarmLedgerData(
      farmer = farmer,
      selectedBatch = specificBatch,
      batches = farmBatches,
      dailyLogs = logs,
      feedDispatches = dispatches,
      medicineEntries = medicines,
      feedTransfers = transfers,
      birdSales = sales,
      totalChicksPlaced = farmBatches.sumOf { it.chicksPlaced },
      totalFreeChicks = farmBatches.sumOf { it.freeChicks },
      totalMortality = farmBatches.sumOf { it.cumulativeMortality },
      totalSurvivingBirds = farmBatches.sumOf { it.survivingBirds },
      totalFeedDispatchedBags = dispatches.sumOf { it.bagsCount },
      totalFeedDispatchedKg = dispatches.sumOf { it.weightKg },
      totalFeedTransferredInBags = inBags,
      totalFeedTransferredOutBags = outBags,
      totalFeedConsumedKg = farmBatches.sumOf { it.cumulativeFeedKg },
      totalFeedReturnedKg = farmBatches.sumOf { it.returnedFeedKg },
      totalMedicineCost = medicines.sumOf { it.totalCost },
      totalBirdsSold = sales.sumOf { it.birdsSold },
      totalBirdsSoldWeightKg = sales.sumOf { it.totalWeightKg },
      totalBirdsSaleAmount = sales.sumOf { it.saleAmount },
      totalSettlementPayable = farmBatches.sumOf { it.settlementPayout },
    )
  }
}

data class KmClaimReportData(
  val claim: KmClaim,
  val dailyEntries: List<KmDailyEntry>,
  val entriesTotalKm: Double,
  val totalDaysLogged: Int,
)

object KmClaimReportCalculator {
  fun buildClaimReport(
    claim: KmClaim,
    allEntries: List<KmDailyEntry>,
  ): KmClaimReportData {
    val targetMonth = claim.resolvedMonthKey
    val matchingEntries = allEntries.filter { entry ->
      val sameStaff = if (claim.operatorId.isNotBlank() && entry.operatorId.isNotBlank()) {
        entry.operatorId == claim.operatorId
      } else {
        entry.empCode.equals(claim.empCode, ignoreCase = true)
      }
      sameStaff && entry.resolvedMonthKey == targetMonth
    }.sortedBy { it.entryEpochDay }

    return KmClaimReportData(
      claim = claim,
      dailyEntries = matchingEntries,
      entriesTotalKm = matchingEntries.sumOf { it.totalKm },
      totalDaysLogged = matchingEntries.size,
    )
  }
}

