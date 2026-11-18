package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.MasterBatchCatalog
import com.example.data.catalog.MasterFarmerCatalog
import com.example.data.catalog.MasterMedicineCatalog
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.ClaimStatus
import com.example.data.model.DEFAULT_SUPER_ADMIN_EMAIL
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
import com.example.data.model.findOperatorForEmailMapping
import com.example.data.model.normalizeMappedEmail
import com.example.data.repository.BroilerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppDestination(val label: String) {
  OVERVIEW("Overview"),
  FARMS("Farms"),
  BATCHES("Batches"),
  SETTLEMENTS("Ledger & PDF"),
  STAFF("Staff"),
  DAILY_ENTRY("Daily Entry"),
  FEED_DISPATCH("Feed Dispatch"),
  FEED_TRANSFER("Inter Feed"),
  KM_CLAIMS("KM & Claim"),
}

enum class FarmFilter(val label: String, val status: FarmStatus?) {
  ALL("All Farms", null),
  ACTIVE("Active Farm", FarmStatus.ACTIVE),
  CLOSING("Closing Farm", FarmStatus.CLOSING),
  EMPTY("Empty Farm", FarmStatus.EMPTY),
  CANCELLED("Cancel Farm List", FarmStatus.CANCELLED),
}

enum class BatchFilter(val label: String, val status: BatchStatus?) {
  ALL("All Batches", null),
  ACTIVE("Active", BatchStatus.ACTIVE),
  CLOSING("Closing", BatchStatus.CLOSING),
  CLOSED("GC Closed", BatchStatus.CLOSED),
}

sealed interface BroilerDataState {
  data object Loading : BroilerDataState
  data class Ready(
    val operators: List<OperatorAccount>,
    val zones: List<IntegrationZone>,
    val farmers: List<Farmer>,
    val batches: List<FlockBatch>,
    val dailyLogs: List<DailyLog>,
    val feedDispatches: List<FeedDispatch>,
    val birdSales: List<BirdSale>,
    val feedTransfers: List<FeedTransfer>,
    val kmEntries: List<KmDailyEntry>,
    val kmClaims: List<KmClaim>,
    val medicineEntries: List<MedicineEntry>,
    val medicinesCatalog: List<MedicineCatalogItem>,
  ) : BroilerDataState
  data class Error(val message: String) : BroilerDataState
}

data class BroilerUiControls(
  val activeOperator: OperatorAccount? = null,
  val manualOperatorLogout: Boolean = false,
  val loginError: String? = null,
  val currentDestination: AppDestination = AppDestination.OVERVIEW,
  val farmFilter: FarmFilter = FarmFilter.ALL,
  val selectedZoneFilterId: String? = null,
  val batchFilter: BatchFilter = BatchFilter.ALL,
  val farmSearchQuery: String = "",
  val batchSearchQuery: String = "",
  val selectedFarmerId: String? = null,
  val selectedBatchId: String? = null,
  val isSaving: Boolean = false,
  val snackbarMessage: String? = null,
)

private data class CoreStreams(
  val operators: List<OperatorAccount>,
  val zones: List<IntegrationZone>,
  val farmers: List<Farmer>,
  val batches: List<FlockBatch>,
)

private data class LedgerStreams(
  val dailyLogs: List<DailyLog>,
  val feedDispatches: List<FeedDispatch>,
  val birdSales: List<BirdSale>,
  val feedTransfers: List<FeedTransfer>,
)

private data class OperationsStreams(
  val kmEntries: List<KmDailyEntry>,
  val kmClaims: List<KmClaim>,
  val medicineEntries: List<MedicineEntry>,
  val medicinesCatalog: List<MedicineCatalogItem>,
)

