package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.DailyLog
import com.example.data.model.FarmLedgerCalculator
import com.example.data.model.FarmStatus
import com.example.data.model.Farmer
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedTransfer
import com.example.data.model.FlockBatch
import com.example.data.model.IntegrationZone
import com.example.data.model.KmClaim
import com.example.data.model.KmClaimReportCalculator
import com.example.data.model.KmDailyEntry
import com.example.data.model.MedicineEntry
import com.example.data.model.OperatorRole
import com.example.data.repository.BroilerRepository
import com.example.pdf.FarmLedgerPdfGenerator
import com.example.pdf.GcStatementPdfGenerator
import com.example.pdf.KmClaimPdfGenerator
import com.example.pdf.PdfFileHelper
import com.example.pdf.ZoneDailyReportPdfGenerator
import com.example.ui.auth.OperatorLoginScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.BatchClosingDialog
import com.example.ui.components.BirdSaleDialog
import com.example.ui.components.CancelFarmDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateBatchDialog
import com.example.ui.components.DailyKmEntryDialog
import com.example.ui.components.DailyLogDialog
import com.example.ui.components.EditMonthlyKmClaimDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FarmerFormDialog
import com.example.ui.components.FeedDispatchDialog
import com.example.ui.components.FeedTransferDialog
import com.example.ui.components.MedicineEntryDialog
import com.example.ui.components.ReviewKmClaimDialog
import com.example.ui.components.ZoneFormDialog
import com.example.ui.screens.BatchesScreen
import com.example.ui.screens.FarmsScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SettlementHubTab
import com.example.ui.screens.SettlementsScreen
import com.example.ui.screens.StaffScreen
import com.example.ui.screens.UserDailyEntryScreen
import com.example.ui.screens.UserFeedDispatchScreen
import com.example.ui.screens.UserFeedTransferScreen
import com.example.ui.screens.UserKmClaimScreen
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.BroilerDataState
import com.example.ui.viewmodel.BroilerViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private sealed interface DeleteTarget {
  data class ZoneTarget(val zone: IntegrationZone) : DeleteTarget
  data class FarmerTarget(val farmer: Farmer) : DeleteTarget
  data class BatchTarget(val batch: FlockBatch) : DeleteTarget
  data class DailyLogTarget(val log: DailyLog, val batch: FlockBatch?) : DeleteTarget
  data class FeedDispatchTarget(val dispatch: FeedDispatch) : DeleteTarget
  data class BirdSaleTarget(val sale: BirdSale, val batch: FlockBatch?) : DeleteTarget
  data class FeedTransferTarget(val transfer: FeedTransfer) : DeleteTarget
  data class MedicineEntryTarget(val entry: MedicineEntry) : DeleteTarget
  data class KmDailyEntryTarget(val entry: KmDailyEntry) : DeleteTarget
  data class KmClaimTarget(val claim: KmClaim) : DeleteTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroilerApp(
  currentUser: FirebaseUser,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: BroilerViewModel = viewModel(
    key = currentUser.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        BroilerViewModel(
          repository = BroilerRepository(db),
          currentUserId = currentUser.uid,
        )
      }
    },
  )

  val dataState by viewModel.dataState.collectAsStateWithLifecycle()
  val controls by viewModel.controls.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var settlementTab by remember { mutableStateOf(SettlementHubTab.GC_CLOSED) }

  // Zone & Farmer dialogs
  var showCreateZoneDialog by remember { mutableStateOf(false) }
  var editingZone by remember { mutableStateOf<IntegrationZone?>(null) }
  var showCreateFarmerDialog by remember { mutableStateOf(false) }
  var editingFarmer by remember { mutableStateOf<Farmer?>(null) }
  var cancellingFarmer by remember { mutableStateOf<Farmer?>(null) }

  // Batch & Ledger dialogs
  var createBatchPreselectedFarmer by remember { mutableStateOf<Farmer?>(null) }
  var showCreateBatchDialog by remember { mutableStateOf(false) }
  var editingBatch by remember { mutableStateOf<FlockBatch?>(null) }

  var dailyLogBatch by remember { mutableStateOf<FlockBatch?>(null) }
  var editingDailyLogPair by remember { mutableStateOf<Pair<FlockBatch, DailyLog>?>(null) }

  var showFeedDispatchDialog by remember { mutableStateOf(false) }
  var feedDispatchPreselectedBatch by remember { mutableStateOf<FlockBatch?>(null) }
  var editingFeedDispatch by remember { mutableStateOf<FeedDispatch?>(null) }

  var showBirdSaleDialog by remember { mutableStateOf(false) }
  var birdSalePreselectedBatch by remember { mutableStateOf<FlockBatch?>(null) }
  var editingBirdSale by remember { mutableStateOf<BirdSale?>(null) }

  var showFeedTransferDialog by remember { mutableStateOf(false) }
  var feedTransferPreselectedBatch by remember { mutableStateOf<FlockBatch?>(null) }
  var editingFeedTransfer by remember { mutableStateOf<FeedTransfer?>(null) }

  var showMedicineEntryDialog by remember { mutableStateOf(false) }
  var medicineEntryPreselectedBatch by remember { mutableStateOf<FlockBatch?>(null) }
  var editingMedicineEntry by remember { mutableStateOf<MedicineEntry?>(null) }

  var closingBatch by remember { mutableStateOf<FlockBatch?>(null) }

  // KM Daily Entry & Monthly Claim dialogs
  var showDailyKmDialog by remember { mutableStateOf(false) }
  var editingDailyKmEntry by remember { mutableStateOf<KmDailyEntry?>(null) }
  var editingMonthlyKmClaim by remember { mutableStateOf<KmClaim?>(null) }
  var reviewingKmClaimPair by remember { mutableStateOf<Pair<KmClaim, Boolean>?>(null) }

  // Delete confirmation dialog
  var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

  // Pending PDF for CreateDocument fallback (< API 29)
  var pendingPdfForSaf by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
  val createPdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf"),
  ) { uri ->
    val payload = pendingPdfForSaf
    pendingPdfForSaf = null
    if (uri != null && payload != null) {
      if (PdfFileHelper.writeToUri(context, uri, payload.second)) {
        viewModel.showMessage("Saved ${payload.first}")
        PdfFileHelper.launchViewOrSharePdf(context, uri, payload.first)
      } else {
        viewModel.showMessage("Failed to write PDF file")
      }
    }
  }

  fun downloadPdf(fileName: String, bytes: ByteArray) {
    val savedUri = PdfFileHelper.saveToDownloadsIfSupported(context, fileName, bytes)
    if (savedUri != null) {
      viewModel.showMessage("Saved PDF to Downloads/SPF/$fileName")
      PdfFileHelper.launchViewOrSharePdf(context, savedUri, fileName)
    } else {
      pendingPdfForSaf = fileName to bytes
      createPdfLauncher.launch(fileName)
    }
  }

  fun sharePdf(fileName: String, bytes: ByteArray) {
    val cacheUri = PdfFileHelper.saveToCacheAndGetShareUri(context, fileName, bytes)
    PdfFileHelper.launchViewOrSharePdf(context, cacheUri, fileName, shareOnly = false)
  }

  LaunchedEffect(controls.snackbarMessage) {
    val msg = controls.snackbarMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.consumeSnackbar()
  }

  when (val state = dataState) {
    is BroilerDataState.Loading -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CircularProgressIndicator()
          Text(
            text = "Connecting SPF Poultry workspace...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    is BroilerDataState.Error -> {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        EmptyStateCard(
          icon = Icons.Rounded.Agriculture,
          title = "Unable to Sync Data",
          description = state.message,
          actionLabel = "Sign Out",
          onAction = { signOutUser(credentialManager, coroutineScope) },
        )
      }
    }

    is BroilerDataState.Ready -> {
      LaunchedEffect(currentUser.uid) {
        viewModel.syncMasterFarmersIfNeeded(context)
      }
      val selectedOperator = controls.activeOperator
      if (selectedOperator == null) {
        OperatorLoginScreen(
          workspaceEmail = currentUser.email ?: currentUser.displayName ?: "Cloud Workspace",
          operators = state.operators,
          isSaving = controls.isSaving,
          loginError = controls.loginError,
          onEmpCodeLogin = { role, code, password ->
            viewModel.loginWithEmpCode(state.operators, role, code, password)
          },
          onBootstrapSuperAdmin = { code, fullName, password ->
            viewModel.bootstrapSuperAdmin(code, fullName, password)
          },
          onClearError = { viewModel.clearLoginError() },
          onDisconnectGoogle = { signOutUser(credentialManager, coroutineScope) },
        )
        return
      }
      val activeOperator = state.operators.firstOrNull { it.id == selectedOperator.id } ?: selectedOperator

      val isSuperAdmin = activeOperator.roleEnum == OperatorRole.SUPER_ADMIN
      val isAdminOrAbove = activeOperator.roleEnum != OperatorRole.USER
      val canSelectPreviousDate = activeOperator.canSelectPreviousDate

      val visibleDestinations = when (activeOperator.roleEnum) {
        OperatorRole.USER -> listOf(
          AppDestination.DAILY_ENTRY,
          AppDestination.FEED_DISPATCH,
          AppDestination.FEED_TRANSFER,
          AppDestination.KM_CLAIMS,
        )
        OperatorRole.ADMIN -> listOf(
          AppDestination.OVERVIEW,
          AppDestination.FARMS,
          AppDestination.BATCHES,
          AppDestination.SETTLEMENTS,
        )
        OperatorRole.SUPER_ADMIN -> listOf(
          AppDestination.OVERVIEW,
          AppDestination.FARMS,
          AppDestination.BATCHES,
          AppDestination.SETTLEMENTS,
          AppDestination.STAFF,
        )
      }

      val activeDestination = if (controls.currentDestination in visibleDestinations) {
        controls.currentDestination
      } else {
        visibleDestinations.first()
      }

      fun requireAdmin(action: () -> Unit) {
        if (isAdminOrAbove) {
          action()
        } else {
          viewModel.showMessage("Only Super Admin and Admin can perform this action.")
        }
      }

      val openBatches = state.batches.filter { it.statusEnum != BatchStatus.CLOSED }
      val farmerById = state.farmers.associateBy { it.id }

      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            TopAppBar(
              title = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.img_spf_logo),
                    contentDescription = "SPF logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp),
                  )
                  Column {
                    Text(
                      text = "SPF",
                      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                      maxLines = 1,
                    )
                    Text(
                      text = "${activeOperator.empCode} • ${activeOperator.roleEnum.label} (${activeOperator.fullName})",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
              ),
              actions = {
                IconButton(
                  onClick = { viewModel.logoutOperator() },
                  modifier = Modifier.testTag("switch_operator_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Logout to Emp Code login",
                  )
                }
              },
            )
          },
          bottomBar = {
            if (!useNavigationRail) {
              NavigationBar {
                visibleDestinations.forEach { dest ->
                  NavigationBarItem(
                    selected = activeDestination == dest,
                    onClick = { viewModel.navigateTo(dest) },
                    icon = { Icon(dest.icon(), contentDescription = dest.label) },
                    label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav_${dest.name.lowercase()}"),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (useNavigationRail) {
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
              ) {
                Spacer(modifier = Modifier.size(12.dp))
                visibleDestinations.forEach { dest ->
                  NavigationRailItem(
                    selected = activeDestination == dest,
                    onClick = { viewModel.navigateTo(dest) },
                    icon = { Icon(dest.icon(), contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    modifier = Modifier.testTag("rail_${dest.name.lowercase()}"),
                  )
                }
              }
            }

            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
              if (controls.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              }

              when (activeDestination) {
                AppDestination.DAILY_ENTRY -> UserDailyEntryScreen(
                  farmers = state.farmers,
                  batches = state.batches,
                  dailyLogs = state.dailyLogs,
                  todayEpochDay = viewModel.todayEpochDay,
                  onLogDailyClick = { dailyLogBatch = it },
                )

                AppDestination.FEED_DISPATCH -> UserFeedDispatchScreen(
                  farmers = state.farmers,
                  batches = state.batches,
                  feedDispatches = state.feedDispatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  onAddFeedDispatchClick = {
                    feedDispatchPreselectedBatch = null
                    showFeedDispatchDialog = true
                  },
                )

                AppDestination.FEED_TRANSFER -> UserFeedTransferScreen(
                  batches = state.batches,
                  feedTransfers = state.feedTransfers,
                  onAddFeedTransferClick = {
                    feedTransferPreselectedBatch = null
                    showFeedTransferDialog = true
                  },
                )

                AppDestination.KM_CLAIMS -> UserKmClaimScreen(
                  activeOperator = activeOperator,
                  kmEntries = state.kmEntries,
                  kmClaims = state.kmClaims,
                  todayEpochDay = viewModel.todayEpochDay,
                  onAddDailyKmClick = { showDailyKmDialog = true },
                  onSubmitMonthlyClaim = { monthKey, ratePerKm, remarks ->
                    viewModel.submitMonthlyKmClaim(monthKey, ratePerKm, remarks)
                  },
                  onDownloadKmClaimPdf = { claim ->
                    val report = KmClaimReportCalculator.buildClaimReport(claim, state.kmEntries)
                    val bytes = KmClaimPdfGenerator.generateClaimDetailPdfBytes(context, report, viewModel.todayEpochDay)
                    val cleanEmp = claim.empCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    downloadPdf("SPF_KM_Claim_${cleanEmp}_${claim.resolvedMonthKey}.pdf", bytes)
                  },
                  onShareKmClaimPdf = { claim ->
                    val report = KmClaimReportCalculator.buildClaimReport(claim, state.kmEntries)
                    val bytes = KmClaimPdfGenerator.generateClaimDetailPdfBytes(context, report, viewModel.todayEpochDay)
                    val cleanEmp = claim.empCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    sharePdf("SPF_KM_Claim_${cleanEmp}_${claim.resolvedMonthKey}.pdf", bytes)
                  },
                )

                AppDestination.OVERVIEW -> OverviewScreen(
                  zones = state.zones,
                  farmers = state.farmers,
                  batches = state.batches,
                  feedDispatches = state.feedDispatches,
                  kmClaims = state.kmClaims,
                  todayEpochDay = viewModel.todayEpochDay,
                  onOpenFarmsFilter = { viewModel.openFarmsWithFilter(it) },
                  onOpenBatchDetail = { viewModel.openBatchDetail(it) },
                  onCreateZoneClick = { requireAdmin { showCreateZoneDialog = true } },
                  onCreateFarmerClick = { requireAdmin { showCreateFarmerDialog = true } },
                  onCreateBatchClick = {
                    requireAdmin {
                      createBatchPreselectedFarmer = null
                      showCreateBatchDialog = true
                    }
                  },
                  onOpenZoneReport = {
                    settlementTab = SettlementHubTab.ZONE_REPORT
                    viewModel.navigateTo(AppDestination.SETTLEMENTS)
                  },
                  onOpenKmClaims = {
                    settlementTab = SettlementHubTab.KM_CLAIMS
                    viewModel.navigateTo(AppDestination.SETTLEMENTS)
                  },
                  onFeedDispatchClick = {
                    feedDispatchPreselectedBatch = null
                    showFeedDispatchDialog = true
                  },
                  onBirdSaleClick = {
                    requireAdmin {
                      birdSalePreselectedBatch = null
                      showBirdSaleDialog = true
                    }
                  },
                )

                AppDestination.FARMS -> FarmsScreen(
                  zones = state.zones,
                  farmers = state.farmers,
                  batches = state.batches,
                  selectedFilter = controls.farmFilter,
                  selectedZoneFilterId = controls.selectedZoneFilterId,
                  searchQuery = controls.farmSearchQuery,
                  selectedFarmerId = controls.selectedFarmerId,
                  canEditOrDelete = isAdminOrAbove,
                  onFilterChange = { viewModel.setFarmFilter(it) },
                  onZoneFilterChange = { viewModel.setZoneFilter(it) },
                  onSearchChange = { viewModel.setFarmSearchQuery(it) },
                  onSelectFarmer = { viewModel.selectFarmer(it) },
                  onCreateZoneClick = { requireAdmin { showCreateZoneDialog = true } },
                  onEditZoneClick = { zone -> requireAdmin { editingZone = zone } },
                  onDeleteZoneClick = { zone -> requireAdmin { deleteTarget = DeleteTarget.ZoneTarget(zone) } },
                  onCreateFarmerClick = { requireAdmin { showCreateFarmerDialog = true } },
                  onEditFarmerClick = { farmer -> requireAdmin { editingFarmer = farmer } },
                  onDeleteFarmerClick = { farmer -> requireAdmin { deleteTarget = DeleteTarget.FarmerTarget(farmer) } },
                  onCreateBatchForFarmerClick = { farmer ->
                    requireAdmin {
                      createBatchPreselectedFarmer = farmer
                      showCreateBatchDialog = true
                    }
                  },
                  onOpenLinkedBatch = { batchId -> viewModel.openBatchDetail(batchId) },
                  onMarkClosingFarm = { farmer ->
                    requireAdmin { viewModel.markFarmStatus(farmer.id, FarmStatus.CLOSING) }
                  },
                  onMarkEmptyFarm = { farmer ->
                    requireAdmin { viewModel.markFarmStatus(farmer.id, FarmStatus.EMPTY) }
                  },
                  onRequestCancelFarm = { farmer ->
                    requireAdmin { cancellingFarmer = farmer }
                  },
                  onReactivateFarm = { farmer ->
                    requireAdmin { viewModel.markFarmStatus(farmer.id, FarmStatus.EMPTY) }
                  },
                  onDownloadFarmLedgerPdf = { farmer ->
                    val ledger = FarmLedgerCalculator.build(
                      farmer = farmer,
                      allBatches = state.batches,
                      dailyLogs = state.dailyLogs,
                      feedDispatches = state.feedDispatches,
                      medicineEntries = state.medicineEntries,
                      feedTransfers = state.feedTransfers,
                      birdSales = state.birdSales,
                    )
                    val bytes = FarmLedgerPdfGenerator.generatePdfBytes(context, ledger, viewModel.todayEpochDay)
                    val cleanCode = farmer.farmCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    downloadPdf("SPF_Farm_Ledger_${cleanCode}.pdf", bytes)
                  },
                  onShareFarmLedgerPdf = { farmer ->
                    val ledger = FarmLedgerCalculator.build(
                      farmer = farmer,
                      allBatches = state.batches,
                      dailyLogs = state.dailyLogs,
                      feedDispatches = state.feedDispatches,
                      medicineEntries = state.medicineEntries,
                      feedTransfers = state.feedTransfers,
                      birdSales = state.birdSales,
                    )
                    val bytes = FarmLedgerPdfGenerator.generatePdfBytes(context, ledger, viewModel.todayEpochDay)
                    val cleanCode = farmer.farmCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    sharePdf("SPF_Farm_Ledger_${cleanCode}.pdf", bytes)
                  },
                  onSyncMasterFarmersClick = {
                    viewModel.syncMasterFarmersNow(context)
                  },
                )

                AppDestination.BATCHES -> BatchesScreen(
                  batches = state.batches,
                  dailyLogs = state.dailyLogs,
                  feedDispatches = state.feedDispatches,
                  birdSales = state.birdSales,
                  feedTransfers = state.feedTransfers,
                  medicineEntries = state.medicineEntries,
                  todayEpochDay = viewModel.todayEpochDay,
                  selectedFilter = controls.batchFilter,
                  searchQuery = controls.batchSearchQuery,
                  selectedBatchId = controls.selectedBatchId,
                  canEditOrDelete = isAdminOrAbove,
                  onFilterChange = { viewModel.setBatchFilter(it) },
                  onSearchChange = { viewModel.setBatchSearchQuery(it) },
                  onSelectBatch = { viewModel.selectBatch(it) },
                  onSyncMasterBatchesClick = { viewModel.syncMasterBatchesNow(context) },
                  onCreateBatchClick = {
                    requireAdmin {
                      createBatchPreselectedFarmer = null
                      showCreateBatchDialog = true
                    }
                  },
                  onEditBatchClick = { batch -> requireAdmin { editingBatch = batch } },
                  onDeleteBatchClick = { batch -> requireAdmin { deleteTarget = DeleteTarget.BatchTarget(batch) } },
                  onLogDailyClick = { dailyLogBatch = it },
                  onEditDailyLogClick = { batch, log -> requireAdmin { editingDailyLogPair = batch to log } },
                  onDeleteDailyLogClick = { batch, log -> requireAdmin { deleteTarget = DeleteTarget.DailyLogTarget(log, batch) } },
                  onIssueFeedClick = { batch ->
                    feedDispatchPreselectedBatch = batch
                    showFeedDispatchDialog = true
                  },
                  onEditFeedDispatchClick = { d -> requireAdmin { editingFeedDispatch = d } },
                  onDeleteFeedDispatchClick = { d -> requireAdmin { deleteTarget = DeleteTarget.FeedDispatchTarget(d) } },
                  onMedicineEntryClick = { batch ->
                    requireAdmin {
                      medicineEntryPreselectedBatch = batch
                      showMedicineEntryDialog = true
                    }
                  },
                  onEditMedicineEntryClick = { m -> requireAdmin { editingMedicineEntry = m } },
                  onDeleteMedicineEntryClick = { m -> requireAdmin { deleteTarget = DeleteTarget.MedicineEntryTarget(m) } },
                  onBirdSaleClick = { batch ->
                    requireAdmin {
                      birdSalePreselectedBatch = batch
                      showBirdSaleDialog = true
                    }
                  },
                  onEditBirdSaleClick = { sale -> requireAdmin { editingBirdSale = sale } },
                  onDeleteBirdSaleClick = { sale ->
                    requireAdmin {
                      val linked = state.batches.firstOrNull { it.id == sale.batchId }
                      deleteTarget = DeleteTarget.BirdSaleTarget(sale, linked)
                    }
                  },
                  onFeedTransferClick = { batch ->
                    feedTransferPreselectedBatch = batch
                    showFeedTransferDialog = true
                  },
                  onEditFeedTransferClick = { tr -> requireAdmin { editingFeedTransfer = tr } },
                  onDeleteFeedTransferClick = { tr -> requireAdmin { deleteTarget = DeleteTarget.FeedTransferTarget(tr) } },
                  onCloseBatchFlowClick = { batch -> requireAdmin { closingBatch = batch } },
                  onDownloadGcPdfClick = { batch ->
                    val bytes = GcStatementPdfGenerator.generatePdfBytes(
                      context = context,
                      batch = batch,
                      farmer = farmerById[batch.farmerId],
                      todayEpochDay = viewModel.todayEpochDay,
                    )
                    downloadPdf("SPF_GC_${batch.batchCode}.pdf", bytes)
                  },
                  onShareGcPdfClick = { batch ->
                    val bytes = GcStatementPdfGenerator.generatePdfBytes(
                      context = context,
                      batch = batch,
                      farmer = farmerById[batch.farmerId],
                      todayEpochDay = viewModel.todayEpochDay,
                    )
                    sharePdf("SPF_GC_${batch.batchCode}.pdf", bytes)
                  },
                  onDownloadBatchLedgerPdfClick = { batch ->
                    val farmer = farmerById[batch.farmerId] ?: Farmer(
                      id = batch.farmerId,
                      farmCode = batch.farmCode,
                      farmerName = batch.farmerName,
                    )
                    val ledger = FarmLedgerCalculator.build(
                      farmer = farmer,
                      allBatches = state.batches,
                      dailyLogs = state.dailyLogs,
                      feedDispatches = state.feedDispatches,
                      medicineEntries = state.medicineEntries,
                      feedTransfers = state.feedTransfers,
                      birdSales = state.birdSales,
                      specificBatch = batch,
                    )
                    val bytes = FarmLedgerPdfGenerator.generatePdfBytes(context, ledger, viewModel.todayEpochDay)
                    downloadPdf("SPF_Ledger_${batch.farmCode}_${batch.batchCode}.pdf", bytes)
                  },
                  onShareBatchLedgerPdfClick = { batch ->
                    val farmer = farmerById[batch.farmerId] ?: Farmer(
                      id = batch.farmerId,
                      farmCode = batch.farmCode,
                      farmerName = batch.farmerName,
                    )
                    val ledger = FarmLedgerCalculator.build(
                      farmer = farmer,
                      allBatches = state.batches,
                      dailyLogs = state.dailyLogs,
                      feedDispatches = state.feedDispatches,
                      medicineEntries = state.medicineEntries,
                      feedTransfers = state.feedTransfers,
                      birdSales = state.birdSales,
                      specificBatch = batch,
                    )
                    val bytes = FarmLedgerPdfGenerator.generatePdfBytes(context, ledger, viewModel.todayEpochDay)
                    sharePdf("SPF_Ledger_${batch.farmCode}_${batch.batchCode}.pdf", bytes)
                  },
                )

                AppDestination.SETTLEMENTS -> SettlementsScreen(
                  zones = state.zones,
                  farmers = state.farmers,
                  batches = state.batches,
                  dailyLogs = state.dailyLogs,
                  feedDispatches = state.feedDispatches,
                  birdSales = state.birdSales,
                  feedTransfers = state.feedTransfers,
                  medicineEntries = state.medicineEntries,
                  kmEntries = state.kmEntries,
                  kmClaims = state.kmClaims,
                  todayEpochDay = viewModel.todayEpochDay,
                  canEditOrDelete = isAdminOrAbove,
                  canSelectPreviousDate = canSelectPreviousDate,
                  selectedTab = settlementTab,
                  onTabChange = { settlementTab = it },
                  onOpenBatch = { viewModel.openBatchDetail(it) },
                  onOpenClosingDialog = { batch -> requireAdmin { closingBatch = batch } },
                  onDownloadGcPdf = { batch ->
                    val bytes = GcStatementPdfGenerator.generatePdfBytes(
                      context = context,
                      batch = batch,
                      farmer = farmerById[batch.farmerId],
                      todayEpochDay = viewModel.todayEpochDay,
                    )
                    downloadPdf("SPF_GC_${batch.batchCode}.pdf", bytes)
                  },
                  onShareGcPdf = { batch ->
                    val bytes = GcStatementPdfGenerator.generatePdfBytes(
                      context = context,
                      batch = batch,
                      farmer = farmerById[batch.farmerId],
                      todayEpochDay = viewModel.todayEpochDay,
                    )
                    sharePdf("SPF_GC_${batch.batchCode}.pdf", bytes)
                  },
                  onDownloadZonePdf = { summary ->
                    val bytes = ZoneDailyReportPdfGenerator.generatePdfBytes(summary)
                    val cleanZone = summary.zoneName.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    downloadPdf("SPF_${cleanZone}_Daily_${summary.reportEpochDay}.pdf", bytes)
                  },
                  onShareZonePdf = { summary ->
                    val bytes = ZoneDailyReportPdfGenerator.generatePdfBytes(summary)
                    val cleanZone = summary.zoneName.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    sharePdf("SPF_${cleanZone}_Daily_${summary.reportEpochDay}.pdf", bytes)
                  },
                  onDownloadFarmLedgerPdf = { ledger ->
                    val bytes = FarmLedgerPdfGenerator.generatePdfBytes(context, ledger, viewModel.todayEpochDay)
                    val cleanCode = ledger.farmer.farmCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val scopeSuffix = ledger.selectedBatch?.batchCode?.let { "_$it" } ?: ""
                    downloadPdf("SPF_Farm_Ledger_${cleanCode}${scopeSuffix}.pdf", bytes)
                  },
                  onShareFarmLedgerPdf = { ledger ->
                    val bytes = FarmLedgerPdfGenerator.generatePdfBytes(context, ledger, viewModel.todayEpochDay)
                    val cleanCode = ledger.farmer.farmCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val scopeSuffix = ledger.selectedBatch?.batchCode?.let { "_$it" } ?: ""
                    sharePdf("SPF_Farm_Ledger_${cleanCode}${scopeSuffix}.pdf", bytes)
                  },
                  onAddFeedDispatchClick = {
                    feedDispatchPreselectedBatch = null
                    showFeedDispatchDialog = true
                  },
                  onEditFeedDispatchClick = { d -> requireAdmin { editingFeedDispatch = d } },
                  onDeleteFeedDispatchClick = { d -> requireAdmin { deleteTarget = DeleteTarget.FeedDispatchTarget(d) } },
                  onAddMedicineEntryClick = {
                    requireAdmin {
                      medicineEntryPreselectedBatch = null
                      showMedicineEntryDialog = true
                    }
                  },
                  onEditMedicineEntryClick = { m -> requireAdmin { editingMedicineEntry = m } },
                  onDeleteMedicineEntryClick = { m -> requireAdmin { deleteTarget = DeleteTarget.MedicineEntryTarget(m) } },
                  onAddFeedTransferClick = {
                    feedTransferPreselectedBatch = null
                    showFeedTransferDialog = true
                  },
                  onEditFeedTransferClick = { tr -> requireAdmin { editingFeedTransfer = tr } },
                  onDeleteFeedTransferClick = { tr -> requireAdmin { deleteTarget = DeleteTarget.FeedTransferTarget(tr) } },
                  onAddBirdSaleClick = {
                    requireAdmin {
                      birdSalePreselectedBatch = null
                      showBirdSaleDialog = true
                    }
                  },
                  onEditBirdSaleClick = { sale -> requireAdmin { editingBirdSale = sale } },
                  onDeleteBirdSaleClick = { sale ->
                    requireAdmin {
                      val linked = state.batches.firstOrNull { it.id == sale.batchId }
                      deleteTarget = DeleteTarget.BirdSaleTarget(sale, linked)
                    }
                  },
                  onAddDailyKmClick = { showDailyKmDialog = true },
                  onEditDailyKmEntry = { entry -> requireAdmin { editingDailyKmEntry = entry } },
                  onDeleteDailyKmEntry = { entry -> requireAdmin { deleteTarget = DeleteTarget.KmDailyEntryTarget(entry) } },
                  onApproveKmClaim = { claim -> requireAdmin { reviewingKmClaimPair = claim to true } },
                  onRejectKmClaim = { claim -> requireAdmin { reviewingKmClaimPair = claim to false } },
                  onEditMonthlyKmClaim = { claim -> requireAdmin { editingMonthlyKmClaim = claim } },
                  onDeleteKmClaim = { claim -> requireAdmin { deleteTarget = DeleteTarget.KmClaimTarget(claim) } },
                  onDownloadKmClaimPdf = { claim ->
                    val report = KmClaimReportCalculator.buildClaimReport(claim, state.kmEntries)
                    val bytes = KmClaimPdfGenerator.generateClaimDetailPdfBytes(context, report, viewModel.todayEpochDay)
                    val cleanEmp = claim.empCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    downloadPdf("SPF_KM_Claim_${cleanEmp}_${claim.resolvedMonthKey}.pdf", bytes)
                  },
                  onShareKmClaimPdf = { claim ->
                    val report = KmClaimReportCalculator.buildClaimReport(claim, state.kmEntries)
                    val bytes = KmClaimPdfGenerator.generateClaimDetailPdfBytes(context, report, viewModel.todayEpochDay)
                    val cleanEmp = claim.empCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    sharePdf("SPF_KM_Claim_${cleanEmp}_${claim.resolvedMonthKey}.pdf", bytes)
                  },
                  onDownloadAllKmClaimsPdf = { claims, filterLabel ->
                    val reports = claims.map { KmClaimReportCalculator.buildClaimReport(it, state.kmEntries) }
                    val bytes = KmClaimPdfGenerator.generateAllClaimsPdfBytes(context, reports, filterLabel, viewModel.todayEpochDay)
                    val cleanFilter = filterLabel.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    downloadPdf("SPF_KM_Claims_Register_${cleanFilter}_${viewModel.todayEpochDay}.pdf", bytes)
                  },
                )

                AppDestination.STAFF -> if (isSuperAdmin) {
                  StaffScreen(
                    operators = state.operators,
                    currentOperatorId = activeOperator.id,
                    onCreateOperator = { code, name, role, password, allowPrev ->
                      viewModel.createOperator(code, name, role, password, allowPrev)
                    },
                    onUpdateOperator = { id, name, role, active, newPwd, allowPrev ->
                      viewModel.updateOperator(id, name, role, active, newPwd, allowPrev)
                    },
                    onDeleteOperator = { op -> viewModel.deleteOperator(op) },
                  )
                }
              }

              // --- Dialogs ---
              if (showCreateZoneDialog) {
                ZoneFormDialog(
                  onDismiss = { showCreateZoneDialog = false },
                  onSave = { code, name, region, sup ->
                    showCreateZoneDialog = false
                    viewModel.createZone(code, name, region, sup)
                  },
                )
              }

              editingZone?.let { zone ->
                ZoneFormDialog(
                  existingZone = zone,
                  onDismiss = { editingZone = null },
                  onSave = { code, name, region, sup ->
                    editingZone = null
                    viewModel.updateZone(zone.id, code, name, region, sup)
                  },
                )
              }

              if (showCreateFarmerDialog) {
                FarmerFormDialog(
                  zones = state.zones,
                  onDismiss = { showCreateFarmerDialog = false },
                  onSave = { code, name, farmName, phone, village, tehsil, region, farmType, zId, zName, pan, aadhaar, acc, bank, ifsc, accType, shedType, cap, sqft, rate ->
                    showCreateFarmerDialog = false
                    viewModel.createFarmer(code, name, farmName, phone, village, tehsil, region, farmType, zId, zName, pan, aadhaar, acc, bank, ifsc, accType, shedType, cap, sqft, rate)
                  },
                )
              }

              editingFarmer?.let { target ->
                FarmerFormDialog(
                  zones = state.zones,
                  existingFarmer = target,
                  onDismiss = { editingFarmer = null },
                  onSave = { code, name, farmName, phone, village, tehsil, region, farmType, zId, zName, pan, aadhaar, acc, bank, ifsc, accType, shedType, cap, sqft, rate ->
                    editingFarmer = null
                    viewModel.updateFarmerProfile(target.id, code, name, farmName, phone, village, tehsil, region, farmType, zId, zName, pan, aadhaar, acc, bank, ifsc, accType, shedType, cap, sqft, rate)
                  },
                )
              }

              cancellingFarmer?.let { target ->
                CancelFarmDialog(
                  farmer = target,
                  onDismiss = { cancellingFarmer = null },
                  onConfirmCancel = { reason ->
                    cancellingFarmer = null
                    viewModel.markFarmStatus(target.id, FarmStatus.CANCELLED, cancelReason = reason)
                  },
                )
              }

              if (showCreateBatchDialog) {
                val emptyFarms = state.farmers.filter { it.statusEnum == FarmStatus.EMPTY }
                CreateBatchDialog(
                  eligibleFarmers = emptyFarms,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  preselectedFarmer = createBatchPreselectedFarmer,
                  onDismiss = {
                    showCreateBatchDialog = false
                    createBatchPreselectedFarmer = null
                  },
                  onCreateBatch = { farmer, code, breed, chicks, free, requestedPlacementDay, age, wt, stdFcr, rate, region, zoneName, farmName, weekNumber, challanNo ->
                    showCreateBatchDialog = false
                    createBatchPreselectedFarmer = null
                    if (farmer != null) {
                      viewModel.createBatch(
                        farmer = farmer,
                        batchCode = code,
                        breed = breed,
                        chicksPlaced = chicks,
                        freeChicks = free,
                        requestedPlacementEpochDay = requestedPlacementDay,
                        targetMarketAgeDays = age,
                        targetAvgWeightKg = wt,
                        standardFcr = stdFcr,
                        growingRatePerKg = rate,
                        region = region,
                        zoneName = zoneName,
                        farmName = farmName,
                        weekNumber = weekNumber,
                        challanNo = challanNo,
                      )
                    }
                  },
                )
              }

              editingBatch?.let { target ->
                CreateBatchDialog(
                  eligibleFarmers = emptyList(),
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  existingBatch = target,
                  onDismiss = { editingBatch = null },
                  onCreateBatch = { _, code, breed, chicks, free, requestedPlacementDay, age, wt, stdFcr, rate, region, zoneName, farmName, weekNumber, challanNo ->
                    editingBatch = null
                    viewModel.updateBatch(
                      batchId = target.id,
                      batchCode = code,
                      breed = breed,
                      chicksPlaced = chicks,
                      freeChicks = free,
                      requestedPlacementEpochDay = requestedPlacementDay,
                      existingPlacementEpochDay = target.placementEpochDay,
                      targetMarketAgeDays = age,
                      targetAvgWeightKg = wt,
                      standardFcr = stdFcr,
                      growingRatePerKg = rate,
                      region = region,
                      zoneName = zoneName,
                      farmName = farmName,
                      weekNumber = weekNumber,
                      challanNo = challanNo,
                    )
                  },
                )
              }

              dailyLogBatch?.let { batch ->
                DailyLogDialog(
                  batch = batch,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  onDismiss = { dailyLogBatch = null },
                  onSaveLog = { reqDay, mort, culls, feedKg, wtG, temp, loose, feeder, notes ->
                    dailyLogBatch = null
                    viewModel.addDailyLog(batch, reqDay, mort, culls, feedKg, wtG, temp, loose, feeder, notes)
                  },
                )
              }

              editingDailyLogPair?.let { (batch, log) ->
                DailyLogDialog(
                  batch = batch,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = false,
                  existingLog = log,
                  onDismiss = { editingDailyLogPair = null },
                  onSaveLog = { _, mort, culls, feedKg, wtG, temp, loose, feeder, notes ->
                    editingDailyLogPair = null
                    viewModel.updateDailyLog(log, batch, mort, culls, feedKg, wtG, temp, loose, feeder, notes)
                  },
                )
              }

              if (showFeedDispatchDialog) {
                FeedDispatchDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  preselectedBatch = feedDispatchPreselectedBatch,
                  onDismiss = {
                    showFeedDispatchDialog = false
                    feedDispatchPreselectedBatch = null
                  },
                  onSaveDispatch = { batch, feedType, bags, weightKg, challan, vehicleNo, remarks, reqDay ->
                    showFeedDispatchDialog = false
                    feedDispatchPreselectedBatch = null
                    if (batch != null) {
                      viewModel.addFeedDispatch(batch, feedType, bags, weightKg, challan, reqDay, vehicleNo, remarks)
                    }
                  },
                )
              }

              editingFeedDispatch?.let { d ->
                FeedDispatchDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  existingDispatch = d,
                  onDismiss = { editingFeedDispatch = null },
                  onSaveDispatch = { _, feedType, bags, weightKg, challan, vehicleNo, remarks, reqDay ->
                    editingFeedDispatch = null
                    viewModel.updateFeedDispatch(d, feedType, bags, weightKg, challan, vehicleNo, remarks, reqDay)
                  },
                )
              }

              if (showBirdSaleDialog) {
                BirdSaleDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  preselectedBatch = birdSalePreselectedBatch,
                  onDismiss = {
                    showBirdSaleDialog = false
                    birdSalePreselectedBatch = null
                  },
                  onSaveBirdSale = { batch, buyer, buyerPhone, vehicle, challan, birds, wtKg, rate, remarks, reqDay ->
                    showBirdSaleDialog = false
                    birdSalePreselectedBatch = null
                    if (batch != null) {
                      viewModel.addBirdSale(batch, buyer, vehicle, challan, birds, wtKg, rate, reqDay, buyerPhone, remarks)
                    }
                  },
                )
              }

              editingBirdSale?.let { sale ->
                val linked = state.batches.firstOrNull { it.id == sale.batchId }
                BirdSaleDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  existingBatch = linked,
                  existingSale = sale,
                  onDismiss = { editingBirdSale = null },
                  onSaveBirdSale = { _, buyer, buyerPhone, vehicle, challan, birds, wtKg, rate, remarks, reqDay ->
                    editingBirdSale = null
                    viewModel.updateBirdSale(sale, linked, buyer, vehicle, challan, birds, wtKg, rate, buyerPhone, remarks, reqDay)
                  },
                )
              }

              if (showFeedTransferDialog) {
                FeedTransferDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  preselectedFromBatch = feedTransferPreselectedBatch,
                  onDismiss = {
                    showFeedTransferDialog = false
                    feedTransferPreselectedBatch = null
                  },
                  onSaveTransfer = { fromB, toB, feedType, bags, wtKg, ref, remarks, reqDay ->
                    showFeedTransferDialog = false
                    feedTransferPreselectedBatch = null
                    if (fromB != null && toB != null) {
                      viewModel.addInterFeedTransfer(fromB, toB, feedType, bags, wtKg, ref, remarks, reqDay)
                    }
                  },
                )
              }

              editingFeedTransfer?.let { tr ->
                FeedTransferDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = false,
                  existingTransfer = tr,
                  onDismiss = { editingFeedTransfer = null },
                  onSaveTransfer = { _, _, feedType, bags, wtKg, ref, remarks, _ ->
                    editingFeedTransfer = null
                    viewModel.updateFeedTransfer(tr, feedType, bags, wtKg, ref, remarks)
                  },
                )
              }

              if (showMedicineEntryDialog) {
                MedicineEntryDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  preselectedBatch = medicineEntryPreselectedBatch,
                  onDismiss = {
                    showMedicineEntryDialog = false
                    medicineEntryPreselectedBatch = null
                  },
                  onSaveMedicine = { batch, medName, category, dosage, cost, slipNo, remarks, reqDay ->
                    showMedicineEntryDialog = false
                    medicineEntryPreselectedBatch = null
                    if (batch != null) {
                      viewModel.addMedicineEntry(batch, medName, category, dosage, cost, slipNo, remarks, reqDay)
                    }
                  },
                )
              }

              editingMedicineEntry?.let { entry ->
                MedicineEntryDialog(
                  openBatches = openBatches,
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  existingEntry = entry,
                  onDismiss = { editingMedicineEntry = null },
                  onSaveMedicine = { _, medName, category, dosage, cost, slipNo, remarks, reqDay ->
                    editingMedicineEntry = null
                    viewModel.updateMedicineEntry(entry, medName, category, dosage, cost, slipNo, remarks, reqDay)
                  },
                )
              }

              closingBatch?.let { batch ->
                val recordedMedTotal = state.medicineEntries
                  .filter { it.batchId == batch.id }
                  .sumOf { it.totalCost }
                BatchClosingDialog(
                  batch = batch,
                  todayEpochDay = viewModel.todayEpochDay,
                  operatorName = activeOperator.fullName,
                  recordedMedicineTotal = recordedMedTotal,
                  onDismiss = { closingBatch = null },
                  onMarkClosing = { birds, wtKg, retFeed, notes, gcDetails ->
                    closingBatch = null
                    viewModel.startClosingBatch(batch, birds, wtKg, retFeed, notes, gcDetails)
                  },
                  onConfirmCloseBatch = { birds, wtKg, retFeed, notes, gcDetails ->
                    closingBatch = null
                    viewModel.closeBatch(batch, birds, wtKg, retFeed, notes, gcDetails)
                  },
                )
              }

              if (showDailyKmDialog) {
                DailyKmEntryDialog(
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  onDismiss = { showDailyKmDialog = false },
                  onSave = { reqDay, startKm, endKm, visited ->
                    showDailyKmDialog = false
                    viewModel.addDailyKmEntry(reqDay, startKm, endKm, visited)
                  },
                )
              }

              editingDailyKmEntry?.let { entry ->
                DailyKmEntryDialog(
                  todayEpochDay = viewModel.todayEpochDay,
                  canSelectPreviousDate = canSelectPreviousDate,
                  existingEntry = entry,
                  onDismiss = { editingDailyKmEntry = null },
                  onSave = { reqDay, startKm, endKm, visited ->
                    editingDailyKmEntry = null
                    viewModel.updateDailyKmEntry(entry, startKm, endKm, visited, reqDay)
                  },
                )
              }

              editingMonthlyKmClaim?.let { claim ->
                EditMonthlyKmClaimDialog(
                  claim = claim,
                  onDismiss = { editingMonthlyKmClaim = null },
                  onSave = { totalKm, rate, visited ->
                    editingMonthlyKmClaim = null
                    viewModel.updateMonthlyKmClaim(claim, totalKm, rate, visited)
                  },
                )
              }

              reviewingKmClaimPair?.let { (claim, approved) ->
                ReviewKmClaimDialog(
                  claim = claim,
                  approved = approved,
                  onDismiss = { reviewingKmClaimPair = null },
                  onConfirm = { remarks ->
                    reviewingKmClaimPair = null
                    viewModel.reviewKmClaim(claim, approved, remarks)
                  },
                )
              }

              deleteTarget?.let { target ->
                val (title, msg) = when (target) {
                  is DeleteTarget.ZoneTarget -> "Delete Zone" to "Remove zone ${target.zone.zoneName}?"
                  is DeleteTarget.FarmerTarget -> "Delete Farmer" to "Permanently delete ${target.farmer.farmCode} (${target.farmer.farmerName})?"
                  is DeleteTarget.BatchTarget -> "Delete Batch" to "Delete batch ${target.batch.batchCode} and mark ${target.batch.farmCode} as Empty Farm?"
                  is DeleteTarget.DailyLogTarget -> "Delete Daily Entry" to "Delete Day ${target.log.dayNumber} log and revert cumulative totals?"
                  is DeleteTarget.FeedDispatchTarget -> "Delete Feed Dispatch" to "Delete challan ${target.dispatch.challanNo}?"
                  is DeleteTarget.BirdSaleTarget -> "Delete Bird Sale" to "Delete sale challan ${target.sale.challanNo} and revert lifted count?"
                  is DeleteTarget.FeedTransferTarget -> "Delete Inter-Feed Transfer" to "Delete transfer ${target.transfer.transferRef}?"
                  is DeleteTarget.MedicineEntryTarget -> "Delete Medicine Entry" to "Delete ${target.entry.medicineName} (${target.entry.invoiceOrSlipNo})?"
                  is DeleteTarget.KmDailyEntryTarget -> "Delete Daily KM Entry" to "Delete daily KM entry (${target.entry.totalKm.toInt()} km) for ${target.entry.empCode}?"
                  is DeleteTarget.KmClaimTarget -> "Delete Monthly KM Claim" to "Delete ${target.claim.resolvedMonthLabel} KM claim for ${target.claim.empCode}?"
                }
                ConfirmDeleteDialog(
                  title = title,
                  message = msg,
                  onDismiss = { deleteTarget = null },
                  onConfirmDelete = {
                    val snap = target
                    deleteTarget = null
                    when (snap) {
                      is DeleteTarget.ZoneTarget -> viewModel.deleteZone(snap.zone)
                      is DeleteTarget.FarmerTarget -> viewModel.deleteFarmer(snap.farmer)
                      is DeleteTarget.BatchTarget -> viewModel.deleteBatch(snap.batch)
                      is DeleteTarget.DailyLogTarget -> viewModel.deleteDailyLog(snap.log, snap.batch)
                      is DeleteTarget.FeedDispatchTarget -> viewModel.deleteFeedDispatch(snap.dispatch)
                      is DeleteTarget.BirdSaleTarget -> viewModel.deleteBirdSale(snap.sale, snap.batch)
                      is DeleteTarget.FeedTransferTarget -> viewModel.deleteFeedTransfer(snap.transfer)
                      is DeleteTarget.MedicineEntryTarget -> viewModel.deleteMedicineEntry(snap.entry)
                      is DeleteTarget.KmDailyEntryTarget -> viewModel.deleteDailyKmEntry(snap.entry)
                      is DeleteTarget.KmClaimTarget -> viewModel.deleteKmClaim(snap.claim)
                    }
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun AppDestination.icon(): ImageVector = when (this) {
  AppDestination.OVERVIEW -> Icons.Rounded.Dashboard
  AppDestination.FARMS -> Icons.Rounded.Agriculture
  AppDestination.BATCHES -> Icons.Rounded.Pets
  AppDestination.SETTLEMENTS -> Icons.Rounded.CurrencyRupee
  AppDestination.STAFF -> Icons.Rounded.Security
  AppDestination.DAILY_ENTRY -> Icons.Rounded.AssignmentTurnedIn
  AppDestination.FEED_DISPATCH -> Icons.Rounded.Inventory2
  AppDestination.FEED_TRANSFER -> Icons.Rounded.SwapHoriz
  AppDestination.KM_CLAIMS -> Icons.Rounded.DirectionsCar
}
