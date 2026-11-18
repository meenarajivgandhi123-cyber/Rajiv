package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.catalog.CatalogBatchRow
import com.example.data.catalog.CatalogFarmerRow
import com.example.data.catalog.CatalogMedicineRow
import com.example.data.catalog.MasterFarmerCatalog
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.ClaimStatus
import com.example.data.model.DailyLog
import com.example.data.model.FarmStatus
import com.example.data.model.Farmer
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedRateBasis
import com.example.data.model.FeedTransfer
import com.example.data.model.FlockBatch
import com.example.data.model.GcStatementDetails
import com.example.data.model.IntegrationZone
import com.example.data.model.KmClaim
import com.example.data.model.KmDailyEntry
import com.example.data.model.KmMonthHelper
import com.example.data.model.MedicineCatalogItem
import com.example.data.model.MedicineEntry
import com.example.data.model.OperatorAccount
import com.example.data.model.OperatorRole
import com.example.data.model.PasswordHasher
import com.example.data.model.SettlementCalculator
import com.example.data.model.computeFeedDispatchTotalAmount
import com.example.data.model.isValidMappedEmail
import com.example.data.model.normalizeMappedEmail
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class SyncCatalogStats(
  val zonesCreated: Int,
  val farmersCreated: Int,
  val farmersBackfilled: Int,
)

data class SyncBatchCatalogStats(
  val zonesCreated: Int,
  val stubFarmersCreated: Int,
  val batchesCreated: Int,
  val batchesBackfilled: Int,
)

data class SyncMedicineCatalogStats(
  val itemsCreated: Int,
  val itemsBackfilled: Int,
)

class BroilerRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun zonesCollection(uid: String) =
    db.collection("users").document(uid).collection("zones")

  private fun operatorsCollection(uid: String) =
    db.collection("users").document(uid).collection("operators")

  private fun farmersCollection(uid: String) =
    db.collection("users").document(uid).collection("farmers")

  private fun batchesCollection(uid: String) =
    db.collection("users").document(uid).collection("batches")

  private fun dailyLogsCollection(uid: String) =
    db.collection("users").document(uid).collection("dailyLogs")

  private fun feedDispatchesCollection(uid: String) =
    db.collection("users").document(uid).collection("feedDispatches")

  private fun birdSalesCollection(uid: String) =
    db.collection("users").document(uid).collection("birdSales")

  private fun feedTransfersCollection(uid: String) =
    db.collection("users").document(uid).collection("feedTransfers")

  private fun kmEntriesCollection(uid: String) =
    db.collection("users").document(uid).collection("kmEntries")

  private fun kmClaimsCollection(uid: String) =
    db.collection("users").document(uid).collection("kmClaims")

  private fun medicineEntriesCollection(uid: String) =
    db.collection("users").document(uid).collection("medicineEntries")

  private fun medicineCatalogCollection(uid: String) =
    db.collection("users").document(uid).collection("medicinesCatalog")

  fun observeZones(userId: String): Flow<List<IntegrationZone>> = flow {
    val path = "users/$userId/zones"
    emitAll(
      zonesCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(IntegrationZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.zoneName }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createZone(
    zoneCode: String,
    zoneName: String,
    region: String,
    supervisorName: String,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val zoneId = "zn_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = zonesCollection(uid).document(zoneId)
      val payload = mapOf(
        "userId" to uid,
        "zoneCode" to zoneCode.trim().uppercase(),
        "zoneName" to zoneName.trim().uppercase(),
        "region" to region.trim().uppercase(),
        "supervisorName" to supervisorName.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        zoneId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateZone(
    zoneId: String,
    zoneCode: String,
    zoneName: String,
    region: String,
    supervisorName: String,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = zonesCollection(uid).document(zoneId)
      val updates = mapOf(
        "zoneCode" to zoneCode.trim().uppercase(),
        "zoneName" to zoneName.trim().uppercase(),
        "region" to region.trim().uppercase(),
        "supervisorName" to supervisorName.trim(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.update(updates).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteZone(zoneId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = zonesCollection(uid).document(zoneId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  fun observeOperators(userId: String): Flow<List<OperatorAccount>> = flow {
    val path = "users/$userId/operators"
    emitAll(
      operatorsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(OperatorAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<OperatorAccount> { it.roleEnum.ordinal }.thenBy { it.empCode })
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createOperator(
    empCode: String,
    fullName: String,
    role: OperatorRole,
    password: String,
    allowPreviousDateEntry: Boolean = false,
    mappedEmail: String = "",
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val normalizedCode = empCode.trim().uppercase()
      val cleanEmail = normalizeMappedEmail(mappedEmail)
      require(normalizedCode.length >= 2) { "Employee code must be at least 2 characters" }
      require(fullName.isNotBlank()) { "Full name is required" }
      require(password.length >= 4) { "Password must be at least 4 characters" }
      require(isValidMappedEmail(cleanEmail)) { "Enter a valid Email ID for mapping" }

      val existing = operatorsCollection(uid)
        .whereEqualTo("empCode", normalizedCode)
        .limit(1)
        .get()
        .await()
      require(existing.isEmpty) { "Employee code $normalizedCode already exists" }

      val operatorId = "op_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = operatorsCollection(uid).document(operatorId)
      val payload = mapOf(
        "userId" to uid,
        "empCode" to normalizedCode,
        "fullName" to fullName.trim(),
        "role" to role.value,
        "passwordHash" to PasswordHasher.hashPassword(password),
        "isActive" to true,
        "allowPreviousDateEntry" to (role == OperatorRole.SUPER_ADMIN || allowPreviousDateEntry),
        "mappedEmail" to cleanEmail.take(120),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        operatorId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateOperator(
    operatorId: String,
    fullName: String,
    role: OperatorRole,
    isActive: Boolean,
    newPassword: String? = null,
    allowPreviousDateEntry: Boolean = false,
    mappedEmail: String? = null,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = operatorsCollection(uid).document(operatorId)
      val updates = mutableMapOf<String, Any>(
        "fullName" to fullName.trim(),
        "role" to role.value,
        "isActive" to isActive,
        "allowPreviousDateEntry" to (role == OperatorRole.SUPER_ADMIN || allowPreviousDateEntry),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (mappedEmail != null) {
        val cleanEmail = normalizeMappedEmail(mappedEmail)
        require(isValidMappedEmail(cleanEmail)) { "Enter a valid Email ID for mapping" }
        updates["mappedEmail"] = cleanEmail.take(120)
      }
      if (!newPassword.isNullOrBlank()) {
        require(newPassword.length >= 4) { "Password must be at least 4 characters" }
        updates["passwordHash"] = PasswordHasher.hashPassword(newPassword)
      }
      try {
        docRef.update(updates).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteOperator(operatorId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = operatorsCollection(uid).document(operatorId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  fun observeFarmers(userId: String): Flow<List<Farmer>> = flow {
    val path = "users/$userId/farmers"
    emitAll(
      farmersCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Farmer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.farmCode }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeBatches(userId: String): Flow<List<FlockBatch>> = flow {
    val path = "users/$userId/batches"
    emitAll(
      batchesCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(FlockBatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.placementEpochDay }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeDailyLogs(userId: String): Flow<List<DailyLog>> = flow {
    val path = "users/$userId/dailyLogs"
    emitAll(
      dailyLogsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.logEpochDay * 100 + it.dayNumber }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeFeedDispatches(userId: String): Flow<List<FeedDispatch>> = flow {
    val path = "users/$userId/feedDispatches"
    emitAll(
      feedDispatchesCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(FeedDispatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.dispatchEpochDay }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeBirdSales(userId: String): Flow<List<BirdSale>> = flow {
    val path = "users/$userId/birdSales"
    emitAll(
      birdSalesCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(BirdSale::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.saleEpochDay }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeFeedTransfers(userId: String): Flow<List<FeedTransfer>> = flow {
    val path = "users/$userId/feedTransfers"
    emitAll(
      feedTransfersCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(FeedTransfer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.transferEpochDay }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeKmEntries(userId: String): Flow<List<KmDailyEntry>> = flow {
    val path = "users/$userId/kmEntries"
    emitAll(
      kmEntriesCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(KmDailyEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.entryEpochDay }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeKmClaims(userId: String): Flow<List<KmClaim>> = flow {
    val path = "users/$userId/kmClaims"
    emitAll(
      kmClaimsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(KmClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareByDescending<KmClaim> { it.resolvedMonthKey }.thenByDescending { it.claimEpochDay })
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeMedicineEntries(userId: String): Flow<List<MedicineEntry>> = flow {
    val path = "users/$userId/medicineEntries"
    emitAll(
      medicineEntriesCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(MedicineEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.entryEpochDay }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeMedicineCatalog(userId: String): Flow<List<MedicineCatalogItem>> = flow {
    val path = "users/$userId/medicinesCatalog"
    emitAll(
      medicineCatalogCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(MedicineCatalogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.medicineCode.uppercase() }
        }
        .catch { error ->
          logStreamErrorIfAuthenticated(userId, error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createFarmer(
    farmCode: String,
    farmerName: String,
    farmName: String = "",
    phone: String,
    village: String,
    zoneId: String = "",
    zoneName: String = "",
    panNumber: String = "",
    accountNumber: String = "",
    bankName: String = "",
    ifscCode: String = "",
    shedType: String,
    shedCapacity: Long,
    growingRatePerKg: Double,
    initialStatus: FarmStatus = FarmStatus.EMPTY,
    tehsil: String = "",
    region: String = "",
    farmType: String = "",
    aadhaarNumber: String = "",
    accountType: String = "",
    totalSqft: Long = shedCapacity,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val farmerId = "frm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = farmersCollection(uid).document(farmerId)
      val payload = mapOf(
        "userId" to uid,
        "farmCode" to farmCode.trim().take(30),
        "farmerName" to farmerName.trim().take(80),
        "farmName" to farmName.trim().ifBlank { farmerName.trim() }.take(100),
        "phone" to phone.trim().take(30),
        "village" to village.trim().take(100),
        "tehsil" to tehsil.trim().take(100),
        "region" to region.trim().uppercase().take(40),
        "farmType" to farmType.trim().uppercase().take(40),
        "zoneId" to zoneId.trim().take(128),
        "zoneName" to zoneName.trim().take(80),
        "panNumber" to panNumber.trim().uppercase().take(30),
        "aadhaarNumber" to aadhaarNumber.trim().take(30),
        "accountNumber" to accountNumber.trim().take(40),
        "bankName" to bankName.trim().take(80),
        "ifscCode" to ifscCode.trim().uppercase().take(30),
        "accountType" to accountType.trim().uppercase().take(30),
        "shedType" to shedType,
        "shedCapacity" to shedCapacity.coerceIn(100L, 500000L),
        "totalSqft" to totalSqft.coerceIn(0L, 500000L),
        "growingRatePerKg" to growingRatePerKg,
        "farmStatus" to initialStatus.value,
        "currentBatchId" to "",
        "currentBatchCode" to "",
        "cancelReason" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        farmerId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateFarmerProfile(
    farmerId: String,
    farmCode: String,
    farmerName: String,
    farmName: String = "",
    phone: String,
    village: String,
    zoneId: String = "",
    zoneName: String = "",
    panNumber: String = "",
    accountNumber: String = "",
    bankName: String = "",
    ifscCode: String = "",
    shedType: String,
    shedCapacity: Long,
    growingRatePerKg: Double,
    tehsil: String = "",
    region: String = "",
    farmType: String = "",
    aadhaarNumber: String = "",
    accountType: String = "",
    totalSqft: Long = shedCapacity,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = farmersCollection(uid).document(farmerId)
      val updates = mapOf(
        "farmCode" to farmCode.trim().take(30),
        "farmerName" to farmerName.trim().take(80),
        "farmName" to farmName.trim().ifBlank { farmerName.trim() }.take(100),
        "phone" to phone.trim().take(30),
        "village" to village.trim().take(100),
        "tehsil" to tehsil.trim().take(100),
        "region" to region.trim().uppercase().take(40),
        "farmType" to farmType.trim().uppercase().take(40),
        "zoneId" to zoneId.trim().take(128),
        "zoneName" to zoneName.trim().take(80),
        "panNumber" to panNumber.trim().uppercase().take(30),
        "aadhaarNumber" to aadhaarNumber.trim().take(30),
        "accountNumber" to accountNumber.trim().take(40),
        "bankName" to bankName.trim().take(80),
        "ifscCode" to ifscCode.trim().uppercase().take(30),
        "accountType" to accountType.trim().uppercase().take(30),
        "shedType" to shedType,
        "shedCapacity" to shedCapacity.coerceIn(100L, 500000L),
        "totalSqft" to totalSqft.coerceIn(0L, 500000L),
        "growingRatePerKg" to growingRatePerKg,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.update(updates).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun syncMasterFarmersCatalog(
    rows: List<CatalogFarmerRow>,
    backfillExisting: Boolean = true,
  ): Result<SyncCatalogStats> {
    return runCatching {
      val uid = requireUserId()
      if (rows.isEmpty()) return@runCatching SyncCatalogStats(0, 0, 0)

      val existingZones = zonesCollection(uid).get().await()
        .toObjects(IntegrationZone::class.java)
      val existingFarmers = farmersCollection(uid).get().await()
        .toObjects(Farmer::class.java)

      val zoneById = existingZones.associateBy { it.id }
      val zoneByNameUpper = existingZones.associateBy { it.zoneName.trim().uppercase() }
      val resolvedZoneIdByName = mutableMapOf<String, String>()

      val pendingWrites = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
      var zonesCreated = 0
      var farmersCreated = 0
      var farmersBackfilled = 0

      val catalogZones = MasterFarmerCatalog.extractDistinctZones(rows)
      catalogZones.forEach { spec ->
        val key = spec.zoneName.trim().uppercase()
        val matched = zoneByNameUpper[key] ?: zoneById[spec.docId]
        if (matched != null) {
          resolvedZoneIdByName[key] = matched.id
        } else {
          resolvedZoneIdByName[key] = spec.docId
          val zoneRef = zonesCollection(uid).document(spec.docId)
          val zonePayload = mapOf(
            "userId" to uid,
            "zoneCode" to spec.zoneCode.take(30),
            "zoneName" to spec.zoneName.take(80),
            "region" to spec.region.take(80),
            "supervisorName" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          pendingWrites += { batch -> batch.set(zoneRef, zonePayload) }
          zonesCreated += 1
        }
      }

      val farmerById = existingFarmers.associateBy { it.id }
      val farmerByCodeAndName = existingFarmers.associateBy {
        "${it.farmCode.trim().uppercase()}|${it.displayFarmName.trim().uppercase()}"
      }
      val farmersByCode = existingFarmers.groupBy { it.farmCode.trim().uppercase() }
      val catalogCodeCounts = rows.groupingBy { it.farmCode.trim().uppercase() }.eachCount()

      rows.forEach { row ->
        val zoneKey = row.zoneName.trim().uppercase()
        val resolvedZoneId = resolvedZoneIdByName[zoneKey] ?: row.zoneDocId
        val pairKey = "${row.farmCode.trim().uppercase()}|${row.farmName.trim().uppercase()}"

        val existingMatch = farmerById[row.docId]
          ?: farmerByCodeAndName[pairKey]
          ?: run {
            val singleCodeCandidates = farmersByCode[row.farmCode.trim().uppercase()].orEmpty()
            if (catalogCodeCounts[row.farmCode.trim().uppercase()] == 1 && singleCodeCandidates.size == 1) {
              val candidate = singleCodeCandidates.first()
              if (candidate.farmerName.equals(row.farmerName, ignoreCase = true) || candidate.region.isBlank()) {
                candidate
              } else null
            } else null
          }

        if (existingMatch == null) {
          val farmerRef = farmersCollection(uid).document(row.docId)
          val farmerPayload = mapOf(
            "userId" to uid,
            "farmCode" to row.farmCode,
            "farmerName" to row.farmerName,
            "farmName" to row.farmName,
            "phone" to row.phone,
            "village" to row.village,
            "tehsil" to row.tehsil,
            "region" to row.region,
            "farmType" to row.farmType,
            "zoneId" to resolvedZoneId,
            "zoneName" to row.zoneName,
            "panNumber" to row.panNumber,
            "aadhaarNumber" to row.aadhaarNumber,
            "accountNumber" to row.accountNumber,
            "bankName" to row.bankName,
            "ifscCode" to row.ifscCode,
            "accountType" to row.accountType,
            "shedType" to "Open-Sided",
            "shedCapacity" to row.shedCapacity,
            "totalSqft" to row.totalSqft,
            "growingRatePerKg" to 9.0,
            "farmStatus" to FarmStatus.EMPTY.value,
            "currentBatchId" to "",
            "currentBatchCode" to "",
            "cancelReason" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          pendingWrites += { batch -> batch.set(farmerRef, farmerPayload) }
          farmersCreated += 1
        } else if (backfillExisting) {
          val updates = mutableMapOf<String, Any>()
          if (existingMatch.farmName.isBlank() && row.farmName.isNotBlank()) updates["farmName"] = row.farmName
          if (existingMatch.phone.isBlank() && row.phone.isNotBlank()) updates["phone"] = row.phone
          if (existingMatch.village.isBlank() && row.village.isNotBlank()) updates["village"] = row.village
          if (existingMatch.tehsil.isBlank() && row.tehsil.isNotBlank()) updates["tehsil"] = row.tehsil
          if (existingMatch.region.isBlank() && row.region.isNotBlank()) updates["region"] = row.region
          if (existingMatch.farmType.isBlank() && row.farmType.isNotBlank()) updates["farmType"] = row.farmType
          if (existingMatch.zoneId.isBlank() && resolvedZoneId.isNotBlank()) updates["zoneId"] = resolvedZoneId
          if (existingMatch.zoneName.isBlank() && row.zoneName.isNotBlank()) updates["zoneName"] = row.zoneName
          if (existingMatch.panNumber.isBlank() && row.panNumber.isNotBlank()) updates["panNumber"] = row.panNumber
          if (existingMatch.aadhaarNumber.isBlank() && row.aadhaarNumber.isNotBlank()) updates["aadhaarNumber"] = row.aadhaarNumber
          if (existingMatch.accountNumber.isBlank() && row.accountNumber.isNotBlank()) updates["accountNumber"] = row.accountNumber
          if (existingMatch.bankName.isBlank() && row.bankName.isNotBlank()) updates["bankName"] = row.bankName
          if (existingMatch.ifscCode.isBlank() && row.ifscCode.isNotBlank()) updates["ifscCode"] = row.ifscCode
          if (existingMatch.accountType.isBlank() && row.accountType.isNotBlank()) updates["accountType"] = row.accountType
          if (existingMatch.totalSqft == 0L && row.totalSqft > 0L) updates["totalSqft"] = row.totalSqft
          if (updates.isNotEmpty()) {
            updates["updatedAt"] = FieldValue.serverTimestamp()
            val farmerRef = farmersCollection(uid).document(existingMatch.id)
            pendingWrites += { batch -> batch.update(farmerRef, updates) }
            farmersBackfilled += 1
          }
        }
      }

      try {
        pendingWrites.chunked(400).forEach { chunk ->
          val batch = db.batch()
          chunk.forEach { op -> op(batch) }
          batch.commit().await()
        }
        SyncCatalogStats(
          zonesCreated = zonesCreated,
          farmersCreated = farmersCreated,
          farmersBackfilled = farmersBackfilled,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid/farmers")
        throw e
      }
    }
  }

  suspend fun updateFarmStatus(
    farmerId: String,
    newStatus: FarmStatus,
    cancelReason: String = "",
    clearBatchLink: Boolean = newStatus == FarmStatus.EMPTY || newStatus == FarmStatus.CANCELLED,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = farmersCollection(uid).document(farmerId)
      val updates = mutableMapOf<String, Any>(
        "farmStatus" to newStatus.value,
        "cancelReason" to if (newStatus == FarmStatus.CANCELLED) cancelReason.trim() else "",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (clearBatchLink) {
        updates["currentBatchId"] = ""
        updates["currentBatchCode"] = ""
      }
      try {
        docRef.update(updates).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun createBatch(
    farmer: Farmer,
    batchCode: String,
    breed: String,
    chicksPlaced: Long,
    freeChicks: Long,
    placementEpochDay: Long,
    targetMarketAgeDays: Long,
    targetAvgWeightKg: Double,
    standardFcr: Double,
    growingRatePerKg: Double,
    region: String = farmer.region,
    zoneName: String = farmer.zoneName,
    farmName: String = farmer.displayFarmName,
    weekNumber: String = "",
    challanNo: String = "",
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val batchId = "bch_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val batchRef = batchesCollection(uid).document(batchId)
      val farmerRef = farmersCollection(uid).document(farmer.id)

      val batchPayload = mapOf(
        "userId" to uid,
        "batchCode" to batchCode.trim().uppercase().take(40),
        "farmerId" to farmer.id,
        "farmerName" to farmer.farmerName.take(80),
        "farmName" to farmName.trim().ifBlank { farmer.displayFarmName }.take(100),
        "farmCode" to farmer.farmCode.take(30),
        "region" to region.trim().uppercase().take(40),
        "zoneName" to zoneName.trim().uppercase().take(80),
        "weekNumber" to weekNumber.trim().take(40),
        "challanNo" to challanNo.trim().take(40),
        "breed" to breed.trim().take(50),
        "chicksPlaced" to chicksPlaced,
        "freeChicks" to freeChicks,
        "placementEpochDay" to placementEpochDay,
        "targetMarketAgeDays" to targetMarketAgeDays,
        "targetAvgWeightKg" to targetAvgWeightKg,
        "standardFcr" to standardFcr,
        "growingRatePerKg" to growingRatePerKg,
        "status" to BatchStatus.ACTIVE.value,
        "cumulativeMortality" to 0L,
        "cumulativeFeedKg" to 0.0,
        "latestAvgWeightGrams" to 42.0,
        "liftedBirds" to 0L,
        "liftedWeightKg" to 0.0,
        "returnedFeedKg" to 0.0,
        "settlementPayout" to 0.0,
        "closingNotes" to "",
        "closedEpochDay" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

      try {
        batchRef.set(batchPayload).await()
        farmerRef.update(
          mapOf(
            "farmStatus" to FarmStatus.ACTIVE.value,
            "currentBatchId" to batchId,
            "currentBatchCode" to batchCode.trim(),
            "cancelReason" to "",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        batchId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, batchRef.path)
        throw e
      }
    }
  }

  suspend fun addDailyLog(
    batch: FlockBatch,
    dayNumber: Long,
    logEpochDay: Long,
    mortalityCount: Long,
    cullCount: Long,
    feedConsumedKg: Double,
    avgWeightGrams: Double,
    shedTempCelsius: Double,
    looseBirds: Long = 0L,
    feederStock: Long = 0L,
    notes: String,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Cannot add logs to a GC Closed batch" }
      val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val logRef = dailyLogsCollection(uid).document(logId)
      val batchRef = batchesCollection(uid).document(batch.id)

      val logPayload = mapOf(
        "userId" to uid,
        "batchId" to batch.id,
        "farmerId" to batch.farmerId,
        "dayNumber" to dayNumber,
        "logEpochDay" to logEpochDay,
        "mortalityCount" to mortalityCount,
        "cullCount" to cullCount,
        "feedConsumedKg" to feedConsumedKg,
        "avgWeightGrams" to avgWeightGrams,
        "shedTempCelsius" to shedTempCelsius,
        "looseBirds" to looseBirds,
        "feederStock" to feederStock,
        "notes" to notes.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

      val newMortality = batch.cumulativeMortality + mortalityCount + cullCount
      val newFeedKg = batch.cumulativeFeedKg + feedConsumedKg
      val newWeight = if (avgWeightGrams > 0.0) avgWeightGrams else batch.latestAvgWeightGrams

      try {
        logRef.set(logPayload).await()
        batchRef.update(
          mapOf(
            "cumulativeMortality" to newMortality,
            "cumulativeFeedKg" to newFeedKg,
            "latestAvgWeightGrams" to newWeight,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        logId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, logRef.path)
        throw e
      }
    }
  }

  suspend fun addFeedDispatch(
    batch: FlockBatch,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    challanNo: String,
    dispatchEpochDay: Long,
    vehicleNo: String = "",
    remarks: String = "",
    unitRate: Double = 0.0,
    rateBasis: String = FeedRateBasis.PER_BAG.value,
    totalAmount: Double? = null,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val dispatchId = "dsp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = feedDispatchesCollection(uid).document(dispatchId)
      val normalizedBasis = FeedRateBasis.fromValue(rateBasis).value
      val safeRate = unitRate.coerceIn(0.0, 1000000.0)
      val computedTotal = (totalAmount ?: computeFeedDispatchTotalAmount(bagsCount, weightKg, safeRate, normalizedBasis))
        .coerceIn(0.0, 200000000.0)
      val payload = mapOf(
        "userId" to uid,
        "batchId" to batch.id,
        "farmerId" to batch.farmerId,
        "farmerName" to batch.farmerName.take(80),
        "farmCode" to batch.farmCode.take(30),
        "farmName" to batch.displayFarmName.take(100),
        "batchCode" to batch.batchCode.take(40),
        "feedType" to feedType,
        "bagsCount" to bagsCount,
        "weightKg" to weightKg,
        "unitRate" to safeRate,
        "rateBasis" to normalizedBasis,
        "totalAmount" to computedTotal,
        "challanNo" to challanNo.trim().take(40),
        "vehicleNo" to vehicleNo.trim().uppercase().take(30),
        "remarks" to remarks.trim().take(200),
        "dispatchEpochDay" to dispatchEpochDay,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        dispatchId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun addBirdSale(
    batch: FlockBatch,
    buyerName: String,
    vehicleNo: String,
    challanNo: String,
    birdsSold: Long,
    totalWeightKg: Double,
    ratePerKg: Double,
    saleEpochDay: Long,
    buyerPhone: String = "",
    remarks: String = "",
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Cannot add sales to a GC Closed batch" }
      val saleId = "sl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val saleRef = birdSalesCollection(uid).document(saleId)
      val batchRef = batchesCollection(uid).document(batch.id)
      val saleAmount = totalWeightKg * ratePerKg
      val autoAgeDays = kotlin.math.max(0L, saleEpochDay - batch.placementEpochDay)
      val autoAvgWeightKg = if (birdsSold > 0L && totalWeightKg > 0.0) {
        totalWeightKg / birdsSold.toDouble()
      } else {
        0.0
      }
      val payload = mapOf(
        "userId" to uid,
        "batchId" to batch.id,
        "farmerId" to batch.farmerId,
        "farmerName" to batch.farmerName.take(80),
        "farmCode" to batch.farmCode.take(30),
        "batchCode" to batch.batchCode.take(40),
        "buyerName" to buyerName.trim().take(80),
        "buyerPhone" to buyerPhone.trim().take(30),
        "vehicleNo" to vehicleNo.trim().uppercase().take(30),
        "challanNo" to challanNo.trim().take(40),
        "remarks" to remarks.trim().take(200),
        "birdsSold" to birdsSold,
        "totalWeightKg" to totalWeightKg,
        "ageDays" to autoAgeDays,
        "avgWeightKg" to autoAvgWeightKg,
        "ratePerKg" to ratePerKg,
        "saleAmount" to saleAmount,
        "saleEpochDay" to saleEpochDay,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        saleRef.set(payload).await()
        batchRef.update(
          mapOf(
            "liftedBirds" to (batch.liftedBirds + birdsSold),
            "liftedWeightKg" to (batch.liftedWeightKg + totalWeightKg),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        saleId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, saleRef.path)
        throw e
      }
    }
  }

  suspend fun addInterFeedTransfer(
    fromBatch: FlockBatch,
    toBatch: FlockBatch,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    transferRef: String,
    remarks: String,
    transferEpochDay: Long,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      require(fromBatch.id != toBatch.id) { "Source and destination batches must be different" }
      val transferId = "ift_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = feedTransfersCollection(uid).document(transferId)
      val payload = mapOf(
        "userId" to uid,
        "fromBatchId" to fromBatch.id,
        "fromFarmerId" to fromBatch.farmerId,
        "fromFarmerName" to fromBatch.farmerName,
        "fromFarmCode" to fromBatch.farmCode,
        "fromBatchCode" to fromBatch.batchCode,
        "toBatchId" to toBatch.id,
        "toFarmerId" to toBatch.farmerId,
        "toFarmerName" to toBatch.farmerName,
        "toFarmCode" to toBatch.farmCode,
        "toBatchCode" to toBatch.batchCode,
        "feedType" to feedType,
        "bagsCount" to bagsCount,
        "weightKg" to weightKg,
        "transferRef" to transferRef.trim(),
        "remarks" to remarks.trim(),
        "transferEpochDay" to transferEpochDay,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        transferId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  private fun GcStatementDetails.toFirestoreMap(): Map<String, Any> = mapOf(
    "companyName" to companyName.trim().take(120),
    "companyAddress" to companyAddress.trim(),
    "companyPhone" to companyPhone.trim(),
    "companyPanGst" to companyPanGst.trim(),
    "chickRate" to chickRate,
    "feedRatePerKg" to feedRatePerKg,
    "adminRatePerChick" to adminRatePerChick,
    "medicineCost" to medicineCost,
    "otherExpCost" to otherExpCost,
    "stdProductionCost" to stdProductionCost,
    "stdGcPerKg" to stdGcPerKg,
    "saleRatePerKg" to saleRatePerKg,
    "saleRateIncentive" to saleRateIncentive,
    "lowCostIncentive" to lowCostIncentive,
    "feedUnloadingAmount" to feedUnloadingAmount,
    "miscAmount" to miscAmount,
    "cfcrWaiverAmount" to cfcrWaiverAmount,
    "highCostDeduction" to highCostDeduction,
    "birdsShortageDeduction" to birdsShortageDeduction,
    "otherDeduction" to otherDeduction,
    "waiverIfAny" to waiverIfAny,
    "tdsPercent" to tdsPercent.coerceIn(0.0, 30.0),
    "shortageBirds" to shortageBirds,
    "shortageFeedBags" to shortageFeedBags,
    "grade" to grade.trim(),
    "meanAgeDays" to meanAgeDays,
    "miscDetail" to miscDetail.trim(),
    "otherDeductionDetail" to otherDeductionDetail.trim(),
    "createdByName" to createdByName.trim(),
    "approvedByName" to approvedByName.trim(),
    "enteredByName" to enteredByName.trim(),
    "paymentByName" to paymentByName.trim(),
  )

  suspend fun startClosingBatch(
    batch: FlockBatch,
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    closingNotes: String,
    gcDetails: GcStatementDetails = batch.gcDetails ?: GcStatementDetails(stdGcPerKg = batch.growingRatePerKg),
    todayEpochDay: Long = batch.placementEpochDay + batch.targetMarketAgeDays,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Batch is already GC Closed" }
      val summary = SettlementCalculator.computeGcStatement(
        batch = batch,
        liftedBirds = liftedBirds,
        liftedWeightKg = liftedWeightKg,
        returnedFeedKg = returnedFeedKg,
        details = gcDetails,
        todayEpochDay = todayEpochDay,
      )
      val batchRef = batchesCollection(uid).document(batch.id)
      val farmerRef = farmersCollection(uid).document(batch.farmerId)
      try {
        batchRef.update(
          mapOf(
            "status" to BatchStatus.CLOSING.value,
            "liftedBirds" to liftedBirds,
            "liftedWeightKg" to liftedWeightKg,
            "returnedFeedKg" to returnedFeedKg,
            "settlementPayout" to summary.totalPayable,
            "closingNotes" to closingNotes.trim(),
            "closedEpochDay" to 0L,
            "gcDetails" to gcDetails.toFirestoreMap(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        farmerRef.update(
          mapOf(
            "farmStatus" to FarmStatus.CLOSING.value,
            "currentBatchId" to batch.id,
            "currentBatchCode" to batch.batchCode,
            "cancelReason" to "",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, batchRef.path)
        throw e
      }
    }
  }

  suspend fun closeBatch(
    batch: FlockBatch,
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    closingNotes: String,
    closedEpochDay: Long,
    gcDetails: GcStatementDetails = batch.gcDetails ?: GcStatementDetails(stdGcPerKg = batch.growingRatePerKg),
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Batch is already GC Closed" }
      val summary = SettlementCalculator.computeGcStatement(
        batch = batch,
        liftedBirds = liftedBirds,
        liftedWeightKg = liftedWeightKg,
        returnedFeedKg = returnedFeedKg,
        details = gcDetails,
        todayEpochDay = closedEpochDay,
      )
      val batchRef = batchesCollection(uid).document(batch.id)
      val farmerRef = farmersCollection(uid).document(batch.farmerId)
      try {
        batchRef.update(
          mapOf(
            "status" to BatchStatus.CLOSED.value,
            "liftedBirds" to liftedBirds,
            "liftedWeightKg" to liftedWeightKg,
            "returnedFeedKg" to returnedFeedKg,
            "settlementPayout" to summary.totalPayable,
            "closingNotes" to closingNotes.trim(),
            "closedEpochDay" to closedEpochDay,
            "gcDetails" to gcDetails.toFirestoreMap(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        farmerRef.update(
          mapOf(
            "farmStatus" to FarmStatus.EMPTY.value,
            "currentBatchId" to "",
            "currentBatchCode" to "",
            "cancelReason" to "",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, batchRef.path)
        throw e
      }
    }
  }

  suspend fun deleteFarmer(farmerId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = farmersCollection(uid).document(farmerId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateBatch(
    batchId: String,
    batchCode: String,
    breed: String,
    chicksPlaced: Long,
    freeChicks: Long,
    placementEpochDay: Long,
    targetMarketAgeDays: Long,
    targetAvgWeightKg: Double,
    standardFcr: Double,
    growingRatePerKg: Double,
    region: String? = null,
    zoneName: String? = null,
    farmName: String? = null,
    weekNumber: String? = null,
    challanNo: String? = null,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = batchesCollection(uid).document(batchId)
      val updates = mutableMapOf<String, Any>(
        "batchCode" to batchCode.trim().uppercase().take(40),
        "breed" to breed.trim().take(50),
        "chicksPlaced" to chicksPlaced,
        "freeChicks" to freeChicks,
        "placementEpochDay" to placementEpochDay,
        "targetMarketAgeDays" to targetMarketAgeDays,
        "targetAvgWeightKg" to targetAvgWeightKg,
        "standardFcr" to standardFcr,
        "growingRatePerKg" to growingRatePerKg,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      region?.let { updates["region"] = it.trim().uppercase().take(40) }
      zoneName?.let { updates["zoneName"] = it.trim().uppercase().take(80) }
      farmName?.let { updates["farmName"] = it.trim().take(100) }
      weekNumber?.let { updates["weekNumber"] = it.trim().take(40) }
      challanNo?.let { updates["challanNo"] = it.trim().take(40) }
      try {
        docRef.update(updates).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun syncMasterBatchesCatalog(
    rows: List<CatalogBatchRow>,
    backfillExisting: Boolean = true,
  ): Result<SyncBatchCatalogStats> {
    return runCatching {
      val uid = requireUserId()
      if (rows.isEmpty()) return@runCatching SyncBatchCatalogStats(0, 0, 0, 0)

      val existingZones = zonesCollection(uid).get().await()
        .toObjects(IntegrationZone::class.java)
      val existingFarmers = farmersCollection(uid).get().await()
        .toObjects(Farmer::class.java)
      val existingBatches = batchesCollection(uid).get().await()
        .toObjects(FlockBatch::class.java)

      val zoneById = existingZones.associateBy { it.id }
      val zoneByNameUpper = existingZones.associateBy { it.zoneName.trim().uppercase() }
      val resolvedZoneIdByName = mutableMapOf<String, String>()

      val phaseOneWrites = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
      var zonesCreated = 0
      var stubFarmersCreated = 0

      // Prepare missing zones referenced by flock rows
      rows.forEach { row ->
        val key = row.zoneName.trim().uppercase().ifBlank { row.region.trim().uppercase() }
        if (key.isNotBlank() && !resolvedZoneIdByName.containsKey(key)) {
          val cleanPart = key.replace(Regex("[^A-Z0-9_-]+"), "_").trim('_').ifBlank { "ZONE" }
          val docId = "zn_cat_$cleanPart".take(120)
          val matched = zoneByNameUpper[key] ?: zoneById[docId]
          if (matched != null) {
            resolvedZoneIdByName[key] = matched.id
          } else {
            resolvedZoneIdByName[key] = docId
            val zoneRef = zonesCollection(uid).document(docId)
            val zonePayload = mapOf(
              "userId" to uid,
              "zoneCode" to cleanPart.replace("_", "-").take(30).ifBlank { "ZN" },
              "zoneName" to key.take(80),
              "region" to row.region.ifBlank { key }.take(80),
              "supervisorName" to "",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
            phaseOneWrites += { batch -> batch.set(zoneRef, zonePayload) }
            zonesCreated += 1
          }
        }
      }

      // Match existing farmers by exact or normalized (space-stripped) farmCode
      val farmersByNormalizedCode = existingFarmers.groupBy {
        it.farmCode.replace(" ", "").trim().uppercase()
      }
      val resolvedFarmerByNormCode = mutableMapOf<String, Farmer>()

      rows.forEach { row ->
        val normCode = row.normalizedFarmCode
        if (!resolvedFarmerByNormCode.containsKey(normCode)) {
          val candidates = farmersByNormalizedCode[normCode].orEmpty()
          val matched = candidates.firstOrNull {
            it.displayFarmName.equals(row.farmName, ignoreCase = true)
          } ?: candidates.firstOrNull()
          if (matched != null) {
            resolvedFarmerByNormCode[normCode] = matched
          } else {
            val stubDocId = "frm_cat_${normCode.replace(Regex("[^A-Z0-9_-]+"), "_").trim('_').ifBlank { "FARM" }}".take(120)
            val zoneKey = row.zoneName.trim().uppercase().ifBlank { row.region.trim().uppercase() }
            val resolvedZoneId = resolvedZoneIdByName[zoneKey] ?: ""
            val stubCapacity = row.housedChicks.coerceIn(100L, 500000L)
            val stubRef = farmersCollection(uid).document(stubDocId)
            val stubPayload = mapOf(
              "userId" to uid,
              "farmCode" to row.farmCode.take(30),
              "farmerName" to row.farmName.take(80),
              "farmName" to row.farmName.take(100),
              "phone" to "",
              "village" to "",
              "tehsil" to "",
              "region" to row.region.take(40),
              "farmType" to "",
              "zoneId" to resolvedZoneId,
              "zoneName" to row.zoneName.take(80),
              "panNumber" to "",
              "aadhaarNumber" to "",
              "accountNumber" to "",
              "bankName" to "",
              "ifscCode" to "",
              "accountType" to "",
              "shedType" to "Open-Sided",
              "shedCapacity" to stubCapacity,
              "totalSqft" to row.housedChicks.coerceIn(0L, 500000L),
              "growingRatePerKg" to 9.0,
              "farmStatus" to FarmStatus.EMPTY.value,
              "currentBatchId" to "",
              "currentBatchCode" to "",
              "cancelReason" to "",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
            phaseOneWrites += { batch -> batch.set(stubRef, stubPayload) }
            stubFarmersCreated += 1
            resolvedFarmerByNormCode[normCode] = Farmer(
              id = stubDocId,
              userId = uid,
              farmCode = row.farmCode,
              farmerName = row.farmName,
              farmName = row.farmName,
              region = row.region,
              zoneId = resolvedZoneId,
              zoneName = row.zoneName,
              shedCapacity = stubCapacity,
              totalSqft = row.housedChicks,
            )
          }
        }
      }

      // Commit zones & stub farmers BEFORE writing batches so security rules exists(farmers/farmerId) passes
      try {
        phaseOneWrites.chunked(400).forEach { chunk ->
          val batch = db.batch()
          chunk.forEach { op -> op(batch) }
          batch.commit().await()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid/farmers")
        throw e
      }

      val batchById = existingBatches.associateBy { it.id }
      val batchByCodeUpper = existingBatches.associateBy { it.batchCode.trim().uppercase() }
      val phaseTwoWrites = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
      var batchesCreated = 0
      var batchesBackfilled = 0

      rows.forEach { row ->
        val farmer = resolvedFarmerByNormCode[row.normalizedFarmCode] ?: return@forEach
        val existingMatch = batchById[row.docId] ?: batchByCodeUpper[row.batchCode.trim().uppercase()]

        if (existingMatch == null) {
          val batchRef = batchesCollection(uid).document(row.docId)
          val payload = mapOf(
            "userId" to uid,
            "batchCode" to row.batchCode.take(40),
            "farmerId" to farmer.id,
            "farmerName" to farmer.farmerName.ifBlank { row.farmName }.take(80),
            "farmName" to row.farmName.ifBlank { farmer.displayFarmName }.take(100),
            "farmCode" to farmer.farmCode.ifBlank { row.farmCode }.take(30),
            "region" to row.region.ifBlank { farmer.region }.take(40),
            "zoneName" to row.zoneName.ifBlank { farmer.zoneName }.take(80),
            "weekNumber" to row.weekNumber.take(40),
            "challanNo" to row.challanNo.take(40),
            "breed" to "Cobb 500",
            "chicksPlaced" to row.chicksPlaced,
            "freeChicks" to row.freeChicks,
            "placementEpochDay" to row.placementEpochDay,
            "targetMarketAgeDays" to 38L,
            "targetAvgWeightKg" to 2.2,
            "standardFcr" to 1.62,
            "growingRatePerKg" to (farmer.growingRatePerKg.takeIf { it > 0.0 } ?: 9.0),
            "status" to row.status.value,
            "cumulativeMortality" to 0L,
            "cumulativeFeedKg" to 0.0,
            "latestAvgWeightGrams" to 42.0,
            "liftedBirds" to 0L,
            "liftedWeightKg" to 0.0,
            "returnedFeedKg" to 0.0,
            "settlementPayout" to 0.0,
            "closingNotes" to if (row.status == BatchStatus.CLOSED) "Imported from flock master sheet" else "",
            "closedEpochDay" to row.closedEpochDay,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          phaseTwoWrites += { batch -> batch.set(batchRef, payload) }
          batchesCreated += 1
        } else if (backfillExisting && existingMatch.statusEnum != BatchStatus.CLOSED) {
          val updates = mutableMapOf<String, Any>()
          if (existingMatch.farmName.isBlank() && row.farmName.isNotBlank()) updates["farmName"] = row.farmName
          if (existingMatch.region.isBlank() && row.region.isNotBlank()) updates["region"] = row.region
          if (existingMatch.zoneName.isBlank() && row.zoneName.isNotBlank()) updates["zoneName"] = row.zoneName
          if (existingMatch.weekNumber.isBlank() && row.weekNumber.isNotBlank()) updates["weekNumber"] = row.weekNumber
          if (existingMatch.challanNo.isBlank() && row.challanNo.isNotBlank()) updates["challanNo"] = row.challanNo
          if (updates.isNotEmpty()) {
            updates["updatedAt"] = FieldValue.serverTimestamp()
            val batchRef = batchesCollection(uid).document(existingMatch.id)
            phaseTwoWrites += { batch -> batch.update(batchRef, updates) }
            batchesBackfilled += 1
          }
        }
      }

      try {
        // batches create rule calls exists(.../farmers/$(farmerId)); Firestore limits get()/exists() to 20 per WriteBatch.
        phaseTwoWrites.chunked(15).forEach { chunk ->
          val batch = db.batch()
          chunk.forEach { op -> op(batch) }
          batch.commit().await()
        }
        SyncBatchCatalogStats(
          zonesCreated = zonesCreated,
          stubFarmersCreated = stubFarmersCreated,
          batchesCreated = batchesCreated,
          batchesBackfilled = batchesBackfilled,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid/batches")
        throw e
      }
    }
  }

  suspend fun deleteBatch(batch: FlockBatch): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Cannot delete a GC Closed batch" }
      val batchRef = batchesCollection(uid).document(batch.id)
      val farmerRef = farmersCollection(uid).document(batch.farmerId)
      try {
        batchRef.delete().await()
        farmerRef.update(
          mapOf(
            "farmStatus" to FarmStatus.EMPTY.value,
            "currentBatchId" to "",
            "currentBatchCode" to "",
            "cancelReason" to "",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, batchRef.path)
        throw e
      }
    }
  }

  suspend fun updateDailyLog(
    log: DailyLog,
    batch: FlockBatch,
    mortalityCount: Long,
    cullCount: Long,
    feedConsumedKg: Double,
    avgWeightGrams: Double,
    shedTempCelsius: Double,
    looseBirds: Long,
    feederStock: Long,
    notes: String,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Cannot edit logs on a GC Closed batch" }
      val logRef = dailyLogsCollection(uid).document(log.id)
      val batchRef = batchesCollection(uid).document(batch.id)
      val deltaMort = (mortalityCount + cullCount) - (log.mortalityCount + log.cullCount)
      val deltaFeed = feedConsumedKg - log.feedConsumedKg
      try {
        logRef.update(
          mapOf(
            "dayNumber" to log.dayNumber,
            "logEpochDay" to log.logEpochDay,
            "mortalityCount" to mortalityCount,
            "cullCount" to cullCount,
            "feedConsumedKg" to feedConsumedKg,
            "avgWeightGrams" to avgWeightGrams,
            "shedTempCelsius" to shedTempCelsius,
            "looseBirds" to looseBirds,
            "feederStock" to feederStock,
            "notes" to notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        batchRef.update(
          mapOf(
            "cumulativeMortality" to (batch.cumulativeMortality + deltaMort).coerceAtLeast(0L),
            "cumulativeFeedKg" to (batch.cumulativeFeedKg + deltaFeed).coerceAtLeast(0.0),
            "latestAvgWeightGrams" to if (avgWeightGrams > 0.0) avgWeightGrams else batch.latestAvgWeightGrams,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, logRef.path)
        throw e
      }
    }
  }

  suspend fun deleteDailyLog(log: DailyLog, batch: FlockBatch?): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val logRef = dailyLogsCollection(uid).document(log.id)
      try {
        logRef.delete().await()
        if (batch != null && batch.statusEnum != BatchStatus.CLOSED) {
          batchesCollection(uid).document(batch.id).update(
            mapOf(
              "cumulativeMortality" to (batch.cumulativeMortality - log.mortalityCount - log.cullCount).coerceAtLeast(0L),
              "cumulativeFeedKg" to (batch.cumulativeFeedKg - log.feedConsumedKg).coerceAtLeast(0.0),
              "latestAvgWeightGrams" to batch.latestAvgWeightGrams,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, logRef.path)
        throw e
      }
    }
  }

  suspend fun updateFeedDispatch(
    dispatch: FeedDispatch,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    challanNo: String,
    vehicleNo: String = dispatch.vehicleNo,
    remarks: String = dispatch.remarks,
    dispatchEpochDay: Long = dispatch.dispatchEpochDay,
    unitRate: Double = dispatch.unitRate,
    rateBasis: String = dispatch.rateBasis,
    totalAmount: Double? = null,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = feedDispatchesCollection(uid).document(dispatch.id)
      val normalizedBasis = FeedRateBasis.fromValue(rateBasis).value
      val safeRate = unitRate.coerceIn(0.0, 1000000.0)
      val resolvedTotal = (totalAmount ?: computeFeedDispatchTotalAmount(bagsCount, weightKg, safeRate, normalizedBasis))
        .coerceIn(0.0, 200000000.0)
      try {
        docRef.update(
          mapOf(
            "feedType" to feedType,
            "bagsCount" to bagsCount,
            "weightKg" to weightKg,
            "unitRate" to safeRate,
            "rateBasis" to normalizedBasis,
            "totalAmount" to resolvedTotal,
            "challanNo" to challanNo.trim().take(40),
            "vehicleNo" to vehicleNo.trim().uppercase().take(30),
            "remarks" to remarks.trim().take(200),
            "dispatchEpochDay" to dispatchEpochDay,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteFeedDispatch(dispatchId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = feedDispatchesCollection(uid).document(dispatchId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateBirdSale(
    sale: BirdSale,
    batch: FlockBatch?,
    buyerName: String,
    vehicleNo: String,
    challanNo: String,
    birdsSold: Long,
    totalWeightKg: Double,
    ratePerKg: Double,
    buyerPhone: String = sale.buyerPhone,
    remarks: String = sale.remarks,
    saleEpochDay: Long = sale.saleEpochDay,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val saleRef = birdSalesCollection(uid).document(sale.id)
      val saleAmount = totalWeightKg * ratePerKg
      val autoAgeDays = batch?.let { kotlin.math.max(0L, saleEpochDay - it.placementEpochDay) } ?: sale.ageDays
      val autoAvgWeightKg = if (birdsSold > 0L && totalWeightKg > 0.0) {
        totalWeightKg / birdsSold.toDouble()
      } else {
        0.0
      }
      try {
        saleRef.update(
          mapOf(
            "buyerName" to buyerName.trim().take(80),
            "buyerPhone" to buyerPhone.trim().take(30),
            "vehicleNo" to vehicleNo.trim().uppercase().take(30),
            "challanNo" to challanNo.trim().take(40),
            "remarks" to remarks.trim().take(200),
            "birdsSold" to birdsSold,
            "totalWeightKg" to totalWeightKg,
            "ageDays" to autoAgeDays,
            "avgWeightKg" to autoAvgWeightKg,
            "ratePerKg" to ratePerKg,
            "saleAmount" to saleAmount,
            "saleEpochDay" to saleEpochDay,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        if (batch != null && batch.statusEnum != BatchStatus.CLOSED) {
          val deltaBirds = birdsSold - sale.birdsSold
          val deltaWeight = totalWeightKg - sale.totalWeightKg
          if (deltaBirds != 0L || deltaWeight != 0.0) {
            val batchDoc = batchesCollection(uid).document(batch.id).get().await()
            val currentBirds = batchDoc.getLong("liftedBirds") ?: batch.liftedBirds
            val currentWeight = batchDoc.getDouble("liftedWeightKg") ?: batch.liftedWeightKg
            batchesCollection(uid).document(batch.id).update(
              mapOf(
                "liftedBirds" to (currentBirds + deltaBirds).coerceAtLeast(0L),
                "liftedWeightKg" to (currentWeight + deltaWeight).coerceAtLeast(0.0),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            ).await()
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, saleRef.path)
        throw e
      }
    }
  }

  suspend fun deleteBirdSale(sale: BirdSale, batch: FlockBatch?): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val saleRef = birdSalesCollection(uid).document(sale.id)
      try {
        saleRef.delete().await()
        if (batch != null && batch.statusEnum != BatchStatus.CLOSED) {
          batchesCollection(uid).document(batch.id).update(
            mapOf(
              "liftedBirds" to (batch.liftedBirds - sale.birdsSold).coerceAtLeast(0L),
              "liftedWeightKg" to (batch.liftedWeightKg - sale.totalWeightKg).coerceAtLeast(0.0),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, saleRef.path)
        throw e
      }
    }
  }

  suspend fun updateFeedTransfer(
    transfer: FeedTransfer,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    transferRef: String,
    remarks: String,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = feedTransfersCollection(uid).document(transfer.id)
      try {
        docRef.update(
          mapOf(
            "feedType" to feedType,
            "bagsCount" to bagsCount,
            "weightKg" to weightKg,
            "transferRef" to transferRef.trim(),
            "remarks" to remarks.trim(),
            "transferEpochDay" to transfer.transferEpochDay,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteFeedTransfer(transferId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = feedTransfersCollection(uid).document(transferId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  suspend fun createDailyKmEntry(
    operatorId: String,
    empCode: String,
    operatorName: String,
    entryEpochDay: Long,
    startKm: Double,
    endKm: Double,
    visitedFarms: String,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      require(endKm >= startKm) { "Closing KM cannot be less than Start KM" }
      val totalKm = endKm - startKm
      val monthKey = KmMonthHelper.monthKeyFromEpochDay(entryEpochDay)
      val entryId = "kme_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = kmEntriesCollection(uid).document(entryId)
      val payload = mapOf(
        "userId" to uid,
        "operatorId" to operatorId,
        "empCode" to empCode.trim().uppercase(),
        "operatorName" to operatorName.trim(),
        "entryEpochDay" to entryEpochDay,
        "monthKey" to monthKey,
        "startKm" to startKm,
        "endKm" to endKm,
        "totalKm" to totalKm,
        "visitedFarms" to visitedFarms.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        entryId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateDailyKmEntry(
    entry: KmDailyEntry,
    startKm: Double,
    endKm: Double,
    visitedFarms: String,
    entryEpochDay: Long,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      require(endKm >= startKm) { "Closing KM cannot be less than Start KM" }
      val totalKm = endKm - startKm
      val monthKey = KmMonthHelper.monthKeyFromEpochDay(entryEpochDay)
      val docRef = kmEntriesCollection(uid).document(entry.id)
      try {
        docRef.update(
          mapOf(
            "startKm" to startKm,
            "endKm" to endKm,
            "totalKm" to totalKm,
            "visitedFarms" to visitedFarms.trim(),
            "entryEpochDay" to entryEpochDay,
            "monthKey" to monthKey,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteDailyKmEntry(entryId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = kmEntriesCollection(uid).document(entryId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  suspend fun submitMonthlyKmClaim(
    operatorId: String,
    empCode: String,
    operatorName: String,
    claimMonthKey: String,
    claimEpochDay: Long,
    dailyEntriesCount: Long,
    startKm: Double,
    endKm: Double,
    totalKm: Double,
    ratePerKm: Double,
    visitedFarms: String,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val normalizedEmp = empCode.trim().uppercase()
      val monthLabel = KmMonthHelper.monthLabelFromKey(claimMonthKey)
      require(dailyEntriesCount >= 1L) { "At least one daily KM entry is required before claiming $monthLabel." }
      require(totalKm >= 0.0 && ratePerKm >= 0.0) { "Invalid KM total or rate." }

      val claimId = KmMonthHelper.deterministicMonthlyClaimId(normalizedEmp, claimMonthKey)
      val docRef = kmClaimsCollection(uid).document(claimId)
      if (docRef.get().await().exists()) {
        throw IllegalStateException("Monthly KM claim for $monthLabel has already been submitted for $normalizedEmp.")
      }

      val claimAmount = totalKm * ratePerKm
      val payload = mapOf(
        "userId" to uid,
        "operatorId" to operatorId,
        "empCode" to normalizedEmp,
        "operatorName" to operatorName.trim(),
        "claimMonthKey" to claimMonthKey,
        "claimMonthLabel" to monthLabel,
        "claimEpochDay" to claimEpochDay,
        "dailyEntriesCount" to dailyEntriesCount,
        "startKm" to startKm,
        "endKm" to endKm,
        "totalKm" to totalKm,
        "ratePerKm" to ratePerKm,
        "claimAmount" to claimAmount,
        "visitedFarms" to visitedFarms.trim(),
        "status" to ClaimStatus.PENDING.value,
        "reviewedByEmpCode" to "",
        "reviewedByName" to "",
        "reviewRemarks" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        claimId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateMonthlyKmClaim(
    claim: KmClaim,
    totalKm: Double,
    ratePerKm: Double,
    visitedFarms: String,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      require(totalKm >= 0.0 && ratePerKm >= 0.0) { "Total KM and rate must be non-negative" }
      val claimAmount = totalKm * ratePerKm
      val docRef = kmClaimsCollection(uid).document(claim.id)
      try {
        docRef.update(
          mapOf(
            "totalKm" to totalKm,
            "ratePerKm" to ratePerKm,
            "claimAmount" to claimAmount,
            "visitedFarms" to visitedFarms.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun reviewKmClaim(
    claimId: String,
    status: ClaimStatus,
    reviewedByEmpCode: String,
    reviewedByName: String,
    reviewRemarks: String,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = kmClaimsCollection(uid).document(claimId)
      try {
        docRef.update(
          mapOf(
            "status" to status.value,
            "reviewedByEmpCode" to reviewedByEmpCode.trim().uppercase(),
            "reviewedByName" to reviewedByName.trim(),
            "reviewRemarks" to reviewRemarks.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteKmClaim(claimId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = kmClaimsCollection(uid).document(claimId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  suspend fun addMedicineEntry(
    batch: FlockBatch,
    medicineName: String,
    category: String,
    dosageOrQty: String,
    totalCost: Double,
    invoiceOrSlipNo: String,
    remarks: String,
    entryEpochDay: Long,
    enteredByEmpCode: String,
    enteredByName: String,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      require(batch.statusEnum != BatchStatus.CLOSED) { "Cannot add medicine entries to a GC Closed batch" }
      val entryId = "med_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = medicineEntriesCollection(uid).document(entryId)
      val payload = mapOf(
        "userId" to uid,
        "batchId" to batch.id,
        "farmerId" to batch.farmerId,
        "farmerName" to batch.farmerName,
        "farmCode" to batch.farmCode,
        "batchCode" to batch.batchCode,
        "medicineName" to medicineName.trim(),
        "category" to category,
        "dosageOrQty" to dosageOrQty.trim(),
        "totalCost" to totalCost.coerceAtLeast(0.0),
        "invoiceOrSlipNo" to invoiceOrSlipNo.trim().uppercase(),
        "remarks" to remarks.trim(),
        "entryEpochDay" to entryEpochDay,
        "enteredByEmpCode" to enteredByEmpCode.trim().uppercase(),
        "enteredByName" to enteredByName.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
        entryId
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateMedicineEntry(
    entry: MedicineEntry,
    medicineName: String,
    category: String,
    dosageOrQty: String,
    totalCost: Double,
    invoiceOrSlipNo: String,
    remarks: String,
    entryEpochDay: Long,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = medicineEntriesCollection(uid).document(entry.id)
      try {
        docRef.update(
          mapOf(
            "medicineName" to medicineName.trim(),
            "category" to category,
            "dosageOrQty" to dosageOrQty.trim(),
            "totalCost" to totalCost.coerceAtLeast(0.0),
            "invoiceOrSlipNo" to invoiceOrSlipNo.trim().uppercase(),
            "remarks" to remarks.trim(),
            "entryEpochDay" to entryEpochDay,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteMedicineEntry(entryId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = medicineEntriesCollection(uid).document(entryId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  suspend fun syncMasterMedicinesCatalog(
    rows: List<CatalogMedicineRow>,
    backfillExisting: Boolean = true,
  ): Result<SyncMedicineCatalogStats> {
    return runCatching {
      val uid = requireUserId()
      if (rows.isEmpty()) return@runCatching SyncMedicineCatalogStats(0, 0)

      val existingItems = medicineCatalogCollection(uid).get().await()
        .toObjects(MedicineCatalogItem::class.java)
      val byId = existingItems.associateBy { it.id }
      val byCodeUpper = existingItems.associateBy { it.medicineCode.trim().uppercase() }

      val writeOps = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
      var created = 0
      var backfilled = 0

      rows.forEach { row ->
        val existingMatch = byId[row.docId] ?: byCodeUpper[row.medicineCode.trim().uppercase()]
        if (existingMatch == null) {
          val ref = medicineCatalogCollection(uid).document(row.docId)
          val payload = mapOf(
            "userId" to uid,
            "medicineCode" to row.medicineCode.take(30),
            "medicineName" to row.medicineName.take(120),
            "category" to row.category,
            "packing" to row.packing.coerceIn(0.0, 100000.0),
            "unit" to row.unit.take(20),
            "rate" to row.rate.coerceIn(0.0, 10000000.0),
            "perKgOrLtr" to row.perKgOrLtr.coerceIn(0.0, 10000000.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          writeOps += { batch -> batch.set(ref, payload) }
          created += 1
        } else if (backfillExisting) {
          val needsUpdate =
            (existingMatch.unit.isBlank() && row.unit.isNotBlank()) ||
              (existingMatch.packing == 0.0 && row.packing > 0.0) ||
              (existingMatch.rate == 0.0 && row.rate > 0.0) ||
              (existingMatch.perKgOrLtr == 0.0 && row.perKgOrLtr > 0.0)
          if (needsUpdate) {
            val ref = medicineCatalogCollection(uid).document(existingMatch.id)
            writeOps += { batch ->
              batch.update(
                ref,
                mapOf(
                  "unit" to existingMatch.unit.ifBlank { row.unit }.take(20),
                  "packing" to if (existingMatch.packing == 0.0) row.packing else existingMatch.packing,
                  "rate" to if (existingMatch.rate == 0.0) row.rate else existingMatch.rate,
                  "perKgOrLtr" to if (existingMatch.perKgOrLtr == 0.0) row.perKgOrLtr else existingMatch.perKgOrLtr,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
            backfilled += 1
          }
        }
      }

      try {
        writeOps.chunked(400).forEach { chunk ->
          val batch = db.batch()
          chunk.forEach { op -> op(batch) }
          batch.commit().await()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid/medicinesCatalog")
        throw e
      }

      SyncMedicineCatalogStats(created, backfilled)
    }
  }

  suspend fun createMedicineCatalogItem(
    medicineCode: String,
    medicineName: String,
    category: String,
    packing: Double,
    unit: String,
    rate: Double,
    perKgOrLtr: Double,
  ): Result<String> {
    return runCatching {
      val uid = requireUserId()
      val cleanCode = medicineCode.trim().uppercase().take(30)
      val cleanName = medicineName.trim().take(120)
      require(cleanCode.isNotEmpty()) { "Medicine code is required" }
      require(cleanName.isNotEmpty()) { "Medicine name is required" }

      val docId = "med_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docRef = medicineCatalogCollection(uid).document(docId)
      val payload = mapOf(
        "userId" to uid,
        "medicineCode" to cleanCode,
        "medicineName" to cleanName,
        "category" to category,
        "packing" to packing.coerceIn(0.0, 100000.0),
        "unit" to unit.trim().uppercase().take(20),
        "rate" to rate.coerceIn(0.0, 10000000.0),
        "perKgOrLtr" to perKgOrLtr.coerceIn(0.0, 10000000.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
      docId
    }
  }

  suspend fun updateMedicineCatalogItem(
    itemId: String,
    medicineCode: String,
    medicineName: String,
    category: String,
    packing: Double,
    unit: String,
    rate: Double,
    perKgOrLtr: Double,
  ): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val cleanCode = medicineCode.trim().uppercase().take(30)
      val cleanName = medicineName.trim().take(120)
      require(cleanCode.isNotEmpty()) { "Medicine code is required" }
      require(cleanName.isNotEmpty()) { "Medicine name is required" }

      val docRef = medicineCatalogCollection(uid).document(itemId)
      try {
        docRef.update(
          mapOf(
            "medicineCode" to cleanCode,
            "medicineName" to cleanName,
            "category" to category,
            "packing" to packing.coerceIn(0.0, 100000.0),
            "unit" to unit.trim().uppercase().take(20),
            "rate" to rate.coerceIn(0.0, 10000000.0),
            "perKgOrLtr" to perKgOrLtr.coerceIn(0.0, 10000000.0),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun deleteMedicineCatalogItem(itemId: String): Result<Unit> {
    return runCatching {
      val uid = requireUserId()
      val docRef = medicineCatalogCollection(uid).document(itemId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  private fun logStreamErrorIfAuthenticated(
    uid: String,
    error: Throwable,
    operationType: OperationType,
    path: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }
}
