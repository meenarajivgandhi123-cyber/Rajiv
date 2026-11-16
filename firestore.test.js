const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_integrator";
const BOB_UID = "bob_integrator";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validZonePayload(uid, overrides = {}) {
  return {
    userId: uid,
    zoneCode: "HR-01",
    zoneName: "HR ZONE",
    region: "HARYANA",
    supervisorName: "Vikram",
    createdAt: ts(),
    updatedAt: ts(),
    ...overrides,
  };
}

function validFarmerPayload(uid, overrides = {}) {
  return {
    userId: uid,
    farmCode: "FRM-101",
    farmerName: "Ramesh Gowda",
    farmName: "Ramesh PF",
    phone: "+919876543210",
    village: "Hoskote North",
    tehsil: "Assandh",
    region: "HR",
    farmType: "OWN FARM",
    zoneId: "zone_hr1",
    zoneName: "HR ZONE",
    panNumber: "GTSPM9885E",
    aadhaarNumber: "3858-5767-6054",
    accountNumber: "50100432918270",
    bankName: "HDFC Bank",
    ifscCode: "HDFC0002846",
    accountType: "SAVING",
    shedType: "EC",
    shedCapacity: 10000,
    totalSqft: 8866,
    growingRatePerKg: 9.0,
    farmStatus: "empty",
    createdAt: ts(),
    updatedAt: ts(),
    ...overrides,
  };
}

function validBatchPayload(uid, farmerId, overrides = {}) {
  return {
    userId: uid,
    batchCode: "UP0161",
    farmerId,
    farmerName: "Ramesh Gowda",
    farmCode: "FRM-101",
    breed: "Cobb 500",
    chicksPlaced: 4858,
    freeChicks: 194,
    placementEpochDay: 20700,
    targetMarketAgeDays: 40,
    targetAvgWeightKg: 2.2,
    standardFcr: 1.62,
    growingRatePerKg: 9.0,
    status: "active",
    cumulativeMortality: 0,
    cumulativeFeedKg: 0,
    latestAvgWeightGrams: 42,
    liftedBirds: 0,
    liftedWeightKg: 0,
    returnedFeedKg: 0,
    settlementPayout: 0,
    closedEpochDay: 0,
    createdAt: ts(),
    updatedAt: ts(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or create farmers or zones", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("farmers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("zones").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("birdSales").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("feedTransfers").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f1").set(validFarmerPayload(ALICE_UID))
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's zones, farms, sales, and transfers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("zones").doc("zone_hr1").set(validZonePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f1").set(validFarmerPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("zones").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("farmers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("birdSales").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("feedTransfers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f2").set(validFarmerPayload(BOB_UID))
  );
});

