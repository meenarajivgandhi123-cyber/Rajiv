package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.DailyLog
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedTransfer
import com.example.data.model.FlockBatch
import com.example.data.model.MedicineEntry
import com.example.ui.components.BatchStatusBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDecimal
import com.example.ui.components.formatEpochDay
import com.example.ui.components.formatNumber
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.viewmodel.BatchFilter

enum class BatchLedgerTab(val label: String) {
  ALL("All Ledger"),
  DAILY("Daily Logs"),
  FEED("Feed Entry"),
  MEDICINE("Medicine"),
  TRANSFER("Inter-Feed"),
  SALES("Birds Sales"),
}

@Composable
fun BatchesScreen(
  batches: List<FlockBatch>,
  dailyLogs: List<DailyLog>,
  feedDispatches: List<FeedDispatch>,
  birdSales: List<BirdSale>,
  feedTransfers: List<FeedTransfer>,
  medicineEntries: List<MedicineEntry>,
  todayEpochDay: Long,
  selectedFilter: BatchFilter,
  searchQuery: String,
  selectedBatchId: String?,
  canEditOrDelete: Boolean,
  onFilterChange: (BatchFilter) -> Unit,
  onSearchChange: (String) -> Unit,
  onSelectBatch: (String?) -> Unit,
  onSyncMasterBatchesClick: () -> Unit = {},
  onCreateBatchClick: () -> Unit,
  onEditBatchClick: (FlockBatch) -> Unit,
  onDeleteBatchClick: (FlockBatch) -> Unit,
  onLogDailyClick: (FlockBatch) -> Unit,
  onEditDailyLogClick: (FlockBatch, DailyLog) -> Unit,
  onDeleteDailyLogClick: (FlockBatch, DailyLog) -> Unit,
  onIssueFeedClick: (FlockBatch) -> Unit,
  onEditFeedDispatchClick: (FeedDispatch) -> Unit,
  onDeleteFeedDispatchClick: (FeedDispatch) -> Unit,
  onMedicineEntryClick: (FlockBatch) -> Unit,
  onEditMedicineEntryClick: (MedicineEntry) -> Unit,
  onDeleteMedicineEntryClick: (MedicineEntry) -> Unit,
  onBirdSaleClick: (FlockBatch) -> Unit,
  onEditBirdSaleClick: (BirdSale) -> Unit,
  onDeleteBirdSaleClick: (BirdSale) -> Unit,
  onFeedTransferClick: (FlockBatch) -> Unit,
  onEditFeedTransferClick: (FeedTransfer) -> Unit,
  onDeleteFeedTransferClick: (FeedTransfer) -> Unit,
  onCloseBatchFlowClick: (FlockBatch) -> Unit,
  onDownloadGcPdfClick: (FlockBatch) -> Unit,
  onShareGcPdfClick: (FlockBatch) -> Unit,
  onDownloadBatchLedgerPdfClick: (FlockBatch) -> Unit,
  onShareBatchLedgerPdfClick: (FlockBatch) -> Unit,
  modifier: Modifier = Modifier,
) {
  val filteredBatches = batches.filter { batch ->
    val matchesFilter = selectedFilter.status == null || batch.statusEnum == selectedFilter.status
    val q = searchQuery.trim()
    val matchesSearch = q.isEmpty() ||
      batch.batchCode.contains(q, ignoreCase = true) ||
      batch.farmerName.contains(q, ignoreCase = true) ||
      batch.farmName.contains(q, ignoreCase = true) ||
      batch.farmCode.contains(q, ignoreCase = true) ||
      batch.zoneName.contains(q, ignoreCase = true) ||
      batch.region.contains(q, ignoreCase = true) ||
      batch.weekNumber.contains(q, ignoreCase = true) ||
      batch.challanNo.contains(q, ignoreCase = true)
    matchesFilter && matchesSearch
  }

  val selectedBatch = batches.firstOrNull { it.id == selectedBatchId }
    ?: filteredBatches.firstOrNull()

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isTwoPane = maxWidth >= 820.dp

    if (isTwoPane) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
          modifier = Modifier
            .weight(0.45f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          BatchesHeaderAndFilters(
            batches = batches,
            selectedFilter = selectedFilter,
            searchQuery = searchQuery,
            canEditOrDelete = canEditOrDelete,
            onFilterChange = onFilterChange,
            onSearchChange = onSearchChange,
            onSyncMasterBatchesClick = onSyncMasterBatchesClick,
            onCreateBatchClick = onCreateBatchClick,
          )
          BatchListPane(
            batches = filteredBatches,
            selectedBatchId = selectedBatch?.id,
            todayEpochDay = todayEpochDay,
            canEditOrDelete = canEditOrDelete,
            onBatchClick = { onSelectBatch(it.id) },
            onCreateBatchClick = onCreateBatchClick,
            modifier = Modifier.weight(1f),
          )
        }

        Surface(
          modifier = Modifier
            .weight(0.55f)
            .fillMaxHeight(),
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
        ) {
          if (selectedBatch != null) {
            BatchDetailPane(
              batch = selectedBatch,
              logs = dailyLogs.filter { it.batchId == selectedBatch.id },
              dispatches = feedDispatches.filter { it.batchId == selectedBatch.id },
              sales = birdSales.filter { it.batchId == selectedBatch.id },
              transfers = feedTransfers.filter {
                it.fromBatchId == selectedBatch.id || it.toBatchId == selectedBatch.id
              },
              medicines = medicineEntries.filter { it.batchId == selectedBatch.id },
              todayEpochDay = todayEpochDay,
              canEditOrDelete = canEditOrDelete,
              showBackButton = false,
              onBack = {},
              onEditBatchClick = { onEditBatchClick(selectedBatch) },
              onDeleteBatchClick = { onDeleteBatchClick(selectedBatch) },
              onLogDailyClick = { onLogDailyClick(selectedBatch) },
              onEditDailyLogClick = { onEditDailyLogClick(selectedBatch, it) },
              onDeleteDailyLogClick = { onDeleteDailyLogClick(selectedBatch, it) },
              onIssueFeedClick = { onIssueFeedClick(selectedBatch) },
              onEditFeedDispatchClick = onEditFeedDispatchClick,
              onDeleteFeedDispatchClick = onDeleteFeedDispatchClick,
              onMedicineEntryClick = { onMedicineEntryClick(selectedBatch) },
              onEditMedicineEntryClick = onEditMedicineEntryClick,
              onDeleteMedicineEntryClick = onDeleteMedicineEntryClick,
              onBirdSaleClick = { onBirdSaleClick(selectedBatch) },
              onEditBirdSaleClick = onEditBirdSaleClick,
              onDeleteBirdSaleClick = onDeleteBirdSaleClick,
              onFeedTransferClick = { onFeedTransferClick(selectedBatch) },
              onEditFeedTransferClick = onEditFeedTransferClick,
              onDeleteFeedTransferClick = onDeleteFeedTransferClick,
              onCloseBatchFlowClick = { onCloseBatchFlowClick(selectedBatch) },
              onDownloadGcPdfClick = { onDownloadGcPdfClick(selectedBatch) },
              onShareGcPdfClick = { onShareGcPdfClick(selectedBatch) },
              onDownloadBatchLedgerPdfClick = { onDownloadBatchLedgerPdfClick(selectedBatch) },
              onShareBatchLedgerPdfClick = { onShareBatchLedgerPdfClick(selectedBatch) },
            )
          } else {
            EmptyStateCard(
              icon = Icons.Rounded.Pets,
              title = "Select a Broiler Batch",
              description = "Inspect auto-calculated age, Daily Entry, Feed Entry, Medicine Entry, Inter-Feed Transfer, Birds Sales Ledger, or download the GC Closed PDF.",
              modifier = Modifier.padding(24.dp),
            )
          }
        }
      }
    } else {
      val mobileDetailBatch = batches.firstOrNull { it.id == selectedBatchId }
      if (mobileDetailBatch != null) {
        BackHandler { onSelectBatch(null) }
        BatchDetailPane(
          batch = mobileDetailBatch,
          logs = dailyLogs.filter { it.batchId == mobileDetailBatch.id },
          dispatches = feedDispatches.filter { it.batchId == mobileDetailBatch.id },
          sales = birdSales.filter { it.batchId == mobileDetailBatch.id },
          transfers = feedTransfers.filter {
            it.fromBatchId == mobileDetailBatch.id || it.toBatchId == mobileDetailBatch.id
          },
          medicines = medicineEntries.filter { it.batchId == mobileDetailBatch.id },
          todayEpochDay = todayEpochDay,
          canEditOrDelete = canEditOrDelete,
          showBackButton = true,
          onBack = { onSelectBatch(null) },
          onEditBatchClick = { onEditBatchClick(mobileDetailBatch) },
          onDeleteBatchClick = { onDeleteBatchClick(mobileDetailBatch) },
          onLogDailyClick = { onLogDailyClick(mobileDetailBatch) },
          onEditDailyLogClick = { onEditDailyLogClick(mobileDetailBatch, it) },
          onDeleteDailyLogClick = { onDeleteDailyLogClick(mobileDetailBatch, it) },
          onIssueFeedClick = { onIssueFeedClick(mobileDetailBatch) },
          onEditFeedDispatchClick = onEditFeedDispatchClick,
          onDeleteFeedDispatchClick = onDeleteFeedDispatchClick,
          onMedicineEntryClick = { onMedicineEntryClick(mobileDetailBatch) },
          onEditMedicineEntryClick = onEditMedicineEntryClick,
          onDeleteMedicineEntryClick = onDeleteMedicineEntryClick,
          onBirdSaleClick = { onBirdSaleClick(mobileDetailBatch) },
          onEditBirdSaleClick = onEditBirdSaleClick,
          onDeleteBirdSaleClick = onDeleteBirdSaleClick,
          onFeedTransferClick = { onFeedTransferClick(mobileDetailBatch) },
          onEditFeedTransferClick = onEditFeedTransferClick,
          onDeleteFeedTransferClick = onDeleteFeedTransferClick,
          onCloseBatchFlowClick = { onCloseBatchFlowClick(mobileDetailBatch) },
          onDownloadGcPdfClick = { onDownloadGcPdfClick(mobileDetailBatch) },
          onShareGcPdfClick = { onShareGcPdfClick(mobileDetailBatch) },
          onDownloadBatchLedgerPdfClick = { onDownloadBatchLedgerPdfClick(mobileDetailBatch) },
          onShareBatchLedgerPdfClick = { onShareBatchLedgerPdfClick(mobileDetailBatch) },
        )
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          BatchesHeaderAndFilters(
            batches = batches,
            selectedFilter = selectedFilter,
            searchQuery = searchQuery,
            canEditOrDelete = canEditOrDelete,
            onFilterChange = onFilterChange,
            onSearchChange = onSearchChange,
            onSyncMasterBatchesClick = onSyncMasterBatchesClick,
            onCreateBatchClick = onCreateBatchClick,
          )
          BatchListPane(
            batches = filteredBatches,
            selectedBatchId = null,
            todayEpochDay = todayEpochDay,
            canEditOrDelete = canEditOrDelete,
            onBatchClick = { onSelectBatch(it.id) },
            onCreateBatchClick = onCreateBatchClick,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun BatchesHeaderAndFilters(
  batches: List<FlockBatch>,
  selectedFilter: BatchFilter,
  searchQuery: String,
  canEditOrDelete: Boolean,
  onFilterChange: (BatchFilter) -> Unit,
  onSearchChange: (String) -> Unit,
  onSyncMasterBatchesClick: () -> Unit,
  onCreateBatchClick: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Flock Batches & Ledger",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "${batches.size} total placements",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (canEditOrDelete) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onSyncMasterBatchesClick,
            modifier = Modifier.testTag("batches_sync_master_button"),
          ) {
            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Sync Flocks")
          }
          Button(
            onClick = onCreateBatchClick,
            modifier = Modifier.testTag("batches_create_batch_button"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("New Batch")
          }
        }
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      placeholder = { Text("Search flock ID, farm code/name, branch, challan...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      singleLine = true,
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth().testTag("batches_search_input"),
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      BatchFilter.entries.forEach { filter ->
        val count = if (filter.status == null) batches.size else batches.count { it.statusEnum == filter.status }
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { onFilterChange(filter) },
          label = { Text("${filter.label} ($count)") },
        )
      }
    }
  }
}

@Composable
private fun BatchListPane(
  batches: List<FlockBatch>,
  selectedBatchId: String?,
  todayEpochDay: Long,
  canEditOrDelete: Boolean,
  onBatchClick: (FlockBatch) -> Unit,
  onCreateBatchClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (batches.isEmpty()) {
    EmptyStateCard(
      icon = Icons.Rounded.Pets,
      title = "No Batches Found",
      description = "Create a new broiler batch on an Empty Farm to start tracking daily mortality, feed, bird sales, and FCR.",
      actionLabel = if (canEditOrDelete) "Create Batch" else null,
      onAction = if (canEditOrDelete) onCreateBatchClick else null,
      modifier = modifier,
    )
  } else {
    LazyColumn(
      modifier = modifier.fillMaxWidth(),
      contentPadding = PaddingValues(bottom = 84.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(batches, key = { it.id }) { batch ->
        val isSelected = batch.id == selectedBatchId
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onBatchClick(batch) }
            .testTag("batch_card_${batch.batchCode}"),
          shape = MaterialTheme.shapes.medium,
          border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = batch.batchCode,
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "${batch.farmCode} • ${batch.displayFarmName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              BatchStatusBadge(status = batch.statusEnum)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${batch.displayAgeLabel(todayEpochDay)} • ${batch.zoneName.ifBlank { batch.breed }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
              )
              Text(
                text = if (batch.effectiveFcr > 0.0) {
                  "FCR ${formatDecimal(batch.effectiveFcr)} • ${formatDecimal(batch.mortalityPercent, 1)}% mort"
                } else {
                  "${formatNumber(batch.totalBirdsStarted)} placed"
                },
                style = MonoMetricSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BatchDetailPane(
  batch: FlockBatch,
  logs: List<DailyLog>,
  dispatches: List<FeedDispatch>,
  sales: List<BirdSale>,
  transfers: List<FeedTransfer>,
  medicines: List<MedicineEntry>,
  todayEpochDay: Long,
  canEditOrDelete: Boolean,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEditBatchClick: () -> Unit,
  onDeleteBatchClick: () -> Unit,
  onLogDailyClick: () -> Unit,
  onEditDailyLogClick: (DailyLog) -> Unit,
  onDeleteDailyLogClick: (DailyLog) -> Unit,
  onIssueFeedClick: () -> Unit,
  onEditFeedDispatchClick: (FeedDispatch) -> Unit,
  onDeleteFeedDispatchClick: (FeedDispatch) -> Unit,
  onMedicineEntryClick: () -> Unit,
  onEditMedicineEntryClick: (MedicineEntry) -> Unit,
  onDeleteMedicineEntryClick: (MedicineEntry) -> Unit,
  onBirdSaleClick: () -> Unit,
  onEditBirdSaleClick: (BirdSale) -> Unit,
  onDeleteBirdSaleClick: (BirdSale) -> Unit,
  onFeedTransferClick: () -> Unit,
  onEditFeedTransferClick: (FeedTransfer) -> Unit,
  onDeleteFeedTransferClick: (FeedTransfer) -> Unit,
  onCloseBatchFlowClick: () -> Unit,
  onDownloadGcPdfClick: () -> Unit,
  onShareGcPdfClick: () -> Unit,
  onDownloadBatchLedgerPdfClick: () -> Unit,
  onShareBatchLedgerPdfClick: () -> Unit,
) {
  var selectedLedgerTab by remember(batch.id) { mutableStateOf(BatchLedgerTab.ALL) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (showBackButton) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to batches")
          }
        }
        Column {
          Text(batch.batchCode, style = MaterialTheme.typography.headlineSmall)
          Text(
            text = "${batch.farmCode} • ${batch.displayFarmName} • Hatch ${formatEpochDay(batch.placementEpochDay)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BatchStatusBadge(status = batch.statusEnum)
        if (canEditOrDelete) {
          if (batch.statusEnum != BatchStatus.CLOSED) {
            IconButton(
              onClick = onEditBatchClick,
              modifier = Modifier.size(34.dp).testTag("edit_batch_btn"),
            ) {
              Icon(Icons.Rounded.Edit, contentDescription = "Edit batch", modifier = Modifier.size(18.dp))
            }
          }
          IconButton(
            onClick = onDeleteBatchClick,
            modifier = Modifier.size(34.dp).testTag("delete_batch_btn"),
          ) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = "Delete batch",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }

    // Performance Metrics Grid
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatCard(
          label = "Surviving / Placed",
          value = "${formatNumber(batch.survivingBirds)} / ${formatNumber(batch.totalBirdsStarted)}",
          modifier = Modifier.weight(1f),
        )
        MiniStatCard(
          label = "Auto Flock Age",
          value = "${batch.displayAgeLabel(todayEpochDay)} / ${batch.targetMarketAgeDays}d",
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatCard(
          label = "Effective FCR (Std ${formatDecimal(batch.standardFcr)})",
          value = if (batch.effectiveFcr > 0.0) formatDecimal(batch.effectiveFcr) else "—",
          modifier = Modifier.weight(1f),
        )
        MiniStatCard(
          label = "Mortality (${formatNumber(batch.cumulativeMortality)})",
          value = "${formatDecimal(batch.mortalityPercent, 2)}%",
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatCard(
          label = "Avg Body Weight",
          value = "${batch.latestAvgWeightGrams.toInt()} g",
          modifier = Modifier.weight(1f),
        )
        MiniStatCard(
          label = "EPEF Score",
          value = batch.computeEpef(todayEpochDay).let { if (it > 0.0) formatDecimal(it, 0) else "—" },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Flock Placement Details Card
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.fillMaxWidth().testTag("batch_placement_details_card"),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "Flock Placement Details",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          DetailMetaItem("Region", batch.region.ifBlank { "—" }, Modifier.weight(1f))
          DetailMetaItem("Branch", batch.zoneName.ifBlank { "—" }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          DetailMetaItem("Farm ID", batch.farmCode.ifBlank { "—" }, Modifier.weight(1f))
          DetailMetaItem("Farm Name", batch.displayFarmName.ifBlank { "—" }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          DetailMetaItem("Hatch Date", formatEpochDay(batch.placementEpochDay), Modifier.weight(1f))
          DetailMetaItem(
            "Total Housed",
            "${formatNumber(batch.totalBirdsStarted)} (D.O.L.D. ${formatNumber(batch.chicksPlaced)} + Free ${formatNumber(batch.freeChicks)})",
            Modifier.weight(1f),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          DetailMetaItem("Week No.", batch.weekNumber.ifBlank { "—" }, Modifier.weight(1f))
          DetailMetaItem("Challan No.", batch.challanNo.ifBlank { "—" }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          DetailMetaItem("Breed", batch.breed.ifBlank { "—" }, Modifier.weight(1f))
          DetailMetaItem(
            "Status / Date",
            if (batch.closedEpochDay > 0L) "${batch.statusEnum.label} (${formatEpochDay(batch.closedEpochDay)})"
            else batch.statusEnum.label,
            Modifier.weight(1f),
          )
        }
      }
    }

    // GC Closed Banner or Actions
    if (batch.statusEnum == BatchStatus.CLOSED) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column {
              Text(
                text = if (batch.closedEpochDay > 0L) {
                  "GC Closed (${formatEpochDay(batch.closedEpochDay)})"
                } else {
                  "GC Closed"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "Sold ${formatNumber(batch.liftedBirds)} birds (${formatDecimal(batch.liftedWeightKg, 1)} kg) • Payable ${formatCurrency(batch.settlementPayout)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onDownloadGcPdfClick,
              modifier = Modifier.weight(1f).testTag("batch_download_gc_pdf_btn"),
            ) {
              Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("Download GC PDF")
            }
            OutlinedButton(
              onClick = onShareGcPdfClick,
              modifier = Modifier.weight(1f).testTag("batch_share_gc_pdf_btn"),
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("Open / Share")
            }
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onLogDailyClick,
            modifier = Modifier.weight(1f).testTag("batch_daily_log_btn"),
          ) {
            Icon(Icons.Rounded.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Daily Entry")
          }
          if (canEditOrDelete) {
            OutlinedButton(
              onClick = onIssueFeedClick,
              modifier = Modifier.weight(1f).testTag("batch_feed_dispatch_btn"),
            ) {
              Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("Feed Entry")
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (canEditOrDelete) {
            OutlinedButton(
              onClick = onBirdSaleClick,
              modifier = Modifier.weight(1f).testTag("batch_bird_sale_btn"),
            ) {
              Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("Birds Sale")
            }
          }
          OutlinedButton(
            onClick = onFeedTransferClick,
            modifier = Modifier.weight(1f).testTag("batch_feed_transfer_btn"),
          ) {
            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Inter-Feed")
          }
        }
        if (canEditOrDelete) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = onMedicineEntryClick,
              modifier = Modifier.weight(1f).testTag("batch_medicine_entry_btn"),
            ) {
              Icon(Icons.Rounded.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("Medicine")
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onCloseBatchFlowClick,
              modifier = Modifier.weight(1f).testTag("batch_close_option_btn"),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text(
                if (batch.statusEnum == BatchStatus.CLOSING) {
                  "Update Closing / GC Closed"
                } else {
                  "Closing Farm / GC Closed"
                }
              )
            }
            if (batch.statusEnum == BatchStatus.CLOSING) {
              OutlinedButton(
                onClick = onDownloadGcPdfClick,
                modifier = Modifier.testTag("batch_closing_preview_pdf_btn"),
              ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("GC PDF")
              }
            }
          }
        }
      }
    }

    HorizontalDivider()

    // Ledger Section Header + PDF Actions + Tabs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Batch Ledger",
        style = MaterialTheme.typography.titleMedium,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = onDownloadBatchLedgerPdfClick,
          modifier = Modifier.testTag("batch_download_ledger_pdf_btn"),
        ) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("Ledger PDF")
        }
        IconButton(
          onClick = onShareBatchLedgerPdfClick,
          modifier = Modifier.testTag("batch_share_ledger_pdf_btn"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = "Share ledger PDF")
        }
      }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      BatchLedgerTab.entries.forEach { tab ->
        val badgeCount = when (tab) {
          BatchLedgerTab.ALL -> logs.size + dispatches.size + medicines.size + transfers.size + sales.size
          BatchLedgerTab.DAILY -> logs.size
          BatchLedgerTab.FEED -> dispatches.size
          BatchLedgerTab.MEDICINE -> medicines.size
          BatchLedgerTab.TRANSFER -> transfers.size
          BatchLedgerTab.SALES -> sales.size
        }
        FilterChip(
          selected = selectedLedgerTab == tab,
          onClick = { selectedLedgerTab = tab },
          label = { Text("${tab.label} ($badgeCount)") },
          modifier = Modifier.testTag("batch_ledger_tab_${tab.name.lowercase()}"),
        )
      }
    }

    // 1. Daily Logs Ledger
    if (selectedLedgerTab == BatchLedgerTab.ALL || selectedLedgerTab == BatchLedgerTab.DAILY) {
      Text(
        text = "Daily Performance Logs (${logs.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
      if (logs.isEmpty()) {
        EmptyLedgerHint("No daily logs recorded yet.")
      } else {
        logs.forEach { log ->
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Day ${log.dayNumber} • ${formatEpochDay(log.logEpochDay)}",
                  style = MaterialTheme.typography.titleSmall,
                )
                if (log.looseBirds > 0L || log.feederStock > 0L) {
                  Text(
                    text = "Loose: ${log.looseBirds} • Feeder Stock: ${log.feederStock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (log.notes.isNotBlank()) {
                  Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "Mort ${log.mortalityCount} • Cull ${log.cullCount}",
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.error,
                  )
                  Text(
                    text = "${formatDecimal(log.feedConsumedKg, 1)} kg • ${log.avgWeightGrams.toInt()} g",
                    style = MonoMetricSmall,
                  )
                }
                if (canEditOrDelete) {
                  IconButton(
                    onClick = { onEditDailyLogClick(log) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit daily log", modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = { onDeleteDailyLogClick(log) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete daily log",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 2. Feed Entry Ledger
    if (selectedLedgerTab == BatchLedgerTab.ALL || selectedLedgerTab == BatchLedgerTab.FEED) {
      Text(
        text = "Feed Entry Details (${dispatches.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
      if (dispatches.isEmpty()) {
        EmptyLedgerHint("No feed dispatches recorded for this batch.")
      } else {
        dispatches.forEach { dispatch ->
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "${dispatch.feedType} • Challan ${dispatch.challanNo}",
                  style = MaterialTheme.typography.titleSmall,
                )
                val destFarm = dispatch.resolvedDestinationFarm
                Text(
                  text = listOfNotNull(
                    if (destFarm.isNotBlank()) "To: $destFarm" else null,
                    formatEpochDay(dispatch.dispatchEpochDay),
                    dispatch.vehicleNo.takeIf { it.isNotBlank() }?.let { "Veh $it" },
                  ).joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (dispatch.remarks.isNotBlank()) {
                  Text(
                    text = dispatch.remarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "+${dispatch.bagsCount} bags (${formatDecimal(dispatch.weightKg, 0)} kg)",
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  if (dispatch.resolvedTotalAmount > 0.0 || dispatch.unitRate > 0.0) {
                    val priceLabel = buildString {
                      if (dispatch.resolvedTotalAmount > 0.0) append(formatCurrency(dispatch.resolvedTotalAmount))
                      if (dispatch.unitRate > 0.0) {
                        if (isNotEmpty()) append(" ")
                        append("(@ ${formatCurrency(dispatch.unitRate)}${dispatch.rateBasisEnum.unitSuffix})")
                      }
                    }
                    Text(
                      text = priceLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                if (canEditOrDelete) {
                  IconButton(
                    onClick = { onEditFeedDispatchClick(dispatch) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit feed dispatch", modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = { onDeleteFeedDispatchClick(dispatch) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete feed dispatch",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 2B. Medicine Entry Ledger
    if (selectedLedgerTab == BatchLedgerTab.ALL || selectedLedgerTab == BatchLedgerTab.MEDICINE) {
      val medTotal = medicines.sumOf { it.totalCost }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Medicine Entry Details (${medicines.size})",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        if (medicines.isNotEmpty()) {
          Text(
            text = "Total ${formatCurrency(medTotal)}",
            style = MonoMetricSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      if (medicines.isEmpty()) {
        EmptyLedgerHint("No medicine or vaccine entries recorded for this batch.")
      } else {
        medicines.forEach { med ->
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${med.medicineName} • ${med.category}",
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = listOfNotNull(
                    med.invoiceOrSlipNo.takeIf { it.isNotBlank() },
                    med.dosageOrQty.takeIf { it.isNotBlank() },
                    formatEpochDay(med.entryEpochDay),
                  ).joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (med.remarks.isNotBlank()) {
                  Text(
                    text = med.remarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = formatCurrency(med.totalCost),
                  style = MonoMetricSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                if (canEditOrDelete) {
                  IconButton(
                    onClick = { onEditMedicineEntryClick(med) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit medicine entry", modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = { onDeleteMedicineEntryClick(med) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete medicine entry",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Inter-Feed Entry Ledger
    if (selectedLedgerTab == BatchLedgerTab.ALL || selectedLedgerTab == BatchLedgerTab.TRANSFER) {
      Text(
        text = "Inter-Feed Entry Details (${transfers.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
      if (transfers.isEmpty()) {
        EmptyLedgerHint("No inter-feed transfers recorded for this batch.")
      } else {
        transfers.forEach { tr ->
          val isIncoming = tr.toBatchId == batch.id
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${if (isIncoming) "IN from" else "OUT to"} ${if (isIncoming) tr.fromFarmCode else tr.toFarmCode} (${if (isIncoming) tr.fromBatchCode else tr.toBatchCode})",
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = "${tr.transferRef} • ${tr.feedType} • ${formatEpochDay(tr.transferEpochDay)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "${if (isIncoming) "+" else "-"}${tr.bagsCount} bags (${formatDecimal(tr.weightKg, 0)} kg)",
                  style = MonoMetricSmall,
                  color = if (isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                if (canEditOrDelete) {
                  IconButton(
                    onClick = { onEditFeedTransferClick(tr) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit feed transfer", modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = { onDeleteFeedTransferClick(tr) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete feed transfer",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 4. Birds Sales Entry Ledger
    if (selectedLedgerTab == BatchLedgerTab.ALL || selectedLedgerTab == BatchLedgerTab.SALES) {
      Text(
        text = "Birds Sales Entry Details (${sales.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
      if (sales.isEmpty()) {
        EmptyLedgerHint("No birds sales recorded for this batch.")
      } else {
        sales.forEach { sale ->
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val saleAge = sale.resolvedAgeDays(batch.placementEpochDay)
                val saleAvgWt = sale.resolvedAvgWeightKg()
                val customerTitle = buildString {
                  append(sale.buyerName)
                  if (sale.buyerPhone.isNotBlank()) append(" (${sale.buyerPhone})")
                  append(" • ${sale.challanNo}")
                }
                Text(
                  text = customerTitle,
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = "${formatEpochDay(sale.saleEpochDay)} • Age D$saleAge" +
                    (if (saleAvgWt > 0.0) " • Avg ${formatDecimal(saleAvgWt, 3)} kg" else "") +
                    (if (sale.vehicleNo.isNotBlank()) " • ${sale.vehicleNo}" else ""),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (sale.remarks.isNotBlank()) {
                  Text(
                    text = sale.remarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "${formatNumber(sale.birdsSold)} birds (${formatDecimal(sale.totalWeightKg, 1)} kg)",
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = "${formatCurrency(sale.saleAmount)} @ ${formatDecimal(sale.ratePerKg, 1)}/kg",
                    style = MonoMetricSmall,
                  )
                }
                if (canEditOrDelete) {
                  IconButton(
                    onClick = { onEditBirdSaleClick(sale) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit bird sale", modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = { onDeleteBirdSaleClick(sale) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete bird sale",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyLedgerHint(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun DetailMetaItem(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MiniStatCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MonoMetricMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
