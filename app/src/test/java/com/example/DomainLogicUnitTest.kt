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
import com.example.data.model.MedicineCatalogItem
import com.example.data.model.MedicineCategory
import com.example.data.model.MedicineEntry
import com.example.data.model.OperatorAccount
import com.example.data.model.OperatorRole
import com.example.data.model.SettlementCalculator
import com.example.data.model.ZoneReportCalculator
import com.example.data.model.filterAndRankMedicineCatalog
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

  @Test
  fun batchCreation_farmCodeSearchRanksExactCodeFirstAndMatchesCaseInsensitively() {
    val farmers = listOf(
      Farmer(id = "1", farmCode = "F146", farmName = "SUNNY PF", zoneName = "HR ZONE", shedCapacity = 7000L),
      Farmer(id = "2", farmCode = "F046", farmName = "NIKHIL PF BRASS", zoneName = "HR ZONE", shedCapacity = 9500L),
      Farmer(id = "3", farmCode = "UPF005", farmName = "JAVED P/F", zoneName = "UP ZONE", shedCapacity = 8866L),
      Farmer(id = "4", farmCode = "UKF124", farmName = "AMAN UNIT -1PF SIMRA", zoneName = "UK ZONE", shedCapacity = 4000L),
    )

    val byPartialCode = com.example.ui.components.filterAndRankFarmersForBatchCreation(farmers, "46")
    assertEquals(listOf("F046", "F146"), byPartialCode.map { it.farmCode })

    val byExactCode = com.example.ui.components.filterAndRankFarmersForBatchCreation(farmers, " f046 ")
    assertEquals("F046", byExactCode.first().farmCode)
    assertEquals(
      "UPF005",
      com.example.ui.components.findExactFarmCodeMatch(farmers, "upf 005")?.farmCode,
    )
    assertEquals(
      null,
      com.example.ui.components.findExactFarmCodeMatch(farmers, "UPF"),
    )

    val byName = com.example.ui.components.filterAndRankFarmersForBatchCreation(farmers, "simra")
    assertEquals(listOf("UKF124"), byName.map { it.farmCode })
  }

  @Test
  fun medicineCatalogSearch_ranksExactCodeAndMatchesNameOrUnit() {
    val catalog = listOf(
      MedicineCatalogItem(id = "1", medicineCode = "M080", medicineName = "TEMPERATURE METER", unit = "PCS", rate = 30.0),
      MedicineCatalogItem(id = "2", medicineCode = "M008", medicineName = "MENTOFIN", unit = "ML", rate = 1330.0),
      MedicineCatalogItem(id = "3", medicineCode = "V001", medicineName = "MB 5000", category = MedicineCategory.VACCINE.value, unit = "PCS", rate = 1425.0),
      MedicineCatalogItem(id = "4", medicineCode = "M018", medicineName = "Gumbarok 480ml=1pcs", unit = "PCS", rate = 500.0),
    )

    val exactCode = filterAndRankMedicineCatalog(catalog, " m008 ")
    assertEquals(listOf("M008"), exactCode.map { it.medicineCode })

    val partialCode = filterAndRankMedicineCatalog(catalog, "08")
    assertEquals(listOf("M008", "M080"), partialCode.map { it.medicineCode })

    val byName = filterAndRankMedicineCatalog(catalog, "gumbarok")
    assertEquals(listOf("M018"), byName.map { it.medicineCode })

    val byCategory = filterAndRankMedicineCatalog(catalog, "vaccine")
    assertEquals(listOf("V001"), byCategory.map { it.medicineCode })
  }

  @Test
  fun feedDispatch_unitRateAndTotalAmount_computesPerBagAndPerKgCorrectly() {
    val perBagTotal = com.example.data.model.computeFeedDispatchTotalAmount(
      bagsCount = 40L,
      weightKg = 2000.0,
      unitRate = 2150.0,
      rateBasis = com.example.data.model.FeedRateBasis.PER_BAG.value,
    )
    assertEquals(86000.0, perBagTotal, 0.01)

    val perKgTotal = com.example.data.model.computeFeedDispatchTotalAmount(
      bagsCount = 40L,
      weightKg = 2000.0,
      unitRate = 43.5,
      rateBasis = com.example.data.model.FeedRateBasis.PER_KG.value,
    )
    assertEquals(87000.0, perKgTotal, 0.01)

    val fallbackResolved = FeedDispatch(
      id = "fd1",
      bagsCount = 20L,
      weightKg = 1000.0,
      unitRate = 2100.0,
      rateBasis = "bag",
      totalAmount = 0.0,
    )
    assertEquals(42000.0, fallbackResolved.resolvedTotalAmount, 0.01)
  }

  @Test
  fun medicineEntry_extractsQtyAndComputesTotalCostAutomatically() {
    assertEquals(1.0, com.example.data.model.extractMedicineQuantity("1 ML")!!, 0.001)
    assertEquals(2.5, com.example.data.model.extractMedicineQuantity(" 2.5 LTR ")!!, 0.001)
    assertEquals(250.0, com.example.data.model.extractMedicineQuantity("250ml")!!, 0.001)
    assertEquals(null, com.example.data.model.extractMedicineQuantity("vials"))

    assertEquals(3990.0, com.example.data.model.computeMedicineTotalCost("3 PCS", 1330.0)!!, 0.01)
    assertEquals(665.0, com.example.data.model.computeMedicineTotalCost("250 ML", 2.66)!!, 0.01)
    assertEquals(null, com.example.data.model.computeMedicineTotalCost("2 LTR", 0.0))
  }

  @Test
  fun operatorEmailMapping_resolvesSuperAdminEmailCaseInsensitively() {
    assertEquals("meenarajivgandhi123@gmail.com", com.example.data.model.DEFAULT_SUPER_ADMIN_EMAIL)
    assertTrue(com.example.data.model.isValidMappedEmail(""))
    assertTrue(com.example.data.model.isValidMappedEmail("Meenarajivgandhi123@gmail.com"))
    assertFalse(com.example.data.model.isValidMappedEmail("not-an-email"))

    val inactiveSuper = com.example.data.model.OperatorAccount(
      id = "op1",
      empCode = "SA-OLD",
      role = com.example.data.model.OperatorRole.SUPER_ADMIN.value,
      isActive = false,
      mappedEmail = "meenarajivgandhi123@gmail.com",
    )
    val activeUser = com.example.data.model.OperatorAccount(
      id = "op2",
      empCode = "USR-10",
      role = com.example.data.model.OperatorRole.USER.value,
      isActive = true,
      mappedEmail = "staff@spf.in",
    )
    val activeSuper = com.example.data.model.OperatorAccount(
      id = "op3",
      empCode = "SA-001",
      role = com.example.data.model.OperatorRole.SUPER_ADMIN.value,
      isActive = true,
      mappedEmail = "meenarajivgandhi123@gmail.com",
    )

    val resolved = com.example.data.model.findOperatorForEmailMapping(
      listOf(inactiveSuper, activeUser, activeSuper),
      "  Meenarajivgandhi123@gmail.com ",
    )
    assertEquals("op3", resolved?.id)
    assertEquals("op2", com.example.data.model.findOperatorForEmailMapping(listOf(activeUser), "STAFF@SPF.IN")?.id)
    assertTrue(com.example.data.model.isSuperAdminMappedEmail("MEENARAJIVGANDHI123@GMAIL.COM"))
    assertFalse(com.example.data.model.isSuperAdminMappedEmail("staff@spf.in", listOf(activeUser)))
    assertTrue(com.example.data.model.isSuperAdminMappedEmail("other_super@spf.in", listOf(activeSuper.copy(mappedEmail = "other_super@spf.in"))))
  }
}
