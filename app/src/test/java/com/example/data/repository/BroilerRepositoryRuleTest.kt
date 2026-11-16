package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.catalog.MasterBatchCatalog
import com.example.data.catalog.MasterFarmerCatalog
import com.example.data.model.BatchStatus
import com.example.data.model.FarmStatus
import com.example.data.model.GcStatementDetails
import com.example.data.model.OperatorRole
import com.example.data.model.PasswordHasher
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BroilerRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun fullFarmZoneSalesTransferAndGcClosedLifecycle_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = BroilerRepository(firestore, auth)
    val suffix = UUID.randomUUID().toString().take(6)

    // 1. Zone Creation
    val zoneId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createZone(
        zoneCode = "HR-$suffix",
        zoneName = "HR ZONE",
        region = "HARYANA",
        supervisorName = "Vikram Singh",
      ).getOrThrow()
    }

    // 2. Farmer data creation linked to Zone & Bank details (starts Empty)
    val farmerId1 = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createFarmer(
        farmCode = "1309-$suffix",
        farmerName = "Meharban",
        farmName = "MEHARBAN PF BAI KHERI",
        phone = "9876543210",
        village = "Assandh",
        zoneId = zoneId,
        zoneName = "HR ZONE",
        panNumber = "GTSPM9885E",
        accountNumber = "50100432918270",
        bankName = "HDFC Bank",
        ifscCode = "HDFC0002846",
        shedType = "EC",
        shedCapacity = 10000L,
        growingRatePerKg = 9.0,
        initialStatus = FarmStatus.EMPTY,
        tehsil = "ASSANDH",
        region = "HR",
        farmType = "OWN FARM",
        aadhaarNumber = "1234-5678-9012",
        accountType = "SAVING",
        totalSqft = 10000L,
      ).getOrThrow()
    }
    val farmerId2 = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createFarmer(
        farmCode = "1310-$suffix",
        farmerName = "Ramesh",
        farmName = "RAMESH PF PAI",
        phone = "9876543211",
        village = "Pai",
        zoneId = zoneId,
        zoneName = "HR ZONE",
        shedType = "Open-Sided",
        shedCapacity = 6000L,
        growingRatePerKg = 9.0,
        initialStatus = FarmStatus.EMPTY,
      ).getOrThrow()
    }

    val farmersAfterCreate = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFarmers(uid).first { list -> list.any { it.id == farmerId1 } && list.any { it.id == farmerId2 } }
    }
    val farmer1 = farmersAfterCreate.first { it.id == farmerId1 }
    val farmer2 = farmersAfterCreate.first { it.id == farmerId2 }
    assertEquals(FarmStatus.EMPTY, farmer1.statusEnum)

    // 3. Batch creation -> Farms become Active Farm
    val batchId1 = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBatch(
        farmer = farmer1,
        batchCode = "UP0161-$suffix",
        breed = "Cobb 500",
        chicksPlaced = 4858L,
        freeChicks = 194L,
        placementEpochDay = 20700L,
        targetMarketAgeDays = 40L,
        targetAvgWeightKg = 2.2,
        standardFcr = 1.62,
        growingRatePerKg = 9.0,
        region = "HR",
        zoneName = "HR ZONE",
        farmName = "MEHARBAN PF BAI KHERI",
        weekNumber = "WEEK 43",
        challanNo = "143",
      ).getOrThrow()
    }
    val batchId2 = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBatch(
        farmer = farmer2,
        batchCode = "UP0162-$suffix",
        breed = "Cobb 500",
        chicksPlaced = 5000L,
        freeChicks = 100L,
        placementEpochDay = 20705L,
        targetMarketAgeDays = 38L,
        targetAvgWeightKg = 2.2,
        standardFcr = 1.62,
        growingRatePerKg = 9.0,
      ).getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateBatch(
        batchId = batchId2,
        batchCode = "UP0162-$suffix",
        breed = "Ross 308",
        chicksPlaced = 5000L,
        freeChicks = 100L,
        placementEpochDay = 20705L,
        targetMarketAgeDays = 38L,
        targetAvgWeightKg = 2.2,
        standardFcr = 1.62,
        growingRatePerKg = 9.0,
        region = "HR",
        zoneName = "HR ZONE",
        farmName = "RAMESH PF PAI",
        weekNumber = "WEEK 44",
        challanNo = "144",
      ).getOrThrow()
    }

    val batchesAfterCreate = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list ->
        list.any { it.id == batchId1 } && list.firstOrNull { it.id == batchId2 }?.weekNumber == "WEEK 44"
      }
    }
    val batch1 = batchesAfterCreate.first { it.id == batchId1 }
    val batch2 = batchesAfterCreate.first { it.id == batchId2 }
    assertEquals("WEEK 43", batch1.weekNumber)
    assertEquals("143", batch1.challanNo)
    assertEquals("HR", batch1.region)
    assertEquals("MEHARBAN PF BAI KHERI", batch1.displayFarmName)
    assertEquals("144", batch2.challanNo)

    // 4. Daily Log, Feed Dispatch, Birds Sales Entry & Inter-Feed Transfer
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addDailyLog(
        batch = batch1,
        dayNumber = 31L,
        logEpochDay = 20731L,
        mortalityCount = 12L,
        cullCount = 2L,
        feedConsumedKg = 17800.0,
        avgWeightGrams = 2229.0,
        shedTempCelsius = 29.0,
        looseBirds = 0L,
        feederStock = 15L,
        notes = "Healthy growth",
      ).getOrThrow()
      val dispatchId = repo.addFeedDispatch(
        batch = batch1,
        feedType = "Grower",
        bagsCount = 360L,
        weightKg = 18000.0,
        challanNo = "CH-$suffix",
        dispatchEpochDay = 20715L,
        vehicleNo = "HR-05-TR-4455",
        remarks = "Direct mill dispatch",
      ).getOrThrow()
      val savedDispatch = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeFeedDispatches(uid).first { list -> list.any { it.id == dispatchId } }
      }.first { it.id == dispatchId }
      assertEquals("1309-$suffix", savedDispatch.farmCode)
      assertEquals("MEHARBAN PF BAI KHERI", savedDispatch.farmName)
      assertEquals("HR-05-TR-4455", savedDispatch.vehicleNo)
      assertEquals("Direct mill dispatch", savedDispatch.remarks)

      repo.updateFeedDispatch(
        dispatch = savedDispatch,
        feedType = "Grower",
        bagsCount = 360L,
        weightKg = 18000.0,
        challanNo = "CH-$suffix",
        vehicleNo = "HR-05-TR-4466",
        remarks = "Verified weighbridge slip",
        dispatchEpochDay = 20715L,
      ).getOrThrow()
      val updatedDispatch = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeFeedDispatches(uid).first { list ->
          list.firstOrNull { it.id == dispatchId }?.vehicleNo == "HR-05-TR-4466"
        }.first { it.id == dispatchId }
      }
      assertEquals("Verified weighbridge slip", updatedDispatch.remarks)
    }
    val saleId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addBirdSale(
        batch = batch1,
        buyerName = "Haryana Poultry Traders",
        vehicleNo = "HR-05-AB-1020",
        challanNo = "SL-$suffix",
        birdsSold = 4400L,
        totalWeightKg = 9807.9,
        ratePerKg = 85.12,
        saleEpochDay = 20739L,
        buyerPhone = "9876543210",
        remarks = "Evening lifting",
      ).getOrThrow()
    }
    val savedSale = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBirdSales(uid).first { list -> list.any { it.id == saleId } }
    }.first { it.id == saleId }
    assertEquals(39L, savedSale.ageDays) // 20739 - 20700 placementEpochDay
    assertEquals(9807.9 / 4400.0, savedSale.avgWeightKg, 0.001)
    assertEquals("9876543210", savedSale.buyerPhone)
    assertEquals("Evening lifting", savedSale.remarks)

    val batchAfterFirstSale = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list ->
        list.firstOrNull { it.id == batchId1 }?.liftedBirds == 4400L
      }.first { it.id == batchId1 }
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateBirdSale(
        sale = savedSale,
        batch = batchAfterFirstSale,
        buyerName = "Haryana Poultry Traders",
        vehicleNo = "HR-05-AB-1020",
        challanNo = "SL-$suffix",
        birdsSold = 4400L,
        totalWeightKg = 9807.9,
        ratePerKg = 85.12,
        buyerPhone = "9812345678",
        remarks = "Cash settled on spot",
        saleEpochDay = 20739L,
      ).getOrThrow()
    }
    val updatedSale = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBirdSales(uid).first { list ->
        list.firstOrNull { it.id == saleId }?.buyerPhone == "9812345678"
      }.first { it.id == saleId }
    }
    assertEquals("Cash settled on spot", updatedSale.remarks)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addInterFeedTransfer(
        fromBatch = batch1,
        toBatch = batch2,
        feedType = "Finisher",
        bagsCount = 4L,
        weightKg = 200.0,
        transferRef = "IFT-$suffix",
        remarks = "Surplus shifted",
        transferEpochDay = 20739L,
      ).getOrThrow()
    }

    val batchAfterSales = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list ->
        list.firstOrNull { it.id == batchId1 }?.liftedBirds == 4400L
      }.first { it.id == batchId1 }
    }

    // 5. GC Closed option -> Batch Closed & Farm returns to Empty Farm
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.closeBatch(
        batch = batchAfterSales,
        liftedBirds = 4400L,
        liftedWeightKg = 9807.9,
        returnedFeedKg = 0.0,
        closingNotes = "Final GC settlement",
        closedEpochDay = 20740L,
        gcDetails = GcStatementDetails(
          companyName = "SANDHU INTEGRATION TEST",
          chickRate = 35.0,
          feedRatePerKg = 40.0,
          adminRatePerChick = 2.0,
          medicineCost = 7505.0,
          stdProductionCost = 82.0,
          stdGcPerKg = 9.0,
          saleRatePerKg = 85.12,
          feedUnloadingAmount = 356.0,
          highCostDeduction = -57023.13,
          tdsPercent = 1.0,
          grade = "C",
          meanAgeDays = 40.71,
          createdByName = "SA-001",
        ),
      ).getOrThrow()
    }
    val closedBatch = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list ->
        list.firstOrNull { it.id == batchId1 }?.statusEnum == BatchStatus.CLOSED
      }.first { it.id == batchId1 }
    }
    assertEquals(BatchStatus.CLOSED, closedBatch.statusEnum)
    assertNotNull(closedBatch.gcDetails)

    // 6. Cancel Farm List transition
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateFarmStatus(
        farmerId = farmerId1,
        newStatus = FarmStatus.CANCELLED,
        cancelReason = "Off-season shed maintenance",
      ).getOrThrow()
    }
    val cancelledFarmers = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFarmers(uid).first { list ->
        list.firstOrNull { it.id == farmerId1 }?.statusEnum == FarmStatus.CANCELLED
      }
    }
    assertEquals("Off-season shed maintenance", cancelledFarmers.first { it.id == farmerId1 }.cancelReason)
  }

  @Test
  fun observeFarmers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = BroilerRepository(firestore, auth)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeFarmers(uid).first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    val firestoreException = thrown!!.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  @Test
  fun observeFarmers_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    signInTestUser(BOB_EMAIL)
    val bobRepo = BroilerRepository(firestore, auth)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeFarmers(aliceUid).first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    val firestoreException = thrown!!.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  @Test
  fun operatorManagement_createVerifyAndUpdatePreviousDatePermission_succeedsForOwner() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = BroilerRepository(firestore, auth)
    val suffix = UUID.randomUUID().toString().take(4).uppercase()
    val empCode = "SA-$suffix"

    val opId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createOperator(
        empCode = empCode,
        fullName = "Super Admin Lead",
        role = OperatorRole.SUPER_ADMIN,
        password = "pass$suffix",
        allowPreviousDateEntry = false,
      ).getOrThrow()
    }

    val operators = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOperators(uid).first { list -> list.any { it.id == opId } }
    }
    val created = operators.first { it.id == opId }
    assertEquals(OperatorRole.SUPER_ADMIN, created.roleEnum)
    assertTrue(created.canSelectPreviousDate)
    assertTrue(PasswordHasher.verifyPassword("pass$suffix", created.passwordHash))

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateOperator(
        operatorId = opId,
        fullName = "Updated Admin",
        role = OperatorRole.ADMIN,
        isActive = true,
        newPassword = "newpass$suffix",
        allowPreviousDateEntry = true,
      ).getOrThrow()
    }
    val updated = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOperators(uid).first { list ->
        list.firstOrNull { it.id == opId }?.roleEnum == OperatorRole.ADMIN
      }.first { it.id == opId }
    }
    assertTrue(updated.allowPreviousDateEntry)
    assertTrue(updated.canSelectPreviousDate)
    assertTrue(PasswordHasher.verifyPassword("newpass$suffix", updated.passwordHash))
  }

  @Test
  fun kmEntriesAndMonthlyClaim_createDuplicateBlockEditApproveDeleteAndCrossUserBlock_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = BroilerRepository(firestore, auth)
    val suffix = UUID.randomUUID().toString().take(4).uppercase()
    val empCode = "USR-$suffix"
    val monthKey = com.example.data.model.KmMonthHelper.monthKeyFromEpochDay(20735L)

    // 1. Create daily KM entry
    val entryId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createDailyKmEntry(
        operatorId = "op_user_$suffix",
        empCode = empCode,
        operatorName = "Field Supervisor $suffix",
        entryEpochDay = 20735L,
        startKm = 15200.0,
        endKm = 15248.5,
        visitedFarms = "1309, 1310",
      ).getOrThrow()
    }

    val createdEntry = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeKmEntries(aliceUid).first { list -> list.any { it.id == entryId } }
    }.first { it.id == entryId }
    assertEquals(48.5, createdEntry.totalKm, 0.01)
    assertEquals(monthKey, createdEntry.resolvedMonthKey)

    // 2. Edit daily KM entry
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.updateDailyKmEntry(
        entry = createdEntry,
        startKm = 15200.0,
        endKm = 15260.0,
        visitedFarms = "1309, 1310, 1311",
        entryEpochDay = 20735L,
      ).getOrThrow()
    }

    // 3. Submit monthly-once KM claim (succeeds once)
    val claimId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.submitMonthlyKmClaim(
        operatorId = "op_user_$suffix",
        empCode = empCode,
        operatorName = "Field Supervisor $suffix",
        claimMonthKey = monthKey,
        claimEpochDay = 20735L,
        dailyEntriesCount = 1L,
        startKm = 15200.0,
        endKm = 15260.0,
        totalKm = 60.0,
        ratePerKm = 4.0,
        visitedFarms = "1309, 1310, 1311",
      ).getOrThrow()
    }
    assertEquals("kmc_${empCode}_$monthKey", claimId)

    // 4. Duplicate monthly claim for same empCode & monthKey must fail
    val duplicateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.submitMonthlyKmClaim(
        operatorId = "op_user_$suffix",
        empCode = empCode,
        operatorName = "Field Supervisor $suffix",
        claimMonthKey = monthKey,
        claimEpochDay = 20736L,
        dailyEntriesCount = 1L,
        startKm = 15200.0,
        endKm = 15260.0,
        totalKm = 60.0,
        ratePerKm = 4.0,
        visitedFarms = "1309",
      )
    }
    assertTrue(duplicateResult.isFailure)

    val createdClaim = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeKmClaims(aliceUid).first { list -> list.any { it.id == claimId } }
    }.first { it.id == claimId }
    assertEquals(com.example.data.model.ClaimStatus.PENDING, createdClaim.statusEnum)
    assertEquals(240.0, createdClaim.claimAmount, 0.01)

    // 5. Admin edits and approves monthly KM claim
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.updateMonthlyKmClaim(
        claim = createdClaim,
        totalKm = 60.0,
        ratePerKm = 5.0,
        visitedFarms = "1309, 1310, 1311",
      ).getOrThrow()
      aliceRepo.reviewKmClaim(
        claimId = claimId,
        status = com.example.data.model.ClaimStatus.APPROVED,
        reviewedByEmpCode = "SA-001",
        reviewedByName = "Chief Integrator",
        reviewRemarks = "Verified monthly travel summary",
      ).getOrThrow()
    }

    val approvedClaim = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeKmClaims(aliceUid).first { list ->
        list.firstOrNull { it.id == claimId }?.statusEnum == com.example.data.model.ClaimStatus.APPROVED
      }.first { it.id == claimId }
    }
    assertEquals(300.0, approvedClaim.claimAmount, 0.01)
    assertEquals("SA-001", approvedClaim.reviewedByEmpCode)

    // 6. Cross-user Bob must fail to observe Alice's kmEntries or kmClaims
    signInTestUser(BOB_EMAIL)
    val bobRepo = BroilerRepository(firestore, auth)
    var thrownEntries: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeKmEntries(aliceUid).first() }
    } catch (e: Exception) {
      thrownEntries = e
    }
    assertNotNull(thrownEntries)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrownEntries!!.unwrapFirestoreException()?.code)

    var thrownClaims: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeKmClaims(aliceUid).first() }
    } catch (e: Exception) {
      thrownClaims = e
    }
    assertNotNull(thrownClaims)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrownClaims!!.unwrapFirestoreException()?.code)

    // 7. Sign back in as Alice and delete both daily KM entry and monthly claim cleanly
    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.deleteDailyKmEntry(entryId).getOrThrow()
      aliceRepo.deleteKmClaim(claimId).getOrThrow()
    }
    val entriesAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeKmEntries(aliceUid).first { list -> list.none { it.id == entryId } }
    }
    val claimsAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeKmClaims(aliceUid).first { list -> list.none { it.id == claimId } }
    }
    assertTrue(entriesAfterDelete.none { it.id == entryId })
    assertTrue(claimsAfterDelete.none { it.id == claimId })
  }

  @Test
  fun ledgerEntries_updateAndDeleteRecomputeBatchTotals_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = BroilerRepository(firestore, auth)
    val suffix = UUID.randomUUID().toString().take(5)

    val farmerId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createFarmer(
        farmCode = "1401-$suffix",
        farmerName = "Test Grower",
        farmName = "TEST PF",
        phone = "9876500000",
        village = "Karnal",
        shedType = "EC",
        shedCapacity = 8000L,
        growingRatePerKg = 9.0,
      ).getOrThrow()
    }
    val farmer = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFarmers(uid).first { it.any { f -> f.id == farmerId } }
    }.first { it.id == farmerId }

    val batchId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBatch(
        farmer = farmer,
        batchCode = "SPF-$suffix",
        breed = "Cobb 500",
        chicksPlaced = 5000L,
        freeChicks = 100L,
        placementEpochDay = 20700L,
        targetMarketAgeDays = 40L,
        targetAvgWeightKg = 2.2,
        standardFcr = 1.62,
        growingRatePerKg = 9.0,
      ).getOrThrow()
    }
    val batch = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { it.any { b -> b.id == batchId } }
    }.first { it.id == batchId }

    val logId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addDailyLog(
        batch = batch,
        dayNumber = 10L,
        logEpochDay = 20709L,
        mortalityCount = 10L,
        cullCount = 2L,
        feedConsumedKg = 300.0,
        avgWeightGrams = 320.0,
        shedTempCelsius = 30.0,
        looseBirds = 0L,
        feederStock = 10L,
        notes = "Initial log",
      ).getOrThrow()
    }
    val savedLog = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDailyLogs(uid).first { it.any { l -> l.id == logId } }
    }.first { it.id == logId }
    val batchAfterAdd = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list -> list.firstOrNull { it.id == batchId }?.cumulativeMortality == 12L }
    }.first { it.id == batchId }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateDailyLog(
        log = savedLog,
        batch = batchAfterAdd,
        mortalityCount = 15L,
        cullCount = 3L,
        feedConsumedKg = 350.0,
        avgWeightGrams = 340.0,
        shedTempCelsius = 29.5,
        looseBirds = 5L,
        feederStock = 12L,
        notes = "Edited by admin",
      ).getOrThrow()
    }
    val batchAfterEdit = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list -> list.firstOrNull { it.id == batchId }?.cumulativeMortality == 18L }
    }.first { it.id == batchId }
    assertEquals(350.0, batchAfterEdit.cumulativeFeedKg, 0.01)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteDailyLog(
        log = savedLog.copy(mortalityCount = 15L, cullCount = 3L, feedConsumedKg = 350.0),
        batch = batchAfterEdit,
      ).getOrThrow()
    }
    val batchAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { list -> list.firstOrNull { it.id == batchId }?.cumulativeMortality == 0L }
    }.first { it.id == batchId }
    assertEquals(0.0, batchAfterDelete.cumulativeFeedKg, 0.01)
  }

  @Test
  fun medicineEntries_createUpdateDeleteAndCrossUserBlock_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = BroilerRepository(firestore, auth)
    val suffix = UUID.randomUUID().toString().take(5)

    val farmerId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createFarmer(
        farmCode = "1501-$suffix",
        farmerName = "Med Farmer",
        farmName = "MED PF",
        phone = "9876511111",
        village = "Assandh",
        shedType = "EC",
        shedCapacity = 6000L,
        growingRatePerKg = 9.0,
      ).getOrThrow()
    }
    val farmer = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFarmers(aliceUid).first { it.any { f -> f.id == farmerId } }
    }.first { it.id == farmerId }

    val batchId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBatch(
        farmer = farmer,
        batchCode = "MED-$suffix",
        breed = "Cobb 500",
        chicksPlaced = 5000L,
        freeChicks = 100L,
        placementEpochDay = 20700L,
        targetMarketAgeDays = 40L,
        targetAvgWeightKg = 2.2,
        standardFcr = 1.62,
        growingRatePerKg = 9.0,
      ).getOrThrow()
    }
    val batch = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(aliceUid).first { it.any { b -> b.id == batchId } }
    }.first { it.id == batchId }

    val entryId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addMedicineEntry(
        batch = batch,
        medicineName = "Lasota Vaccine",
        category = "Vaccine",
        dosageOrQty = "5 vials",
        totalCost = 1850.0,
        invoiceOrSlipNo = "MED-$suffix",
        remarks = "Day 7 vaccine",
        entryEpochDay = 20707L,
        enteredByEmpCode = "ADM-101",
        enteredByName = "Admin Lead",
      ).getOrThrow()
    }

    val saved = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicineEntries(aliceUid).first { list -> list.any { it.id == entryId } }
    }.first { it.id == entryId }
    assertEquals("Lasota Vaccine", saved.medicineName)
    assertEquals(1850.0, saved.totalCost, 0.01)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateMedicineEntry(
        entry = saved,
        medicineName = "Gumboro + Lasota",
        category = "Vaccine",
        dosageOrQty = "10 vials",
        totalCost = 2900.0,
        invoiceOrSlipNo = "MED-${suffix}R",
        remarks = "Updated bill",
        entryEpochDay = 20708L,
      ).getOrThrow()
    }

    val updated = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicineEntries(aliceUid).first { list ->
        list.firstOrNull { it.id == entryId }?.totalCost == 2900.0
      }.first { it.id == entryId }
    }
    assertEquals("Gumboro + Lasota", updated.medicineName)

    // Cross-user Bob must fail to observe Alice's medicineEntries
    signInTestUser(BOB_EMAIL)
    val bobRepo = BroilerRepository(firestore, auth)
    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeMedicineEntries(aliceUid).first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrown!!.unwrapFirestoreException()?.code)

    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteMedicineEntry(entryId).getOrThrow()
    }
    val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicineEntries(aliceUid).first { list -> list.none { it.id == entryId } }
    }
    assertTrue(afterDelete.none { it.id == entryId })
  }

  @Test
  fun syncMasterFarmersCatalog_importsAllRowsAndZonesIdempotently_succeeds() = runBlocking {
    val uid = signInTestUser("catalog_${UUID.randomUUID().toString().take(8)}@test.com")
    val repo = BroilerRepository(firestore, auth)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalogRows = MasterFarmerCatalog.loadFromAssets(context)
    assertTrue("Expected at least 350 parsed farmer rows, got ${catalogRows.size}", catalogRows.size >= 350)

    // Pre-create one farmer matching UPF005 with blank metadata to test non-destructive backfill
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createFarmer(
        farmCode = "UPF005",
        farmerName = "JAVED",
        farmName = "JAVED P/F",
        phone = "",
        village = "",
        shedType = "Open-Sided",
        shedCapacity = 5000L,
        growingRatePerKg = 9.0,
        totalSqft = 0L,
      ).getOrThrow()
    }

    val firstStats = withTimeout(30_000L) {
      repo.syncMasterFarmersCatalog(catalogRows, backfillExisting = true).getOrThrow()
    }
    assertEquals(8, firstStats.zonesCreated)
    assertEquals(catalogRows.size - 1, firstStats.farmersCreated)
    assertEquals(1, firstStats.farmersBackfilled)

    val zones = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeZones(uid).first { it.size >= 8 }
    }
    assertTrue(zones.any { it.zoneName == "UP ZONE" && it.region == "UP" })
    assertTrue(zones.any { it.zoneName == "UKKH ZONE" && it.region == "UKKH" })

    val farmers = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFarmers(uid).first { it.size >= catalogRows.size }
    }
    assertEquals(catalogRows.size, farmers.size)

    val javed = farmers.firstOrNull { it.farmCode == "UPF005" }
    assertNotNull(javed)
    assertEquals("TANSIPUR", javed!!.tehsil)
    assertEquals("UP", javed.region)
    assertEquals("OWN FARM", javed.farmType)
    assertEquals("3858-5767-6054", javed.aadhaarNumber)
    assertEquals("SAVING", javed.accountType)
    assertEquals(8866L, javed.totalSqft)

    val f004ZeroSqft = farmers.firstOrNull { it.farmCode == "F004" }
    assertNotNull(f004ZeroSqft)
    assertEquals(0L, f004ZeroSqft!!.totalSqft)
    assertEquals(100L, f004ZeroSqft.shedCapacity)

    // Re-running sync must be completely idempotent (0 new creates, 0 backfills)
    val secondStats = withTimeout(30_000L) {
      repo.syncMasterFarmersCatalog(catalogRows, backfillExisting = true).getOrThrow()
    }
    assertEquals(0, secondStats.zonesCreated)
    assertEquals(0, secondStats.farmersCreated)
    assertEquals(0, secondStats.farmersBackfilled)
  }

  @Test
  fun syncMasterBatchesCatalog_importsAllFlocksAndStubFarmersIdempotently_succeeds() = runBlocking {
    val uid = signInTestUser("batch_catalog_${UUID.randomUUID().toString().take(8)}@test.com")
    val repo = BroilerRepository(firestore, auth)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val farmerRows = MasterFarmerCatalog.loadFromAssets(context)
    val batchRows = MasterBatchCatalog.loadFromAssets(context)
    assertEquals("Expected all 299 parsed flock rows", 299, batchRows.size)

    withTimeout(45_000L) {
      repo.syncMasterFarmersCatalog(farmerRows, backfillExisting = true).getOrThrow()
    }
    val firstStats = withTimeout(45_000L) {
      repo.syncMasterBatchesCatalog(batchRows, backfillExisting = true).getOrThrow()
    }
    assertEquals(299, firstStats.batchesCreated)
    assertEquals(6, firstStats.stubFarmersCreated)

    val batches = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBatches(uid).first { it.size >= 299 }
    }
    assertEquals(299, batches.size)

    val farmers = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFarmers(uid).first { it.any { f -> f.farmCode == "RJ004" } }
    }
    assertTrue(farmers.any { it.farmCode == "RJ004" })

    val rj0800 = batches.firstOrNull { it.batchCode == "RJ0800" }
    assertNotNull(rj0800)
    assertEquals(0L, rj0800!!.totalBirdsStarted)

    val selfTransfer1000 = batches.firstOrNull { it.batchCode == "1000" }
    assertNotNull(selfTransfer1000)
    assertEquals(0L, selfTransfer1000!!.totalBirdsStarted)

    val batch1010 = batches.firstOrNull { it.batchCode == "1010" }
    assertNotNull(batch1010)
    assertEquals(10000L, batch1010!!.chicksPlaced)
    assertEquals(200L, batch1010.freeChicks)
    assertEquals(10200L, batch1010.totalBirdsStarted)
    assertEquals(BatchStatus.CLOSED, batch1010.statusEnum)
    assertTrue(batch1010.closedEpochDay > 0L)

    // Re-running must be completely idempotent
    val secondStats = withTimeout(45_000L) {
      repo.syncMasterBatchesCatalog(batchRows, backfillExisting = true).getOrThrow()
    }
    assertEquals(0, secondStats.zonesCreated)
    assertEquals(0, secondStats.stubFarmersCreated)
    assertEquals(0, secondStats.batchesCreated)
    assertEquals(0, secondStats.batchesBackfilled)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_broiler@test.com"
    const val BOB_EMAIL = "bob_broiler@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