test("Owner can create zone, farmer, place batch, GC close with gcDetails, and cancel farm", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const zoneRef = aliceDb.collection("users").doc(ALICE_UID).collection("zones").doc("zone_hr1");
  const farmerRef = aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("farmer_1");
  const batchRef = aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("batch_1");

  await assertSucceeds(zoneRef.set(validZonePayload(ALICE_UID)));
  await assertSucceeds(farmerRef.set(validFarmerPayload(ALICE_UID)));
  await assertSucceeds(batchRef.set(validBatchPayload(ALICE_UID, "farmer_1")));

  await assertSucceeds(
    farmerRef.update({
      farmStatus: "active",
      currentBatchId: "batch_1",
      currentBatchCode: "UP0161",
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    batchRef.update({
      status: "closed",
      liftedBirds: 4400,
      liftedWeightKg: 9807.9,
      returnedFeedKg: 0,
      settlementPayout: 31287.93,
      closingNotes: "GC Closed",
      closedEpochDay: 20740,
      gcDetails: {
        companyName: "BROILER INTEGRATOR FARM",
        companyAddress: "Assandh Road",
        companyPhone: "9876543210",
        companyPanGst: "GSTIN-06",
        chickRate: 35.0,
        feedRatePerKg: 40.0,
        adminRatePerChick: 2.0,
        medicineCost: 7505.0,
        otherExpCost: 0.0,
        stdProductionCost: 82.0,
        stdGcPerKg: 9.0,
        saleRatePerKg: 85.12,
        saleRateIncentive: 0.0,
        lowCostIncentive: 0.0,
        feedUnloadingAmount: 356.0,
        miscAmount: 0.0,
        cfcrWaiverAmount: 0.0,
        highCostDeduction: -57023.13,
        birdsShortageDeduction: 0.0,
        otherDeduction: 0.0,
        waiverIfAny: 0.0,
        tdsPercent: 1.0,
        shortageBirds: 0,
        shortageFeedBags: 0.0,
        grade: "C",
        meanAgeDays: 40.71,
        miscDetail: "",
        otherDeductionDetail: "",
        createdByName: "SA-001",
        approvedByName: "SA-001",
        enteredByName: "ADM-101",
        paymentByName: "Accounts",
      },
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    farmerRef.update({
      farmStatus: "empty",
      currentBatchId: "",
      currentBatchCode: "",
      updatedAt: ts(),
    })
  );

  await assertFails(
    batchRef.update({
      liftedWeightKg: 99999,
      updatedAt: ts(),
    })
  );
  await assertFails(batchRef.delete());

  await assertSucceeds(
    farmerRef.update({
      farmStatus: "cancelled",
      cancelReason: "Shed under renovation",
      updatedAt: ts(),
    })
  );
});

test("Shadow update with ghost field and orphaned batch creation are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f_ghost").set({
      ...validFarmerPayload(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("b_orphan").set(
      validBatchPayload(ALICE_UID, "non_existent_farmer")
    )
  );
});

test("Owner can log daily entry, feed dispatch, birds sales entry, and inter-feed transfer", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f1").set(validFarmerPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f2").set(validFarmerPayload(ALICE_UID, { farmCode: "FRM-102" }))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("b1").set(validBatchPayload(ALICE_UID, "f1"))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("b2").set(validBatchPayload(ALICE_UID, "f2", { batchCode: "UP0162", farmCode: "FRM-102" }))
  );

  // Hatch date Day 0 daily log must succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("log_hatch").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      dayNumber: 0,
      logEpochDay: 20700,
      mortalityCount: 1,
      cullCount: 0,
      feedConsumedKg: 40,
      avgWeightGrams: 42,
      shedTempCelsius: 32.0,
      looseBirds: 0,
      feederStock: 10,
      notes: "Hatch placement Day 0 log",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("log_1").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      dayNumber: 31,
      logEpochDay: 20731,
      mortalityCount: 6,
      cullCount: 0,
      feedConsumedKg: 1250,
      avgWeightGrams: 1600,
      shedTempCelsius: 29.5,
      looseBirds: 300,
      feederStock: 1169,
      notes: "Good feed intake",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("feedDispatches").doc("disp_1").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      batchCode: "UP0161",
      feedType: "Starter",
      bagsCount: 40,
      weightKg: 2000,
      challanNo: "CH-9021",
      dispatchEpochDay: 20701,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("birdSales").doc("sale_1").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      buyerName: "Haryana Traders",
      vehicleNo: "HR-05-AB-1234",
      challanNo: "SL-101",
      birdsSold: 450,
      totalWeightKg: 990.5,
      ageDays: 35,
      avgWeightKg: 2.201,
      ratePerKg: 86.0,
      saleAmount: 85183.0,
      saleEpochDay: 20735,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("birdSales").doc("sale_bad_age").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      buyerName: "Haryana Traders",
      challanNo: "SL-102",
      birdsSold: 100,
      totalWeightKg: 200.0,
      ageDays: -1,
      avgWeightKg: 2.0,
      ratePerKg: 86.0,
      saleAmount: 17200.0,
      saleEpochDay: 20735,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("feedTransfers").doc("trf_1").set({
      userId: ALICE_UID,
      fromBatchId: "b1",
      fromFarmerId: "f1",
      fromFarmerName: "Ramesh Gowda",
      fromFarmCode: "FRM-101",
      fromBatchCode: "UP0161",
      toBatchId: "b2",
      toFarmerId: "f2",
      toFarmerName: "Ramesh Gowda",
      toFarmCode: "FRM-102",
      toBatchCode: "UP0162",
      feedType: "Finisher",
      bagsCount: 12,
      weightKg: 600,
      transferRef: "IFT-501",
      remarks: "Surplus shifted to neighboring farm",
      transferEpochDay: 20735,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Self-transfer (same batch) must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("feedTransfers").doc("trf_self").set({
      userId: ALICE_UID,
      fromBatchId: "b1",
      fromFarmerId: "f1",
      fromFarmerName: "Ramesh Gowda",
      fromFarmCode: "FRM-101",
      fromBatchCode: "UP0161",
      toBatchId: "b1",
      toFarmerId: "f1",
      toFarmerName: "Ramesh Gowda",
      toFarmCode: "FRM-101",
      toBatchCode: "UP0161",
      feedType: "Finisher",
      bagsCount: 5,
      weightKg: 250,
      transferRef: "IFT-502",
      remarks: "",
      transferEpochDay: 20735,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Owner can manage super_admin, admin, user operators with allowPreviousDateEntry while invalid roles and cross-user reads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const opRef = aliceDb.collection("users").doc(ALICE_UID).collection("operators").doc("op_sa001");
  const userOpRef = aliceDb.collection("users").doc(ALICE_UID).collection("operators").doc("op_usr001");

  await assertSucceeds(
    opRef.set({
      userId: ALICE_UID,
      empCode: "SA-001",
      fullName: "Chief Integrator",
      role: "super_admin",
      passwordHash: "salt1234:abcdef0123456789abcdef0123456789",
      isActive: true,
      allowPreviousDateEntry: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    userOpRef.set({
      userId: ALICE_UID,
      empCode: "USR-201",
      fullName: "Field Supervisor",
      role: "user",
      passwordHash: "salt1234:abcdef0123456789abcdef0123456789",
      isActive: true,
      allowPreviousDateEntry: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Super Admin granting Previous Date Entry permission succeeds
  await assertSucceeds(
    userOpRef.update({
      fullName: "Field Supervisor",
      role: "user",
      isActive: true,
      allowPreviousDateEntry: true,
      updatedAt: ts(),
    })
  );

  // Non-boolean allowPreviousDateEntry fails schema check
  await assertFails(
    userOpRef.update({
      fullName: "Field Supervisor",
      role: "user",
      isActive: true,
      allowPreviousDateEntry: "yes",
      updatedAt: ts(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("operators").doc("op_bad").set({
      userId: ALICE_UID,
      empCode: "X-001",
      fullName: "Bad Role",
      role: "guest",
      passwordHash: "salt1234:abcdef0123456789abcdef0123456789",
      isActive: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("operators").get());
});

test("Daily KM entries and monthly-once KM claims lifecycle: create, edit, deterministic ID check, Admin approve, reject invalid, cross-user block, and delete", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("kmEntries").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("kmClaims").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const entryRef = aliceDb.collection("users").doc(ALICE_UID).collection("kmEntries").doc("kme_1");

  await assertSucceeds(
    entryRef.set({
      userId: ALICE_UID,
      operatorId: "op_usr001",
      empCode: "USR-201",
      operatorName: "Field Supervisor",
      entryEpochDay: 20735,
      monthKey: "2026-10",
      startKm: 12000,
      endKm: 12065,
      totalKm: 65,
      visitedFarms: "1309, 1310",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Invalid daily KM entry where endKm < startKm must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("kmEntries").doc("kme_bad").set({
      userId: ALICE_UID,
      operatorId: "op_usr001",
      empCode: "USR-201",
      operatorName: "Field Supervisor",
      entryEpochDay: 20735,
      monthKey: "2026-10",
      startKm: 12065,
      endKm: 12000,
      totalKm: 0,
      visitedFarms: "1309",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Admin/Super Admin edits daily KM entry
  await assertSucceeds(
    entryRef.update({
      startKm: 12000,
      endKm: 12080,
      totalKm: 80,
      visitedFarms: "1309, 1310, 1311",
      entryEpochDay: 20735,
      monthKey: "2026-10",
      updatedAt: ts(),
    })
  );

  // Monthly KM claim with non-deterministic random ID must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("kmClaims").doc("kmc_random_1").set({
      userId: ALICE_UID,
      operatorId: "op_usr001",
      empCode: "USR-201",
      operatorName: "Field Supervisor",
      claimMonthKey: "2026-10",
      claimMonthLabel: "Oct 2026",
      claimEpochDay: 20735,
      dailyEntriesCount: 1,
      startKm: 12000,
      endKm: 12080,
      totalKm: 80,
      ratePerKm: 4.5,
      claimAmount: 360.0,
      visitedFarms: "1309, 1310, 1311",
      status: "pending",
      reviewedByEmpCode: "",
      reviewedByName: "",
      reviewRemarks: "",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Monthly-once deterministic KM claim succeeds
  const claimRef = aliceDb.collection("users").doc(ALICE_UID).collection("kmClaims").doc("kmc_USR-201_2026-10");
  await assertSucceeds(
    claimRef.set({
      userId: ALICE_UID,
      operatorId: "op_usr001",
      empCode: "USR-201",
      operatorName: "Field Supervisor",
      claimMonthKey: "2026-10",
      claimMonthLabel: "Oct 2026",
      claimEpochDay: 20735,
      dailyEntriesCount: 1,
      startKm: 12000,
      endKm: 12080,
      totalKm: 80,
      ratePerKm: 4.5,
      claimAmount: 360.0,
      visitedFarms: "1309, 1310, 1311",
      status: "pending",
      reviewedByEmpCode: "",
      reviewedByName: "",
      reviewRemarks: "",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Admin/Super Admin edits monthly KM claim
  await assertSucceeds(
    claimRef.update({
      totalKm: 90,
      ratePerKm: 5.0,
      claimAmount: 450.0,
      visitedFarms: "1309, 1310, 1311, 1312",
      updatedAt: ts(),
    })
  );

  // Admin/Super Admin approves monthly claim
  await assertSucceeds(
    claimRef.update({
      status: "approved",
      reviewedByEmpCode: "SA-001",
      reviewedByName: "Chief Integrator",
      reviewRemarks: "Verified October KM log",
      updatedAt: ts(),
    })
  );

  // Invalid status fails
  await assertFails(
    claimRef.update({
      status: "paid_out",
      reviewedByEmpCode: "SA-001",
      reviewedByName: "Chief Integrator",
      reviewRemarks: "",
      updatedAt: ts(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kmEntries").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kmClaims").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kmEntries").doc("kme_1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kmClaims").doc("kmc_USR-201_2026-10").delete());

  await assertSucceeds(entryRef.delete());
  await assertSucceeds(claimRef.delete());
});

test("Medicine entries lifecycle: create, update, delete, schema validation, and cross-user/unauth rejection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("medicineEntries").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f1").set(validFarmerPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("b1").set(validBatchPayload(ALICE_UID, "f1"))
  );

  const medRef = aliceDb.collection("users").doc(ALICE_UID).collection("medicineEntries").doc("med_1");
  await assertSucceeds(
    medRef.set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      medicineName: "Lasota Vaccine",
      category: "Vaccine",
      dosageOrQty: "5000 doses",
      totalCost: 2450.0,
      invoiceOrSlipNo: "MED-1042",
      remarks: "Day 7 eye drop vaccination",
      entryEpochDay: 20707,
      enteredByEmpCode: "ADM-101",
      enteredByName: "Admin Lead",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Invalid category or negative cost must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicineEntries").doc("med_bad_cat").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      medicineName: "Unknown",
      category: "Feed",
      totalCost: 500.0,
      invoiceOrSlipNo: "MED-9999",
      entryEpochDay: 20707,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicineEntries").doc("med_neg_cost").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      medicineName: "Enrofloxacin",
      category: "Antibiotic",
      totalCost: -50.0,
      invoiceOrSlipNo: "MED-9998",
      entryEpochDay: 20707,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Orphaned batch must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicineEntries").doc("med_orphan").set({
      userId: ALICE_UID,
      batchId: "non_existent_batch",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP9999",
      medicineName: "Tonic",
      category: "Supplement / Vitamin",
      totalCost: 800.0,
      invoiceOrSlipNo: "MED-9997",
      entryEpochDay: 20707,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Update mutable fields succeeds
  await assertSucceeds(
    medRef.update({
      medicineName: "Lasota + Gumboro Vaccine",
      category: "Vaccine",
      dosageOrQty: "5000 doses x2",
      totalCost: 3100.0,
      invoiceOrSlipNo: "MED-1042R",
      remarks: "Updated dosage and bill",
      entryEpochDay: 20708,
      updatedAt: ts(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("medicineEntries").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("medicineEntries").doc("med_1").delete());

  await assertSucceeds(medRef.delete());
});

test("Farmer profile supports spreadsheet metadata (region, farmType, tehsil, totalSqft, aadhaarNumber, accountType) and rejects invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const farmerRef = aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("frm_cat_UPF005");

  await assertSucceeds(
    farmerRef.set(
      validFarmerPayload(ALICE_UID, {
        farmCode: "UPF005",
        farmerName: "JAVED",
        farmName: "JAVED P/F",
        village: "CHALAULI",
        tehsil: "TANSIPUR",
        region: "UP",
        farmType: "OWN FARM",
        zoneId: "zn_cat_UP_ZONE",
        zoneName: "UP ZONE",
        phone: "8755871361",
        totalSqft: 8866,
        shedCapacity: 8866,
        aadhaarNumber: "3858-5767-6054",
        panNumber: "BWNPJ0174N",
        accountNumber: "3360101003093",
        bankName: "CANARA BANK",
        ifscCode: "CNRB0003360",
        accountType: "SAVING",
      })
    )
  );

  // Backfill / update new profile fields succeeds
  await assertSucceeds(
    farmerRef.update({
      tehsil: "SAHARANPUR",
      region: "UP",
      farmType: "RENTED FARM",
      totalSqft: 9000,
      aadhaarNumber: "3858-5767-9999",
      accountType: "CURRENT",
      updatedAt: ts(),
    })
  );

  // Negative totalSqft must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("frm_bad_sqft").set(
      validFarmerPayload(ALICE_UID, { totalSqft: -10 })
    )
  );

  // Oversized aadhaarNumber must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("frm_bad_aadhaar").set(
      validFarmerPayload(ALICE_UID, { aadhaarNumber: "X".repeat(31) })
    )
  );
});

test("FlockBatch supports spreadsheet flock details (region, zoneName, farmName, weekNumber, challanNo) and closed import locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f_upf005").set(
      validFarmerPayload(ALICE_UID, {
        farmCode: "UPF005",
        farmerName: "JAVED",
        farmName: "JAVED P/F",
        region: "UP",
        zoneName: "UP ZONE",
      })
    )
  );

  // Active batch creation with full flock details succeeds & can update weekNumber/challanNo
  const activeBatchRef = aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("bch_active_1");
  await assertSucceeds(
    activeBatchRef.set(
      validBatchPayload(ALICE_UID, "f_upf005", {
        batchCode: "UP0170",
        farmerName: "JAVED",
        farmName: "JAVED P/F",
        farmCode: "UPF005",
        region: "UP",
        zoneName: "UP ZONE",
        weekNumber: "47.4",
        challanNo: "807",
        chicksPlaced: 6086,
        freeChicks: 122,
      })
    )
  );
  await assertSucceeds(
    activeBatchRef.update({
      weekNumber: "47.5",
      challanNo: "808",
      region: "UP",
      zoneName: "UP ZONE",
      updatedAt: ts(),
    })
  );

  // Historical closed flock import (including 0-chick self-transfer flock) succeeds
  const closedBatchRef = aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("bch_cat_1000");
  await assertSucceeds(
    closedBatchRef.set(
      validBatchPayload(ALICE_UID, "f_upf005", {
        batchCode: "1000",
        farmerName: "TASAWWUR",
        farmName: "SELF TRANSFER",
        farmCode: "UPF001",
        region: "UP",
        zoneName: "UP ZONE",
        weekNumber: "0",
        challanNo: "0",
        chicksPlaced: 0,
        freeChicks: 0,
        placementEpochDay: 20441,
        status: "closed",
        closedEpochDay: 20547,
      })
    )
  );

  // Imported closed batch cannot be modified or deleted afterward
  await assertFails(
    closedBatchRef.update({
      weekNumber: "99",
      updatedAt: ts(),
    })
  );
  await assertFails(closedBatchRef.delete());

  // Oversized weekNumber must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("bch_bad_wk").set(
      validBatchPayload(ALICE_UID, "f_upf005", { weekNumber: "W".repeat(41) })
    )
  );
});

test("FeedDispatch destination farm/vehicle/remarks and BirdSale customer phone/remarks support create, update, and reject invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farmers").doc("f1").set(validFarmerPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("batches").doc("b1").set(validBatchPayload(ALICE_UID, "f1"))
  );

  const dispRef = aliceDb.collection("users").doc(ALICE_UID).collection("feedDispatches").doc("disp_ext_1");
  await assertSucceeds(
    dispRef.set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      farmName: "Ramesh PF",
      batchCode: "UP0161",
      feedType: "Starter",
      bagsCount: 50,
      weightKg: 2500.0,
      challanNo: "FD-1001",
      vehicleNo: "HR-05-TR-8899",
      remarks: "Delivered to shed 1",
      dispatchEpochDay: 20705,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    dispRef.update({
      feedType: "Grower",
      bagsCount: 60,
      weightKg: 3000.0,
      challanNo: "FD-1001R",
      vehicleNo: "HR-05-TR-9900",
      remarks: "Revised quantity",
      dispatchEpochDay: 20706,
      updatedAt: ts(),
    })
  );

  // Oversized vehicleNo must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("feedDispatches").doc("disp_bad_veh").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      farmName: "Ramesh PF",
      batchCode: "UP0161",
      feedType: "Starter",
      bagsCount: 20,
      weightKg: 1000.0,
      challanNo: "FD-1002",
      vehicleNo: "V".repeat(41),
      dispatchEpochDay: 20705,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  const saleRef = aliceDb.collection("users").doc(ALICE_UID).collection("birdSales").doc("sale_ext_1");
  await assertSucceeds(
    saleRef.set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      buyerName: "Karnal Fresh Traders",
      buyerPhone: "9876543210",
      vehicleNo: "HR-45-AB-2211",
      challanNo: "BS-301",
      birdsSold: 500,
      totalWeightKg: 1125.0,
      ageDays: 36,
      avgWeightKg: 2.25,
      ratePerKg: 92.0,
      saleAmount: 103500.0,
      remarks: "Morning weighment",
      saleEpochDay: 20736,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    saleRef.update({
      buyerName: "Karnal Fresh Traders LLP",
      buyerPhone: "9876500011",
      vehicleNo: "HR-45-AB-2212",
      challanNo: "BS-301R",
      birdsSold: 520,
      totalWeightKg: 1170.0,
      ageDays: 37,
      avgWeightKg: 2.25,
      ratePerKg: 93.0,
      saleAmount: 108810.0,
      remarks: "Updated weighment slip",
      saleEpochDay: 20737,
      updatedAt: ts(),
    })
  );

  // Oversized buyerPhone must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("birdSales").doc("sale_bad_phone").set({
      userId: ALICE_UID,
      batchId: "b1",
      farmerId: "f1",
      farmerName: "Ramesh Gowda",
      farmCode: "FRM-101",
      batchCode: "UP0161",
      buyerName: "Test Buyer",
      buyerPhone: "9".repeat(31),
      vehicleNo: "HR-45-AB-2211",
      challanNo: "BS-302",
      birdsSold: 100,
      totalWeightKg: 220.0,
      ageDays: 36,
      avgWeightKg: 2.2,
      ratePerKg: 90.0,
      saleAmount: 19800.0,
      saleEpochDay: 20736,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});




