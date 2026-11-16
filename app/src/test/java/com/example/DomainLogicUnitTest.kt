package com.example

import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.DailyLog
import com.example.data.model.FarmLedgerCalculator
import com.example.data.model.FarmStatus
import com.example.data.model.Farmer
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedTransfer
import com.example.data.model.FlockBatch
import com.example.data.model.GcStatementDetails
import com.example.data.model.IntegrationZone
import com.example.data.model.KmClaim
import com.example.data.model.KmClaimReportCalculator
import com.example.data.model.KmDailyEntry
import com.example.data.model.KmMonthHelper
import com.example.data.model.MedicineEntry
import com.example.data.model.OperatorAccount
import com.example.data.model.OperatorRole
import com.example.data.model.SettlementCalculator
import com.example.data.model.ZoneReportCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicUnitTest {

  @Test
  fun previousDateEntryPermission_matchesRoleAndSuperAdminGrant() {
    val superAdmin = OperatorAccount(
      empCode = "SA-001",
      role = OperatorRole.SUPER_ADMIN.value,
      allowPreviousDateEntry = false,
    )
    val adminDefault = OperatorAccount(
      empCode = "ADM-101",
      role = OperatorRole.ADMIN.value,
      allowPreviousDateEntry = false,
    )
    val adminGranted = OperatorAccount(
      empCode = "ADM-102",
      role = OperatorRole.ADMIN.value,
      allowPreviousDateEntry = true,
    )
    val userDefault = OperatorAccount(
      empCode = "USR-201",
      role = OperatorRole.USER.value,
      allowPreviousDateEntry = false,
    )
    val userGranted = OperatorAccount(
      empCode = "USR-202",
      role = OperatorRole.USER.value,
      allowPreviousDateEntry = true,
    )

    assertTrue(superAdmin.canSelectPreviousDate)
    assertFalse(adminDefault.canSelectPreviousDate)
    assertTrue(adminGranted.canSelectPreviousDate)
    assertFalse(userDefault.canSelectPreviousDate)
    assertTrue(userGranted.canSelectPreviousDate)
  }

  @Test
  fun batchAge_autoCalculatesFromPlacementAndFreezesOnClose() {
    val activeBatch = FlockBatch(
      id = "b1",
      placementEpochDay = 20700L,
      closedEpochDay = 0L,
    )
    assertEquals(0L, activeBatch.currentAgeDays(20700L))
    assertEquals(0L, activeBatch.currentAgeDays(20695L))
    assertEquals(1L, activeBatch.currentAgeDays(20701L))
    assertEquals(31L, activeBatch.currentAgeDays(20731L))

    val closedBatch = activeBatch.copy(
      status = BatchStatus.CLOSED.value,
      closedEpochDay = 20739L,
    )
    assertEquals(39L, closedBatch.currentAgeDays(20800L))
  }

  @Test
  fun gcStatementCalculator_computesTotalsFcrAndTdsCorrectly() {
    val batch = FlockBatch(
      id = "b1",
      batchCode = "UP0161",
      chicksPlaced = 4858L,
      freeChicks = 194L,
      placementEpochDay = 20700L,
      cumulativeMortality = 200L,
      cumulativeFeedKg = 16000.0,
      growingRatePerKg = 9.0,
    )
    val details = GcStatementDetails(
      chickRate = 35.0,
      feedRatePerKg = 40.0,
      adminRatePerChick = 2.0,
      medicineCost = 5000.0,
      otherExpCost = 1000.0,
      stdProductionCost = 82.0,
      stdGcPerKg = 9.0,
      lowCostIncentive = 2000.0,
      tdsPercent = 1.0,
    )
    val summary = SettlementCalculator.computeGcStatement(
      batch = batch,
      liftedBirds = 4700L,
      liftedWeightKg = 10000.0,
      returnedFeedKg = 500.0,
      details = details,
      todayEpochDay = 20739L,
    )

    assertEquals(15500.0, summary.feedUsedKg, 0.01)
    assertEquals(1.55, summary.fcr, 0.01)
    assertEquals(39.0, summary.meanAgeDays, 0.01)
    // stdGcAmount = 10000 * 9.0 = 90000.0; + 2000 lowCostIncentive = 92000 totalGc; tds 1% = 920; payable = 91080
    assertEquals(90000.0, summary.stdGcAmount, 0.01)
    assertEquals(92000.0, summary.totalGc, 0.01)
    assertEquals(920.0, summary.tdsAmount, 0.01)
    assertEquals(91080.0, summary.totalPayable, 0.01)
  }

  @Test
  fun zoneDailyReportCalculator_computesZoneRowsFeedBagsAndTotals() {
    val zone = IntegrationZone(id = "z1", zoneCode = "HR-01", zoneName = "HR ZONE", region = "HARYANA")
    val farmer1 = Farmer(
      id = "f1",
      farmCode = "1309",
      farmerName = "Naresh",
      farmName = "Naresh PF",
      zoneId = "z1",
      zoneName = "HR ZONE",
      farmStatus = FarmStatus.ACTIVE.value,
      currentBatchId = "b1",
    )
    val farmerOtherZone = Farmer(
      id = "f2",
      farmCode = "2001",
      farmerName = "Other",
      farmName = "Other PF",
      zoneId = "z2",
      zoneName = "PB ZONE",
      farmStatus = FarmStatus.ACTIVE.value,
      currentBatchId = "b2",
    )
    val batch1 = FlockBatch(
      id = "b1",
      batchCode = "UP1",
      farmerId = "f1",
      chicksPlaced = 5000L,
      freeChicks = 100L,
      placementEpochDay = 20700L,
      cumulativeMortality = 50L,
      cumulativeFeedKg = 5000.0, // 100 bags consumed
      latestAvgWeightGrams = 1500.0,
    )
    val batch2 = FlockBatch(
      id = "b2",
      batchCode = "PB1",
      farmerId = "f2",
      chicksPlaced = 4000L,
      placementEpochDay = 20700L,
    )
    val log1 = DailyLog(
      id = "l1",
      batchId = "b1",
      farmerId = "f1",
      dayNumber = 26L,
      logEpochDay = 20725L,
      mortalityCount = 5L,
      cullCount = 1L,
      feedConsumedKg = 500.0,
      avgWeightGrams = 1550.0,
      looseBirds = 20L,
      feederStock = 12L,
    )
    val dispatches = listOf(
      FeedDispatch(id = "d1", batchId = "b1", bagsCount = 150L),
    )
    val transfers = listOf(
      FeedTransfer(id = "t1", fromBatchId = "b1", toBatchId = "b2", bagsCount = 20L),
    )

    val report = ZoneReportCalculator.buildZoneDailyReport(
      zone = zone,
      zoneLabelFallback = "HR ZONE",
      regionFallback = "HARYANA",
      farmers = listOf(farmer1, farmerOtherZone),
      batches = listOf(batch1, batch2),
      dailyLogs = listOf(log1),
      feedDispatches = dispatches,
      feedTransfers = transfers,
      reportEpochDay = 20725L,
    )

    assertEquals(1, report.rows.size)
    val row = report.rows.first()
    assertEquals(25L, row.ageDays)
    assertEquals(6L, row.dailyMort)
    assertEquals(130L, row.receivedFeedBags) // 150 dispatched - 20 transferred out
    assertEquals(30.0, row.balanceFeedBags, 0.01) // 130 - (5000 / 50)
    assertEquals(20L, report.totalLooseBirds)
    assertEquals(12L, report.totalFeederStock)
  }

  @Test
  fun kmMonthHelper_formatsShiftsAndBuildsDeterministicClaimIds() {
    // 20700 epochDay = 2026-09-04 UTC
    val key = KmMonthHelper.monthKeyFromEpochDay(20700L)
    assertEquals("2026-09", key)
    assertEquals("Sep 2026", KmMonthHelper.monthLabelFromKey(key))
    assertEquals("2026-08", KmMonthHelper.shiftMonthKey(key, -1))
    assertEquals("2026-10", KmMonthHelper.shiftMonthKey(key, 1))
    assertEquals("kmc_USR-201_2026-09", KmMonthHelper.deterministicMonthlyClaimId(" usr-201 ", key))
  }

  @Test
  fun farmLedgerCalculator_aggregatesBatchesFeedMedicineSalesAndTransfersCorrectly() {
    val farmer = Farmer(id = "f1", farmCode = "1309", farmerName = "Meharban", farmName = "Meharban PF")
    val b1 = FlockBatch(
      id = "b1",
      batchCode = "UP01",
      farmerId = "f1",
      chicksPlaced = 5000L,
      freeChicks = 100L,
      cumulativeMortality = 120L,
      cumulativeFeedKg = 15000.0,
      returnedFeedKg = 200.0,
      settlementPayout = 85000.0,
      status = BatchStatus.CLOSED.value,
      closedEpochDay = 20739L,
    )
    val b2 = FlockBatch(
      id = "b2",
      batchCode = "UP02",
      farmerId = "f1",
      chicksPlaced = 4000L,
      freeChicks = 80L,
      cumulativeMortality = 20L,
      cumulativeFeedKg = 1000.0,
    )
    val otherBatch = FlockBatch(id = "b3", batchCode = "PB01", farmerId = "f2", chicksPlaced = 3000L)

    val ledgerAll = FarmLedgerCalculator.build(
      farmer = farmer,
      allBatches = listOf(b1, b2, otherBatch),
      dailyLogs = listOf(DailyLog(id = "l1", batchId = "b1", farmerId = "f1")),
      feedDispatches = listOf(
        FeedDispatch(id = "d1", batchId = "b1", farmerId = "f1", bagsCount = 300L, weightKg = 15000.0),
        FeedDispatch(id = "d2", batchId = "b3", farmerId = "f2", bagsCount = 99L, weightKg = 4950.0),
      ),
      medicineEntries = listOf(
        MedicineEntry(id = "m1", batchId = "b2", farmerId = "f1", totalCost = 2500.0),
      ),
      feedTransfers = listOf(
        FeedTransfer(id = "t1", fromBatchId = "b1", fromFarmerId = "f1", toBatchId = "b3", toFarmerId = "f2", bagsCount = 10L),
        FeedTransfer(id = "t2", fromBatchId = "b3", fromFarmerId = "f2", toBatchId = "b2", toFarmerId = "f1", bagsCount = 5L),
      ),
      birdSales = listOf(
        com.example.data.model.BirdSale(id = "s1", batchId = "b1", farmerId = "f1", birdsSold = 4500L, totalWeightKg = 9900.0, saleAmount = 850000.0),
      ),
    )

    assertEquals(2, ledgerAll.batches.size)
    assertEquals(9000L, ledgerAll.totalChicksPlaced)
    assertEquals(180L, ledgerAll.totalFreeChicks)
    assertEquals(140L, ledgerAll.totalMortality)
    assertEquals(300L, ledgerAll.totalFeedDispatchedBags)
    assertEquals(5L, ledgerAll.totalFeedTransferredInBags)
    assertEquals(10L, ledgerAll.totalFeedTransferredOutBags)
    assertEquals(2500.0, ledgerAll.totalMedicineCost, 0.01)
    assertEquals(4500L, ledgerAll.totalBirdsSold)
    assertEquals(85000.0, ledgerAll.totalSettlementPayable, 0.01)

    val ledgerSingleBatch = FarmLedgerCalculator.build(
      farmer = farmer,
      allBatches = listOf(b1, b2, otherBatch),
      dailyLogs = listOf(DailyLog(id = "l1", batchId = "b1", farmerId = "f1")),
      feedDispatches = listOf(FeedDispatch(id = "d1", batchId = "b1", farmerId = "f1", bagsCount = 300L, weightKg = 15000.0)),
      medicineEntries = listOf(MedicineEntry(id = "m1", batchId = "b2", farmerId = "f1", totalCost = 2500.0)),
      feedTransfers = emptyList(),
      birdSales = emptyList(),
      specificBatch = b2,
    )
    assertEquals(1, ledgerSingleBatch.batches.size)
    assertEquals(4000L, ledgerSingleBatch.totalChicksPlaced)
    assertEquals(0, ledgerSingleBatch.dailyLogs.size)
    assertEquals(2500.0, ledgerSingleBatch.totalMedicineCost, 0.01)
  }

  @Test
  fun birdSale_resolvesAutoAgeFromHatchAndAutoAvgWeightFromTotalWeight() {
    val sale = BirdSale(
      id = "s1",
      batchId = "b1",
      birdsSold = 1000L,
      totalWeightKg = 2150.0,
      saleEpochDay = 20735L,
    )
    assertEquals(35L, sale.resolvedAgeDays(placementEpochDay = 20700L))
    assertEquals(0L, sale.resolvedAgeDays(placementEpochDay = 20735L)) // Hatch day = Age 0
    assertEquals(2.15, sale.resolvedAvgWeightKg(), 0.001)

    val storedSale = sale.copy(ageDays = 36L, avgWeightKg = 2.18)
    assertEquals(35L, storedSale.resolvedAgeDays(placementEpochDay = 20700L))
    assertEquals(36L, storedSale.resolvedAgeDays(placementEpochDay = null))
    assertEquals(2.18, storedSale.resolvedAvgWeightKg(), 0.001)
  }

  @Test
  fun kmClaimReportCalculator_linksMatchingEmployeeMonthlyEntriesInChronologicalOrder() {
    val claim = KmClaim(
      id = "kmc_USR-201_2026-09",
      operatorId = "op_1",
      empCode = "USR-201",
      operatorName = "Raju",
      claimMonthKey = "2026-09",
      claimMonthLabel = "Sep 2026",
      totalKm = 105.0,
      ratePerKm = 4.0,
      claimAmount = 420.0,
    )
    val entries = listOf(
      KmDailyEntry(id = "e2", operatorId = "op_1", empCode = "USR-201", entryEpochDay = 20705L, monthKey = "2026-09", startKm = 1050.0, endKm = 1110.0, totalKm = 60.0),
      KmDailyEntry(id = "e1", operatorId = "op_1", empCode = "USR-201", entryEpochDay = 20702L, monthKey = "2026-09", startKm = 1000.0, endKm = 1045.0, totalKm = 45.0),
      KmDailyEntry(id = "eOtherMonth", operatorId = "op_1", empCode = "USR-201", entryEpochDay = 20730L, monthKey = "2026-10", totalKm = 99.0),
      KmDailyEntry(id = "eOtherStaff", operatorId = "op_2", empCode = "USR-202", entryEpochDay = 20703L, monthKey = "2026-09", totalKm = 70.0),
    )

    val report = KmClaimReportCalculator.buildClaimReport(claim, entries)
    assertEquals(2, report.totalDaysLogged)
    assertEquals(105.0, report.entriesTotalKm, 0.01)
    assertEquals(listOf("e1", "e2"), report.dailyEntries.map { it.id })
  }
}