class BroilerViewModel(
  private val repository: BroilerRepository,
  private val currentUserId: String,
) : ViewModel() {

  val todayEpochDay: Long
    get() = System.currentTimeMillis() / MILLIS_PER_DAY

  private val coreFlow = combine(
    repository.observeOperators(currentUserId),
    repository.observeZones(currentUserId),
    repository.observeFarmers(currentUserId),
    repository.observeBatches(currentUserId),
  ) { operators, zones, farmers, batches ->
    CoreStreams(operators, zones, farmers, batches)
  }

  private val ledgerFlow = combine(
    repository.observeDailyLogs(currentUserId),
    repository.observeFeedDispatches(currentUserId),
    repository.observeBirdSales(currentUserId),
    repository.observeFeedTransfers(currentUserId),
  ) { dailyLogs, feedDispatches, birdSales, feedTransfers ->
    LedgerStreams(dailyLogs, feedDispatches, birdSales, feedTransfers)
  }

  private val operationsFlow = combine(
    repository.observeKmEntries(currentUserId),
    repository.observeKmClaims(currentUserId),
    repository.observeMedicineEntries(currentUserId),
    repository.observeMedicineCatalog(currentUserId),
  ) { kmEntries, kmClaims, medicineEntries, medicinesCatalog ->
    OperationsStreams(kmEntries, kmClaims, medicineEntries, medicinesCatalog)
  }

  val dataState: StateFlow<BroilerDataState> = combine(
    coreFlow,
    ledgerFlow,
    operationsFlow,
  ) { core, ledger, ops ->
    BroilerDataState.Ready(
      operators = core.operators,
      zones = core.zones,
      farmers = core.farmers,
      batches = core.batches,
      dailyLogs = ledger.dailyLogs,
      feedDispatches = ledger.feedDispatches,
      birdSales = ledger.birdSales,
      feedTransfers = ledger.feedTransfers,
      kmEntries = ops.kmEntries,
      kmClaims = ops.kmClaims,
      medicineEntries = ops.medicineEntries,
      medicinesCatalog = ops.medicinesCatalog,
    ) as BroilerDataState
  }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing integration streams", rootError)
      emit(BroilerDataState.Error(rootError.message ?: "Failed to sync SPF integration data"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = BroilerDataState.Loading,
    )

  private val _controls = MutableStateFlow(BroilerUiControls())
  val controls: StateFlow<BroilerUiControls> = _controls.asStateFlow()

  private var catalogSyncJob: Job? = null

  fun syncMasterFarmersIfNeeded(context: Context) {
    val appContext = context.applicationContext
    val prefs = appContext.getSharedPreferences(CATALOG_PREFS_NAME, Context.MODE_PRIVATE)
    val prefKey = "${CATALOG_SYNC_KEY_PREFIX}_$currentUserId"
    if (prefs.getBoolean(prefKey, false) || catalogSyncJob?.isActive == true) return

    catalogSyncJob = viewModelScope.launch {
      try {
        val (farmerRows, batchRows, medicineRows) = withContext(Dispatchers.IO) {
          Triple(
            MasterFarmerCatalog.loadFromAssets(appContext),
            MasterBatchCatalog.loadFromAssets(appContext),
            MasterMedicineCatalog.loadFromAssets(appContext),
          )
        }
        val farmerStats = repository.syncMasterFarmersCatalog(farmerRows, backfillExisting = true).getOrThrow()
        val batchStats = repository.syncMasterBatchesCatalog(batchRows, backfillExisting = true).getOrThrow()
        val medStats = repository.syncMasterMedicinesCatalog(medicineRows, backfillExisting = true).getOrThrow()
        prefs.edit().putBoolean(prefKey, true).apply()
        val totalZones = farmerStats.zonesCreated + batchStats.zonesCreated
        val totalFarms = farmerStats.farmersCreated + batchStats.stubFarmersCreated
        if (totalFarms > 0 || totalZones > 0 || batchStats.batchesCreated > 0 || medStats.itemsCreated > 0) {
          _controls.update {
            it.copy(
              snackbarMessage = "Synced $totalFarms farms, ${batchStats.batchesCreated} flocks, ${medStats.itemsCreated} medicines & $totalZones zones"
            )
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Auto master catalog sync failed", e)
      }
    }
  }

  fun syncMasterFarmersNow(context: Context) {
    requireAdminOrSuperAdmin {
      val appContext = context.applicationContext
      runMutation("Synced master farmer directory") {
        val rows = withContext(Dispatchers.IO) {
          MasterFarmerCatalog.loadFromAssets(appContext)
        }
        val stats = repository.syncMasterFarmersCatalog(rows, backfillExisting = true).getOrThrow()
        _controls.update {
          it.copy(
            snackbarMessage = "Synced ${stats.farmersCreated} new, ${stats.farmersBackfilled} updated, ${stats.zonesCreated} zones"
          )
        }
      }
    }
  }

  fun syncMasterBatchesNow(context: Context) {
    requireAdminOrSuperAdmin {
      val appContext = context.applicationContext
      runMutation("Synced master flock sheet") {
        val (farmerRows, batchRows) = withContext(Dispatchers.IO) {
          MasterFarmerCatalog.loadFromAssets(appContext) to MasterBatchCatalog.loadFromAssets(appContext)
        }
        val farmerStats = repository.syncMasterFarmersCatalog(farmerRows, backfillExisting = true).getOrThrow()
        val batchStats = repository.syncMasterBatchesCatalog(batchRows, backfillExisting = true).getOrThrow()
        appContext.getSharedPreferences(CATALOG_PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putBoolean("${CATALOG_SYNC_KEY_PREFIX}_$currentUserId", true)
          .apply()
        val totalZones = farmerStats.zonesCreated + batchStats.zonesCreated
        _controls.update {
          it.copy(
            snackbarMessage = "Synced ${batchStats.batchesCreated} new flocks, ${batchStats.batchesBackfilled} updated (${batchStats.stubFarmersCreated + farmerStats.farmersCreated} farms, $totalZones zones)"
          )
        }
      }
    }
  }

  fun syncMasterMedicinesNow(context: Context) {
    requireAdminOrSuperAdmin {
      val appContext = context.applicationContext
      runMutation("Synced medicine & vaccine catalog") {
        val rows = withContext(Dispatchers.IO) {
          MasterMedicineCatalog.loadFromAssets(appContext)
        }
        val stats = repository.syncMasterMedicinesCatalog(rows, backfillExisting = true).getOrThrow()
        _controls.update {
          it.copy(
            snackbarMessage = "Synced ${stats.itemsCreated} new, ${stats.itemsBackfilled} updated medicine items"
          )
        }
      }
    }
  }

  private fun currentOperator(): OperatorAccount? {
    val selected = _controls.value.activeOperator ?: return null
    val readyOperators = (dataState.value as? BroilerDataState.Ready)?.operators.orEmpty()
    return readyOperators.firstOrNull { it.id == selected.id } ?: selected
  }

  private fun resolveEffectiveEpochDay(requestedEpochDay: Long?): Long {
    val canBackdate = currentOperator()?.canSelectPreviousDate == true
    val today = todayEpochDay
    return if (canBackdate && requestedEpochDay != null && requestedEpochDay in 18000L..today) {
      requestedEpochDay
    } else {
      today
    }
  }

  private inline fun requireSuperAdmin(action: () -> Unit) {
    if (_controls.value.activeOperator?.roleEnum == OperatorRole.SUPER_ADMIN) {
      action()
    } else {
      _controls.update { it.copy(snackbarMessage = "Only Super Admin can manage staff accounts or permissions.") }
    }
  }

  private inline fun requireAdminOrSuperAdmin(action: () -> Unit) {
    val role = _controls.value.activeOperator?.roleEnum
    if (role == OperatorRole.SUPER_ADMIN || role == OperatorRole.ADMIN) {
      action()
    } else {
      _controls.update { it.copy(snackbarMessage = "Only Super Admin and Admin can update, approve, or delete records.") }
    }
  }

  private var autoEmailProvisionJob: Job? = null
  private var autoEmailProvisionAttempted = false

  fun resolveAutoEmailLogin(
    workspaceEmail: String?,
    displayName: String?,
    operators: List<OperatorAccount>,
  ) {
    val current = _controls.value
    if (current.activeOperator != null || current.manualOperatorLogout) return

    val directMatch = findOperatorForEmailMapping(operators, workspaceEmail)
    if (directMatch != null) {
      val dest = if (directMatch.roleEnum == OperatorRole.USER) {
        AppDestination.DAILY_ENTRY
      } else {
        AppDestination.OVERVIEW
      }
      _controls.update {
        it.copy(
          activeOperator = directMatch,
          loginError = null,
          currentDestination = dest,
          snackbarMessage = "Email mapped: signed in as ${directMatch.roleEnum.label} (${directMatch.empCode})",
        )
      }
      return
    }

    val cleanEmail = normalizeMappedEmail(workspaceEmail)
    if (cleanEmail == DEFAULT_SUPER_ADMIN_EMAIL &&
      !autoEmailProvisionAttempted &&
      autoEmailProvisionJob?.isActive != true
    ) {
      autoEmailProvisionAttempted = true
      autoEmailProvisionJob = viewModelScope.launch {
        try {
          val unmappedSuper = operators.firstOrNull {
            it.isActive && it.roleEnum == OperatorRole.SUPER_ADMIN && it.mappedEmail.isBlank()
          }
          val resolvedOp = if (unmappedSuper != null) {
            repository.updateOperator(
              operatorId = unmappedSuper.id,
              fullName = unmappedSuper.fullName,
              role = unmappedSuper.roleEnum,
              isActive = unmappedSuper.isActive,
              allowPreviousDateEntry = true,
              mappedEmail = cleanEmail,
            ).getOrThrow()
            unmappedSuper.copy(mappedEmail = cleanEmail, allowPreviousDateEntry = true)
          } else {
            val code = if (operators.none { it.empCode.equals("SA-001", ignoreCase = true) }) {
              "SA-001"
            } else {
              "SA-EMAIL"
            }
            val opName = displayName?.takeIf { it.isNotBlank() } ?: "Meena (Super Admin)"
            val randomSeedPwd = java.util.UUID.randomUUID().toString()
            val opId = repository.createOperator(
              empCode = code,
              fullName = opName,
              role = OperatorRole.SUPER_ADMIN,
              password = randomSeedPwd,
              allowPreviousDateEntry = true,
              mappedEmail = cleanEmail,
            ).getOrThrow()
            OperatorAccount(
              id = opId,
              userId = currentUserId,
              empCode = code,
              fullName = opName,
              role = OperatorRole.SUPER_ADMIN.value,
              isActive = true,
              allowPreviousDateEntry = true,
              mappedEmail = cleanEmail,
            )
          }
          _controls.update {
            it.copy(
              activeOperator = resolvedOp,
              loginError = null,
              currentDestination = AppDestination.OVERVIEW,
              snackbarMessage = "Super Admin granted for $cleanEmail (${resolvedOp.empCode})",
            )
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Auto email Super Admin mapping failed", e)
        }
      }
    }
  }

  fun loginWithMappedEmail(operator: OperatorAccount) {
    if (!operator.isActive) {
      _controls.update { it.copy(loginError = "Account ${operator.empCode} is disabled. Contact Super Admin.") }
      return
    }
    val dest = if (operator.roleEnum == OperatorRole.USER) {
      AppDestination.DAILY_ENTRY
    } else {
      AppDestination.OVERVIEW
    }
    _controls.update {
      it.copy(
        activeOperator = operator,
        manualOperatorLogout = false,
        loginError = null,
        currentDestination = dest,
      )
    }
  }

  fun loginWithEmpCode(
    operators: List<OperatorAccount>,
    selectedRole: OperatorRole,
    empCode: String,
    password: String,
  ) {
    val normalized = empCode.trim().uppercase()
    if (normalized.isBlank() || password.isBlank()) {
      _controls.update { it.copy(loginError = "Enter both Employee Code and Password.") }
      return
    }
    val match = operators.firstOrNull { it.empCode.equals(normalized, ignoreCase = true) }
    if (match == null) {
      _controls.update { it.copy(loginError = "Employee code $normalized not found.") }
      return
    }
    if (!match.isActive) {
      _controls.update { it.copy(loginError = "Account $normalized is disabled. Contact Super Admin.") }
      return
    }
    if (match.roleEnum != selectedRole) {
      _controls.update {
        it.copy(loginError = "$normalized is registered as ${match.roleEnum.label}, not ${selectedRole.label}.")
      }
      return
    }
    if (!PasswordHasher.verifyPassword(password, match.passwordHash)) {
      _controls.update { it.copy(loginError = "Incorrect password for $normalized.") }
      return
    }
    val startDestination = if (match.roleEnum == OperatorRole.USER) {
      AppDestination.DAILY_ENTRY
    } else {
      AppDestination.OVERVIEW
    }
    _controls.update {
      it.copy(
        activeOperator = match,
        manualOperatorLogout = false,
        loginError = null,
        currentDestination = startDestination,
      )
    }
  }

  fun bootstrapSuperAdmin(
    empCode: String,
    fullName: String,
    password: String,
    mappedEmail: String = "",
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isSaving = true, loginError = null) }
      try {
        val cleanEmail = normalizeMappedEmail(mappedEmail)
        val opId = repository.createOperator(
          empCode = empCode,
          fullName = fullName,
          role = OperatorRole.SUPER_ADMIN,
          password = password,
          allowPreviousDateEntry = true,
          mappedEmail = cleanEmail,
        ).getOrThrow()
        val created = OperatorAccount(
          id = opId,
          userId = currentUserId,
          empCode = empCode.trim().uppercase(),
          fullName = fullName.trim(),
          role = OperatorRole.SUPER_ADMIN.value,
          isActive = true,
          allowPreviousDateEntry = true,
          mappedEmail = cleanEmail,
        )
        _controls.update {
          it.copy(
            isSaving = false,
            activeOperator = created,
            manualOperatorLogout = false,
            loginError = null,
            currentDestination = AppDestination.OVERVIEW,
            snackbarMessage = "Super Admin ${created.empCode} created & signed in",
          )
        }
      } catch (e: CancellationException) {
        _controls.update { it.copy(isSaving = false) }
        throw e
      } catch (e: Exception) {
        _controls.update {
          it.copy(
            isSaving = false,
            loginError = e.localizedMessage ?: "Failed to create Super Admin account",
          )
        }
      }
    }
  }

  fun logoutOperator() {
    _controls.update {
      it.copy(
        activeOperator = null,
        manualOperatorLogout = true,
        loginError = null,
        currentDestination = AppDestination.OVERVIEW,
        selectedFarmerId = null,
        selectedBatchId = null,
      )
    }
  }

  fun clearLoginError() {
    _controls.update { it.copy(loginError = null) }
  }

  fun createOperator(
    empCode: String,
    fullName: String,
    role: OperatorRole,
    password: String,
    allowPreviousDateEntry: Boolean,
    mappedEmail: String = "",
  ) {
    requireSuperAdmin {
      runMutation("Created ${role.label} account ${empCode.trim().uppercase()}") {
        repository.createOperator(
          empCode = empCode,
          fullName = fullName,
          role = role,
          password = password,
          allowPreviousDateEntry = allowPreviousDateEntry,
          mappedEmail = mappedEmail,
        ).getOrThrow()
      }
    }
  }

  fun updateOperator(
    operatorId: String,
    fullName: String,
    role: OperatorRole,
    isActive: Boolean,
    newPassword: String?,
    allowPreviousDateEntry: Boolean,
    mappedEmail: String? = null,
  ) {
    requireSuperAdmin {
      runMutation("Updated staff account $fullName") {
        repository.updateOperator(
          operatorId = operatorId,
          fullName = fullName,
          role = role,
          isActive = isActive,
          newPassword = newPassword,
          allowPreviousDateEntry = allowPreviousDateEntry,
          mappedEmail = mappedEmail,
        ).getOrThrow()
      }
    }
  }

  fun deleteOperator(operator: OperatorAccount) {
    requireSuperAdmin {
      runMutation("Removed ${operator.empCode}") {
        repository.deleteOperator(operator.id).getOrThrow()
      }
    }
  }

  fun createZone(
    zoneCode: String,
    zoneName: String,
    region: String,
    supervisorName: String,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Created zone ${zoneName.trim().uppercase()}") {
        repository.createZone(zoneCode, zoneName, region, supervisorName).getOrThrow()
      }
    }
  }

  fun updateZone(
    zoneId: String,
    zoneCode: String,
    zoneName: String,
    region: String,
    supervisorName: String,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Updated zone ${zoneName.trim().uppercase()}") {
        repository.updateZone(zoneId, zoneCode, zoneName, region, supervisorName).getOrThrow()
      }
    }
  }

  fun deleteZone(zone: IntegrationZone) {
    requireAdminOrSuperAdmin {
      runMutation("Removed zone ${zone.zoneName}") {
        repository.deleteZone(zone.id).getOrThrow()
      }
    }
  }

  fun navigateTo(destination: AppDestination) {
    _controls.update { it.copy(currentDestination = destination) }
  }

  fun openFarmsWithFilter(filter: FarmFilter) {
    _controls.update {
      it.copy(
        currentDestination = AppDestination.FARMS,
        farmFilter = filter,
        selectedFarmerId = null,
      )
    }
  }

  fun openBatchDetail(batchId: String) {
    _controls.update {
      it.copy(
        currentDestination = AppDestination.BATCHES,
        batchFilter = BatchFilter.ALL,
        selectedBatchId = batchId,
      )
    }
  }

  fun setFarmFilter(filter: FarmFilter) {
    _controls.update { it.copy(farmFilter = filter, selectedFarmerId = null) }
  }

  fun setZoneFilter(zoneId: String?) {
    _controls.update { it.copy(selectedZoneFilterId = zoneId, selectedFarmerId = null) }
  }

  fun setBatchFilter(filter: BatchFilter) {
    _controls.update { it.copy(batchFilter = filter, selectedBatchId = null) }
  }

  fun setFarmSearchQuery(query: String) {
    _controls.update { it.copy(farmSearchQuery = query) }
  }

  fun setBatchSearchQuery(query: String) {
    _controls.update { it.copy(batchSearchQuery = query) }
  }

  fun selectFarmer(farmerId: String?) {
    _controls.update { it.copy(selectedFarmerId = farmerId) }
  }

  fun selectBatch(batchId: String?) {
    _controls.update { it.copy(selectedBatchId = batchId) }
  }

  fun consumeSnackbar() {
    _controls.update { it.copy(snackbarMessage = null) }
  }

  fun showMessage(message: String) {
    _controls.update { it.copy(snackbarMessage = message) }
  }

  fun createFarmer(
    farmCode: String,
    farmerName: String,
    farmName: String,
    phone: String,
    village: String,
    tehsil: String,
    region: String,
    farmType: String,
    zoneId: String,
    zoneName: String,
    panNumber: String,
    aadhaarNumber: String,
    accountNumber: String,
    bankName: String,
    ifscCode: String,
    accountType: String,
    shedType: String,
    shedCapacity: Long,
    totalSqft: Long,
    growingRatePerKg: Double,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Farmer $farmCode added as Empty Farm") {
        repository.createFarmer(
          farmCode = farmCode,
          farmerName = farmerName,
          farmName = farmName,
          phone = phone,
          village = village,
          zoneId = zoneId,
          zoneName = zoneName,
          panNumber = panNumber,
          accountNumber = accountNumber,
          bankName = bankName,
          ifscCode = ifscCode,
          shedType = shedType,
          shedCapacity = shedCapacity,
          growingRatePerKg = growingRatePerKg,
          initialStatus = FarmStatus.EMPTY,
          tehsil = tehsil,
          region = region,
          farmType = farmType,
          aadhaarNumber = aadhaarNumber,
          accountType = accountType,
          totalSqft = totalSqft,
        ).getOrThrow()
      }
    }
  }

  fun updateFarmerProfile(
    farmerId: String,
    farmCode: String,
    farmerName: String,
    farmName: String,
    phone: String,
    village: String,
    tehsil: String,
    region: String,
    farmType: String,
    zoneId: String,
    zoneName: String,
    panNumber: String,
    aadhaarNumber: String,
    accountNumber: String,
    bankName: String,
    ifscCode: String,
    accountType: String,
    shedType: String,
    shedCapacity: Long,
    totalSqft: Long,
    growingRatePerKg: Double,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Updated farmer $farmerName") {
        repository.updateFarmerProfile(
          farmerId = farmerId,
          farmCode = farmCode,
          farmerName = farmerName,
          farmName = farmName,
          phone = phone,
          village = village,
          zoneId = zoneId,
          zoneName = zoneName,
          panNumber = panNumber,
          accountNumber = accountNumber,
          bankName = bankName,
          ifscCode = ifscCode,
          shedType = shedType,
          shedCapacity = shedCapacity,
          growingRatePerKg = growingRatePerKg,
          tehsil = tehsil,
          region = region,
          farmType = farmType,
          aadhaarNumber = aadhaarNumber,
          accountType = accountType,
          totalSqft = totalSqft,
        ).getOrThrow()
      }
    }
  }

  fun deleteFarmer(farmer: Farmer) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted farmer ${farmer.farmCode}") {
        repository.deleteFarmer(farmer.id).getOrThrow()
        _controls.update { it.copy(selectedFarmerId = null) }
      }
    }
  }

  fun markFarmStatus(
    farmerId: String,
    newStatus: FarmStatus,
    cancelReason: String = "",
  ) {
    requireAdminOrSuperAdmin {
      val label = when (newStatus) {
        FarmStatus.ACTIVE -> "Marked as Active Farm"
        FarmStatus.CLOSING -> "Moved to Closing Farm"
        FarmStatus.EMPTY -> "Marked as Empty Farm"
        FarmStatus.CANCELLED -> "Added to Cancel Farm List"
      }
      runMutation(label) {
        repository.updateFarmStatus(
          farmerId = farmerId,
          newStatus = newStatus,
          cancelReason = cancelReason,
        ).getOrThrow()
      }
    }
  }

  fun createBatch(
    farmer: Farmer,
    batchCode: String,
    breed: String,
    chicksPlaced: Long,
    freeChicks: Long,
    requestedPlacementEpochDay: Long?,
    targetMarketAgeDays: Long,
    targetAvgWeightKg: Double,
    standardFcr: Double,
    growingRatePerKg: Double,
    region: String = farmer.region,
    zoneName: String = farmer.zoneName,
    farmName: String = farmer.displayFarmName,
    weekNumber: String = "",
    challanNo: String = "",
  ) {
    requireAdminOrSuperAdmin {
      val placementDay = resolveEffectiveEpochDay(requestedPlacementEpochDay)
      runMutation("Batch $batchCode placed — ${farmer.farmCode} is now Active Farm") {
        val batchId = repository.createBatch(
          farmer = farmer,
          batchCode = batchCode,
          breed = breed,
          chicksPlaced = chicksPlaced,
          freeChicks = freeChicks,
          placementEpochDay = placementDay,
          targetMarketAgeDays = targetMarketAgeDays,
          targetAvgWeightKg = targetAvgWeightKg,
          standardFcr = standardFcr,
          growingRatePerKg = growingRatePerKg,
          region = region,
          zoneName = zoneName,
          farmName = farmName,
          weekNumber = weekNumber,
          challanNo = challanNo,
        ).getOrThrow()
        _controls.update { it.copy(selectedBatchId = batchId) }
      }
    }
  }

  fun updateBatch(
    batchId: String,
    batchCode: String,
    breed: String,
    chicksPlaced: Long,
    freeChicks: Long,
    requestedPlacementEpochDay: Long?,
    existingPlacementEpochDay: Long,
    targetMarketAgeDays: Long,
    targetAvgWeightKg: Double,
    standardFcr: Double,
    growingRatePerKg: Double,
    region: String? = null,
    zoneName: String? = null,
    farmName: String? = null,
    weekNumber: String? = null,
    challanNo: String? = null,
  ) {
    requireAdminOrSuperAdmin {
      val effectivePlacementDay = if (currentOperator()?.canSelectPreviousDate == true && requestedPlacementEpochDay != null) {
        requestedPlacementEpochDay
      } else {
        existingPlacementEpochDay
      }
      runMutation("Updated batch $batchCode") {
        repository.updateBatch(
          batchId = batchId,
          batchCode = batchCode,
          breed = breed,
          chicksPlaced = chicksPlaced,
          freeChicks = freeChicks,
          placementEpochDay = effectivePlacementDay,
          targetMarketAgeDays = targetMarketAgeDays,
          targetAvgWeightKg = targetAvgWeightKg,
          standardFcr = standardFcr,
          growingRatePerKg = growingRatePerKg,
          region = region,
          zoneName = zoneName,
          farmName = farmName,
          weekNumber = weekNumber,
          challanNo = challanNo,
        ).getOrThrow()
      }
    }
  }

  fun deleteBatch(batch: FlockBatch) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted batch ${batch.batchCode} — ${batch.farmCode} reset to Empty Farm") {
        repository.deleteBatch(batch).getOrThrow()
        _controls.update { it.copy(selectedBatchId = null) }
      }
    }
  }

  fun addDailyLog(
    batch: FlockBatch,
    requestedLogEpochDay: Long?,
    mortalityCount: Long,
    cullCount: Long,
    feedConsumedKg: Double,
    avgWeightGrams: Double,
    shedTempCelsius: Double,
    looseBirds: Long,
    feederStock: Long,
    notes: String,
  ) {
    val logEpochDay = resolveEffectiveEpochDay(requestedLogEpochDay)
    val autoDay = batch.currentAgeDays(logEpochDay)
    runMutation("Day $autoDay log synced for ${batch.batchCode}") {
      repository.addDailyLog(
        batch = batch,
        dayNumber = autoDay,
        logEpochDay = logEpochDay,
        mortalityCount = mortalityCount,
        cullCount = cullCount,
        feedConsumedKg = feedConsumedKg,
        avgWeightGrams = avgWeightGrams,
        shedTempCelsius = shedTempCelsius,
        looseBirds = looseBirds,
        feederStock = feederStock,
        notes = notes,
      ).getOrThrow()
    }
  }

  fun updateDailyLog(
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
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Updated Day ${log.dayNumber} log for ${batch.batchCode}") {
        repository.updateDailyLog(
          log = log,
          batch = batch,
          mortalityCount = mortalityCount,
          cullCount = cullCount,
          feedConsumedKg = feedConsumedKg,
          avgWeightGrams = avgWeightGrams,
          shedTempCelsius = shedTempCelsius,
          looseBirds = looseBirds,
          feederStock = feederStock,
          notes = notes,
        ).getOrThrow()
      }
    }
  }

  fun deleteDailyLog(log: DailyLog, batch: FlockBatch?) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted Day ${log.dayNumber} log") {
        repository.deleteDailyLog(log, batch).getOrThrow()
      }
    }
  }

  fun addFeedDispatch(
    batch: FlockBatch,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    challanNo: String,
    requestedDispatchEpochDay: Long? = null,
    vehicleNo: String = "",
    remarks: String = "",
    unitRate: Double = 0.0,
    rateBasis: String = FeedRateBasis.PER_BAG.value,
    totalAmount: Double? = null,
  ) {
    val dispatchDay = resolveEffectiveEpochDay(requestedDispatchEpochDay)
    runMutation("Dispatched $bagsCount bags ($feedType) to ${batch.farmCode}") {
      repository.addFeedDispatch(
        batch = batch,
        feedType = feedType,
        bagsCount = bagsCount,
        weightKg = weightKg,
        challanNo = challanNo,
        dispatchEpochDay = dispatchDay,
        vehicleNo = vehicleNo,
        remarks = remarks,
        unitRate = unitRate,
        rateBasis = rateBasis,
        totalAmount = totalAmount,
      ).getOrThrow()
    }
  }

  fun updateFeedDispatch(
    dispatch: FeedDispatch,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    challanNo: String,
    vehicleNo: String = dispatch.vehicleNo,
    remarks: String = dispatch.remarks,
    requestedEpochDay: Long? = null,
    unitRate: Double = dispatch.unitRate,
    rateBasis: String = dispatch.rateBasis,
    totalAmount: Double? = null,
  ) {
    requireAdminOrSuperAdmin {
      val effectiveDay = if (currentOperator()?.canSelectPreviousDate == true && requestedEpochDay != null) {
        requestedEpochDay
      } else {
        dispatch.dispatchEpochDay
      }
      runMutation("Updated feed challan ${challanNo.uppercase()}") {
        repository.updateFeedDispatch(
          dispatch = dispatch,
          feedType = feedType,
          bagsCount = bagsCount,
          weightKg = weightKg,
          challanNo = challanNo,
          vehicleNo = vehicleNo,
          remarks = remarks,
          dispatchEpochDay = effectiveDay,
          unitRate = unitRate,
          rateBasis = rateBasis,
          totalAmount = totalAmount,
        ).getOrThrow()
      }
    }
  }

  fun deleteFeedDispatch(dispatch: FeedDispatch) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted feed dispatch ${dispatch.challanNo}") {
        repository.deleteFeedDispatch(dispatch.id).getOrThrow()
      }
    }
  }

  fun addBirdSale(
    batch: FlockBatch,
    buyerName: String,
    vehicleNo: String,
    challanNo: String,
    birdsSold: Long,
    totalWeightKg: Double,
    ratePerKg: Double,
    requestedSaleEpochDay: Long? = null,
    buyerPhone: String = "",
    remarks: String = "",
  ) {
    requireAdminOrSuperAdmin {
      val saleDay = resolveEffectiveEpochDay(requestedSaleEpochDay)
      runMutation("Recorded sale of $birdsSold birds for ${batch.batchCode}") {
        repository.addBirdSale(
          batch = batch,
          buyerName = buyerName,
          vehicleNo = vehicleNo,
          challanNo = challanNo,
          birdsSold = birdsSold,
          totalWeightKg = totalWeightKg,
          ratePerKg = ratePerKg,
          saleEpochDay = saleDay,
          buyerPhone = buyerPhone,
          remarks = remarks,
        ).getOrThrow()
      }
    }
  }

  fun updateBirdSale(
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
    requestedEpochDay: Long? = null,
  ) {
    requireAdminOrSuperAdmin {
      val effectiveDay = if (currentOperator()?.canSelectPreviousDate == true && requestedEpochDay != null) {
        requestedEpochDay
      } else {
        sale.saleEpochDay
      }
      runMutation("Updated bird sale ${sale.challanNo}") {
        repository.updateBirdSale(
          sale = sale,
          batch = batch,
          buyerName = buyerName,
          vehicleNo = vehicleNo,
          challanNo = challanNo,
          birdsSold = birdsSold,
          totalWeightKg = totalWeightKg,
          ratePerKg = ratePerKg,
          buyerPhone = buyerPhone,
          remarks = remarks,
          saleEpochDay = effectiveDay,
        ).getOrThrow()
      }
    }
  }

  fun deleteBirdSale(sale: BirdSale, batch: FlockBatch?) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted bird sale ${sale.challanNo}") {
        repository.deleteBirdSale(sale, batch).getOrThrow()
      }
    }
  }

  fun addInterFeedTransfer(
    fromBatch: FlockBatch,
    toBatch: FlockBatch,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    transferRef: String,
    remarks: String,
    requestedTransferEpochDay: Long? = null,
  ) {
    val transferDay = resolveEffectiveEpochDay(requestedTransferEpochDay)
    runMutation("Transferred $bagsCount bags from ${fromBatch.batchCode} to ${toBatch.batchCode}") {
      repository.addInterFeedTransfer(
        fromBatch = fromBatch,
        toBatch = toBatch,
        feedType = feedType,
        bagsCount = bagsCount,
        weightKg = weightKg,
        transferRef = transferRef,
        remarks = remarks,
        transferEpochDay = transferDay,
      ).getOrThrow()
    }
  }

  fun updateFeedTransfer(
    transfer: FeedTransfer,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    transferRef: String,
    remarks: String,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Updated feed transfer ${transferRef.uppercase()}") {
        repository.updateFeedTransfer(transfer, feedType, bagsCount, weightKg, transferRef, remarks).getOrThrow()
      }
    }
  }

  fun deleteFeedTransfer(transfer: FeedTransfer) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted feed transfer ${transfer.transferRef}") {
        repository.deleteFeedTransfer(transfer.id).getOrThrow()
      }
    }
  }

  fun addDailyKmEntry(
    requestedEpochDay: Long?,
    startKm: Double,
    endKm: Double,
    visitedFarms: String,
  ) {
    val op = currentOperator() ?: return
    val entryDay = resolveEffectiveEpochDay(requestedEpochDay)
    val dist = (endKm - startKm).coerceAtLeast(0.0)
    runMutation("Saved daily KM entry (${dist.toInt()} km)") {
      repository.createDailyKmEntry(
        operatorId = op.id,
        empCode = op.empCode,
        operatorName = op.fullName,
        entryEpochDay = entryDay,
        startKm = startKm,
        endKm = endKm,
        visitedFarms = visitedFarms,
      ).getOrThrow()
    }
  }

  fun updateDailyKmEntry(
    entry: KmDailyEntry,
    startKm: Double,
    endKm: Double,
    visitedFarms: String,
    requestedEpochDay: Long?,
  ) {
    requireAdminOrSuperAdmin {
      val effectiveDay = if (currentOperator()?.canSelectPreviousDate == true && requestedEpochDay != null) {
        requestedEpochDay
      } else {
        entry.entryEpochDay
      }
      runMutation("Updated daily KM entry for ${entry.empCode}") {
        repository.updateDailyKmEntry(entry, startKm, endKm, visitedFarms, effectiveDay).getOrThrow()
      }
    }
  }

  fun deleteDailyKmEntry(entry: KmDailyEntry) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted daily KM entry (${entry.empCode})") {
        repository.deleteDailyKmEntry(entry.id).getOrThrow()
      }
    }
  }

  fun submitMonthlyKmClaim(
    monthKey: String,
    ratePerKm: Double,
    remarks: String,
  ) {
    val op = currentOperator() ?: return
    val ready = dataState.value as? BroilerDataState.Ready ?: return
    val monthEntries = ready.kmEntries.filter { entry ->
      (entry.operatorId == op.id || entry.empCode.equals(op.empCode, ignoreCase = true)) &&
        entry.resolvedMonthKey == monthKey
    }
    if (monthEntries.isEmpty()) {
      _controls.update {
        it.copy(snackbarMessage = "Log at least one daily KM entry in ${KmMonthHelper.monthLabelFromKey(monthKey)} before submitting the monthly claim.")
      }
      return
    }
    val alreadyClaimed = ready.kmClaims.any { claim ->
      (claim.operatorId == op.id || claim.empCode.equals(op.empCode, ignoreCase = true)) &&
        claim.resolvedMonthKey == monthKey
    }
    if (alreadyClaimed) {
      _controls.update {
        it.copy(snackbarMessage = "Monthly claim for ${KmMonthHelper.monthLabelFromKey(monthKey)} is already submitted (monthly once only).")
      }
      return
    }
    val ordered = monthEntries.sortedBy { it.entryEpochDay }
    val totalKm = ordered.sumOf { it.totalKm }
    val firstStart = ordered.first().startKm
    val lastEnd = ordered.last().endKm
    val routeSummary = remarks.trim().ifBlank {
      ordered.map { it.visitedFarms.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
        .take(280)
    }
    val monthLabel = KmMonthHelper.monthLabelFromKey(monthKey)
    runMutation("Submitted $monthLabel monthly KM claim (${totalKm.toInt()} km) for approval") {
      repository.submitMonthlyKmClaim(
        operatorId = op.id,
        empCode = op.empCode,
        operatorName = op.fullName,
        claimMonthKey = monthKey,
        claimEpochDay = todayEpochDay,
        dailyEntriesCount = ordered.size.toLong(),
        startKm = firstStart,
        endKm = lastEnd,
        totalKm = totalKm,
        ratePerKm = ratePerKm,
        visitedFarms = routeSummary,
      ).getOrThrow()
    }
  }

  fun updateMonthlyKmClaim(
    claim: KmClaim,
    totalKm: Double,
    ratePerKm: Double,
    visitedFarms: String,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Updated ${claim.resolvedMonthLabel} KM claim for ${claim.empCode}") {
        repository.updateMonthlyKmClaim(claim, totalKm, ratePerKm, visitedFarms).getOrThrow()
      }
    }
  }

  fun reviewKmClaim(
    claim: KmClaim,
    approved: Boolean,
    reviewRemarks: String,
  ) {
    requireAdminOrSuperAdmin {
      val op = currentOperator() ?: return@requireAdminOrSuperAdmin
      val nextStatus = if (approved) ClaimStatus.APPROVED else ClaimStatus.REJECTED
      runMutation("${claim.empCode} KM claim ${nextStatus.label}") {
        repository.reviewKmClaim(
          claimId = claim.id,
          status = nextStatus,
          reviewedByEmpCode = op.empCode,
          reviewedByName = op.fullName,
          reviewRemarks = reviewRemarks,
        ).getOrThrow()
      }
    }
  }

  fun deleteKmClaim(claim: KmClaim) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted ${claim.resolvedMonthLabel} KM claim (${claim.empCode})") {
        repository.deleteKmClaim(claim.id).getOrThrow()
      }
    }
  }

  fun addMedicineEntry(
    batch: FlockBatch,
    medicineName: String,
    category: String,
    dosageOrQty: String,
    totalCost: Double,
    invoiceOrSlipNo: String,
    remarks: String,
    requestedEpochDay: Long? = null,
  ) {
    requireAdminOrSuperAdmin {
      val op = currentOperator() ?: return@requireAdminOrSuperAdmin
      val entryDay = resolveEffectiveEpochDay(requestedEpochDay)
      runMutation("Added medicine entry ($medicineName) for ${batch.batchCode}") {
        repository.addMedicineEntry(
          batch = batch,
          medicineName = medicineName,
          category = category,
          dosageOrQty = dosageOrQty,
          totalCost = totalCost,
          invoiceOrSlipNo = invoiceOrSlipNo,
          remarks = remarks,
          entryEpochDay = entryDay,
          enteredByEmpCode = op.empCode,
          enteredByName = op.fullName,
        ).getOrThrow()
      }
    }
  }

  fun updateMedicineEntry(
    entry: MedicineEntry,
    medicineName: String,
    category: String,
    dosageOrQty: String,
    totalCost: Double,
    invoiceOrSlipNo: String,
    remarks: String,
    requestedEpochDay: Long?,
  ) {
    requireAdminOrSuperAdmin {
      val effectiveDay = if (currentOperator()?.canSelectPreviousDate == true && requestedEpochDay != null) {
        requestedEpochDay
      } else {
        entry.entryEpochDay
      }
      runMutation("Updated medicine entry ${entry.invoiceOrSlipNo}") {
        repository.updateMedicineEntry(
          entry = entry,
          medicineName = medicineName,
          category = category,
          dosageOrQty = dosageOrQty,
          totalCost = totalCost,
          invoiceOrSlipNo = invoiceOrSlipNo,
          remarks = remarks,
          entryEpochDay = effectiveDay,
        ).getOrThrow()
      }
    }
  }

  fun deleteMedicineEntry(entry: MedicineEntry) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted medicine entry ${entry.medicineName}") {
        repository.deleteMedicineEntry(entry.id).getOrThrow()
      }
    }
  }

  fun createMedicineCatalogItem(
    medicineCode: String,
    medicineName: String,
    category: String,
    packing: Double,
    unit: String,
    rate: Double,
    perKgOrLtr: Double,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Added $medicineCode ($medicineName) to medicine list") {
        repository.createMedicineCatalogItem(
          medicineCode = medicineCode,
          medicineName = medicineName,
          category = category,
          packing = packing,
          unit = unit,
          rate = rate,
          perKgOrLtr = perKgOrLtr,
        ).getOrThrow()
      }
    }
  }

  fun updateMedicineCatalogItem(
    item: MedicineCatalogItem,
    medicineCode: String,
    medicineName: String,
    category: String,
    packing: Double,
    unit: String,
    rate: Double,
    perKgOrLtr: Double,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("Updated medicine $medicineCode") {
        repository.updateMedicineCatalogItem(
          itemId = item.id,
          medicineCode = medicineCode,
          medicineName = medicineName,
          category = category,
          packing = packing,
          unit = unit,
          rate = rate,
          perKgOrLtr = perKgOrLtr,
        ).getOrThrow()
      }
    }
  }

  fun deleteMedicineCatalogItem(item: MedicineCatalogItem) {
    requireAdminOrSuperAdmin {
      runMutation("Deleted ${item.medicineCode} (${item.medicineName})") {
        repository.deleteMedicineCatalogItem(item.id).getOrThrow()
      }
    }
  }

  fun startClosingBatch(
    batch: FlockBatch,
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    closingNotes: String,
    gcDetails: GcStatementDetails,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("${batch.batchCode} moved to Closing Farm") {
        repository.startClosingBatch(
          batch = batch,
          liftedBirds = liftedBirds,
          liftedWeightKg = liftedWeightKg,
          returnedFeedKg = returnedFeedKg,
          closingNotes = closingNotes,
          gcDetails = gcDetails,
          todayEpochDay = todayEpochDay,
        ).getOrThrow()
      }
    }
  }

  fun closeBatch(
    batch: FlockBatch,
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    closingNotes: String,
    gcDetails: GcStatementDetails,
  ) {
    requireAdminOrSuperAdmin {
      runMutation("${batch.batchCode} GC Closed — ${batch.farmCode} is now Empty Farm") {
        repository.closeBatch(
          batch = batch,
          liftedBirds = liftedBirds,
          liftedWeightKg = liftedWeightKg,
          returnedFeedKg = returnedFeedKg,
          closingNotes = closingNotes,
          closedEpochDay = todayEpochDay,
          gcDetails = gcDetails,
        ).getOrThrow()
      }
    }
  }

  private fun runMutation(successMessage: String, block: suspend () -> Unit) {
    viewModelScope.launch {
      _controls.update { it.copy(isSaving = true) }
      try {
        block()
        _controls.update {
          it.copy(
            isSaving = false,
            snackbarMessage = it.snackbarMessage ?: successMessage,
          )
        }
      } catch (e: CancellationException) {
        _controls.update { it.copy(isSaving = false) }
        throw e
      } catch (e: Exception) {
        _controls.update {
          it.copy(
            isSaving = false,
            snackbarMessage = e.localizedMessage ?: "Operation failed",
          )
        }
      }
    }
  }

  private companion object {
    const val TAG = "BroilerVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
    const val MILLIS_PER_DAY = 86_400_000L
    const val CATALOG_PREFS_NAME = "spf_catalog_sync"
    const val CATALOG_SYNC_KEY_PREFIX = "master_catalogs_v5_synced"
  }
}
