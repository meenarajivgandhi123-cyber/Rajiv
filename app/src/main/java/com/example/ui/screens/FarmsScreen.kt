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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FarmStatus
import com.example.data.model.Farmer
import com.example.data.model.FlockBatch
import com.example.data.model.IntegrationZone
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FarmStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatNumber
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.viewmodel.FarmFilter

@Composable
fun FarmsScreen(
  zones: List<IntegrationZone>,
  farmers: List<Farmer>,
  batches: List<FlockBatch>,
  selectedFilter: FarmFilter,
  selectedZoneFilterId: String?,
  searchQuery: String,
  selectedFarmerId: String?,
  canEditOrDelete: Boolean,
  onFilterChange: (FarmFilter) -> Unit,
  onZoneFilterChange: (String?) -> Unit,
  onSearchChange: (String) -> Unit,
  onSelectFarmer: (String?) -> Unit,
  onCreateZoneClick: () -> Unit,
  onEditZoneClick: (IntegrationZone) -> Unit,
  onDeleteZoneClick: (IntegrationZone) -> Unit,
  onCreateFarmerClick: () -> Unit,
  onEditFarmerClick: (Farmer) -> Unit,
  onDeleteFarmerClick: (Farmer) -> Unit,
  onCreateBatchForFarmerClick: (Farmer) -> Unit,
  onOpenLinkedBatch: (String) -> Unit,
  onMarkClosingFarm: (Farmer) -> Unit,
  onMarkEmptyFarm: (Farmer) -> Unit,
  onRequestCancelFarm: (Farmer) -> Unit,
  onReactivateFarm: (Farmer) -> Unit,
  onDownloadFarmLedgerPdf: (Farmer) -> Unit,
  onShareFarmLedgerPdf: (Farmer) -> Unit,
  onSyncMasterFarmersClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val filteredFarmers = farmers.filter { farmer ->
    val matchesFilter = selectedFilter.status == null || farmer.statusEnum == selectedFilter.status
    val matchesZone = selectedZoneFilterId == null || farmer.zoneId == selectedZoneFilterId
    val q = searchQuery.trim()
    val matchesSearch = q.isEmpty() ||
      farmer.farmerName.contains(q, ignoreCase = true) ||
      farmer.displayFarmName.contains(q, ignoreCase = true) ||
      farmer.farmCode.contains(q, ignoreCase = true) ||
      farmer.zoneName.contains(q, ignoreCase = true) ||
      farmer.region.contains(q, ignoreCase = true) ||
      farmer.farmType.contains(q, ignoreCase = true) ||
      farmer.village.contains(q, ignoreCase = true) ||
      farmer.tehsil.contains(q, ignoreCase = true) ||
      farmer.phone.contains(q, ignoreCase = true)
    matchesFilter && matchesZone && matchesSearch
  }

  val selectedFarmer = farmers.firstOrNull { it.id == selectedFarmerId }
    ?: filteredFarmers.firstOrNull()

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
            .weight(0.46f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FarmsHeaderAndFilters(
            zones = zones,
            farmers = farmers,
            selectedFilter = selectedFilter,
            selectedZoneFilterId = selectedZoneFilterId,
            searchQuery = searchQuery,
            canEditOrDelete = canEditOrDelete,
            onFilterChange = onFilterChange,
            onZoneFilterChange = onZoneFilterChange,
            onSearchChange = onSearchChange,
            onCreateZoneClick = onCreateZoneClick,
            onEditZoneClick = onEditZoneClick,
            onDeleteZoneClick = onDeleteZoneClick,
            onCreateFarmerClick = onCreateFarmerClick,
            onSyncMasterFarmersClick = onSyncMasterFarmersClick,
          )
          FarmListPane(
            farmers = filteredFarmers,
            selectedFarmerId = selectedFarmer?.id,
            selectedFilter = selectedFilter,
            canEditOrDelete = canEditOrDelete,
            onFarmerClick = { onSelectFarmer(it.id) },
            onCreateFarmerClick = onCreateFarmerClick,
            modifier = Modifier.weight(1f),
          )
        }

        Surface(
          modifier = Modifier
            .weight(0.54f)
            .fillMaxHeight(),
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
        ) {
          if (selectedFarmer != null) {
            val linkedBatch = batches.firstOrNull { it.id == selectedFarmer.currentBatchId }
            val farmBatchCount = batches.count { it.farmerId == selectedFarmer.id }
            FarmDetailPane(
              farmer = selectedFarmer,
              linkedBatch = linkedBatch,
              farmBatchCount = farmBatchCount,
              canEditOrDelete = canEditOrDelete,
              showBackButton = false,
              onBack = {},
              onEditFarmerClick = { onEditFarmerClick(selectedFarmer) },
              onDeleteFarmerClick = { onDeleteFarmerClick(selectedFarmer) },
              onCreateBatchClick = { onCreateBatchForFarmerClick(selectedFarmer) },
              onOpenLinkedBatch = { linkedBatch?.let { onOpenLinkedBatch(it.id) } },
              onDownloadFarmLedgerPdf = { onDownloadFarmLedgerPdf(selectedFarmer) },
              onShareFarmLedgerPdf = { onShareFarmLedgerPdf(selectedFarmer) },
              onMarkClosingFarm = { onMarkClosingFarm(selectedFarmer) },
              onMarkEmptyFarm = { onMarkEmptyFarm(selectedFarmer) },
              onRequestCancelFarm = { onRequestCancelFarm(selectedFarmer) },
              onReactivateFarm = { onReactivateFarm(selectedFarmer) },
            )
          } else {
            EmptyStateCard(
              icon = Icons.Rounded.Agriculture,
              title = "Select a Farm",
              description = "Pick any farm on the left to inspect zone assignment, bank details, active batch status, or lifecycle transitions.",
              modifier = Modifier.padding(24.dp),
            )
          }
        }
      }
    } else {
      val mobileDetailFarmer = farmers.firstOrNull { it.id == selectedFarmerId }
      if (mobileDetailFarmer != null) {
        BackHandler { onSelectFarmer(null) }
        val linkedBatch = batches.firstOrNull { it.id == mobileDetailFarmer.currentBatchId }
        val farmBatchCount = batches.count { it.farmerId == mobileDetailFarmer.id }
        FarmDetailPane(
          farmer = mobileDetailFarmer,
          linkedBatch = linkedBatch,
          farmBatchCount = farmBatchCount,
          canEditOrDelete = canEditOrDelete,
          showBackButton = true,
          onBack = { onSelectFarmer(null) },
          onEditFarmerClick = { onEditFarmerClick(mobileDetailFarmer) },
          onDeleteFarmerClick = { onDeleteFarmerClick(mobileDetailFarmer) },
          onCreateBatchClick = { onCreateBatchForFarmerClick(mobileDetailFarmer) },
          onOpenLinkedBatch = { linkedBatch?.let { onOpenLinkedBatch(it.id) } },
          onDownloadFarmLedgerPdf = { onDownloadFarmLedgerPdf(mobileDetailFarmer) },
          onShareFarmLedgerPdf = { onShareFarmLedgerPdf(mobileDetailFarmer) },
          onMarkClosingFarm = { onMarkClosingFarm(mobileDetailFarmer) },
          onMarkEmptyFarm = { onMarkEmptyFarm(mobileDetailFarmer) },
          onRequestCancelFarm = { onRequestCancelFarm(mobileDetailFarmer) },
          onReactivateFarm = { onReactivateFarm(mobileDetailFarmer) },
        )
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FarmsHeaderAndFilters(
            zones = zones,
            farmers = farmers,
            selectedFilter = selectedFilter,
            selectedZoneFilterId = selectedZoneFilterId,
            searchQuery = searchQuery,
            canEditOrDelete = canEditOrDelete,
            onFilterChange = onFilterChange,
            onZoneFilterChange = onZoneFilterChange,
            onSearchChange = onSearchChange,
            onCreateZoneClick = onCreateZoneClick,
            onEditZoneClick = onEditZoneClick,
            onDeleteZoneClick = onDeleteZoneClick,
            onCreateFarmerClick = onCreateFarmerClick,
            onSyncMasterFarmersClick = onSyncMasterFarmersClick,
          )
          FarmListPane(
            farmers = filteredFarmers,
            selectedFarmerId = null,
            selectedFilter = selectedFilter,
            canEditOrDelete = canEditOrDelete,
            onFarmerClick = { onSelectFarmer(it.id) },
            onCreateFarmerClick = onCreateFarmerClick,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun FarmsHeaderAndFilters(
  zones: List<IntegrationZone>,
  farmers: List<Farmer>,
  selectedFilter: FarmFilter,
  selectedZoneFilterId: String?,
  searchQuery: String,
  canEditOrDelete: Boolean,
  onFilterChange: (FarmFilter) -> Unit,
  onZoneFilterChange: (String?) -> Unit,
  onSearchChange: (String) -> Unit,
  onCreateZoneClick: () -> Unit,
  onEditZoneClick: (IntegrationZone) -> Unit,
  onDeleteZoneClick: (IntegrationZone) -> Unit,
  onCreateFarmerClick: () -> Unit,
  onSyncMasterFarmersClick: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Column {
        Text(
          text = "Zones & Contract Farms",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "${zones.size} zones • ${farmers.size} registered sheds",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (canEditOrDelete) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onCreateFarmerClick,
            modifier = Modifier.testTag("farms_add_farmer_button"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Farmer")
          }
          OutlinedButton(
            onClick = onCreateZoneClick,
            modifier = Modifier.testTag("farms_add_zone_button"),
          ) {
            Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Zone")
          }
          OutlinedButton(
            onClick = onSyncMasterFarmersClick,
            modifier = Modifier.testTag("farms_sync_master_button"),
          ) {
            Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Sync Sheet")
          }
        }
      }
    }

    if (zones.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = selectedZoneFilterId == null,
          onClick = { onZoneFilterChange(null) },
          label = { Text("All Zones") },
        )
        zones.forEach { zone ->
          val count = farmers.count { it.zoneId == zone.id }
          Surface(
            shape = MaterialTheme.shapes.small,
            color = if (selectedZoneFilterId == zone.id) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            modifier = Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable {
                onZoneFilterChange(if (selectedZoneFilterId == zone.id) null else zone.id)
              },
          ) {
            Row(
              modifier = Modifier.padding(
                start = 10.dp,
                end = if (canEditOrDelete) 4.dp else 10.dp,
                top = 6.dp,
                bottom = 6.dp,
              ),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "${zone.zoneName} ($count)",
                style = MaterialTheme.typography.labelMedium,
              )
              if (canEditOrDelete) {
                IconButton(
                  onClick = { onEditZoneClick(zone) },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(Icons.Rounded.Edit, contentDescription = "Edit zone", modifier = Modifier.size(14.dp))
                }
                if (count == 0) {
                  IconButton(
                    onClick = { onDeleteZoneClick(zone) },
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete zone",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      placeholder = { Text("Search farm code, farmer, village, tehsil, phone, region...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      singleLine = true,
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth(),
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FarmFilter.entries.forEach { filter ->
        val count = if (filter.status == null) farmers.size else farmers.count { it.statusEnum == filter.status }
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { onFilterChange(filter) },
          label = { Text("${filter.label} ($count)") },
          modifier = Modifier.testTag("farm_filter_${filter.name.lowercase()}"),
        )
      }
    }
  }
}

@Composable
private fun FarmListPane(
  farmers: List<Farmer>,
  selectedFarmerId: String?,
  selectedFilter: FarmFilter,
  canEditOrDelete: Boolean,
  onFarmerClick: (Farmer) -> Unit,
  onCreateFarmerClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (farmers.isEmpty()) {
    EmptyStateCard(
      icon = if (selectedFilter == FarmFilter.CANCELLED) Icons.Rounded.Block else Icons.Rounded.Agriculture,
      title = "No farms in ${selectedFilter.label}",
      description = when (selectedFilter) {
        FarmFilter.CANCELLED -> "No farms have been moved to the Cancel Farm List."
        FarmFilter.EMPTY -> "No Empty Farms right now. Close an active batch or add a new farmer."
        FarmFilter.CLOSING -> "No farms are currently in the Closing Farm phase."
        FarmFilter.ACTIVE -> "No farms currently have an active broiler batch."
        FarmFilter.ALL -> "Create zones and register contract farmers to manage sheds and place broiler batches."
      },
      actionLabel = if (canEditOrDelete && selectedFilter == FarmFilter.ALL) "Create Farmer" else null,
      onAction = if (canEditOrDelete && selectedFilter == FarmFilter.ALL) onCreateFarmerClick else null,
      modifier = modifier,
    )
  } else {
    LazyColumn(
      modifier = modifier.fillMaxWidth(),
      contentPadding = PaddingValues(bottom = 84.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(farmers, key = { it.id }) { farmer ->
        val isSelected = farmer.id == selectedFarmerId
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onFarmerClick(farmer) }
            .testTag("farm_card_${farmer.farmCode}"),
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
                  text = farmer.displayFarmName,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = listOfNotNull(
                    farmer.farmCode,
                    farmer.farmerName,
                    farmer.region.takeIf { it.isNotBlank() },
                    farmer.zoneName.takeIf { it.isNotBlank() },
                    farmer.farmType.takeIf { it.isNotBlank() },
                  ).joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val locationLine = listOfNotNull(
                  farmer.village.takeIf { it.isNotBlank() },
                  farmer.tehsil.takeIf { it.isNotBlank() },
                  farmer.effectiveSqft.takeIf { it > 0L }?.let { "${formatNumber(it)} sq.ft" },
                ).joinToString(" • ")
                if (locationLine.isNotBlank()) {
                  Text(
                    text = locationLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              FarmStatusBadge(status = farmer.statusEnum)
            }
            if (farmer.currentBatchCode.isNotBlank()) {
              Text(
                text = "Current Batch: ${farmer.currentBatchCode} • ${formatNumber(farmer.shedCapacity)} cap",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (farmer.statusEnum == FarmStatus.CANCELLED && farmer.cancelReason.isNotBlank()) {
              Text(
                text = "Cancelled: ${farmer.cancelReason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FarmDetailPane(
  farmer: Farmer,
  linkedBatch: FlockBatch?,
  farmBatchCount: Int,
  canEditOrDelete: Boolean,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEditFarmerClick: () -> Unit,
  onDeleteFarmerClick: () -> Unit,
  onCreateBatchClick: () -> Unit,
  onOpenLinkedBatch: () -> Unit,
  onDownloadFarmLedgerPdf: () -> Unit,
  onShareFarmLedgerPdf: () -> Unit,
  onMarkClosingFarm: () -> Unit,
  onMarkEmptyFarm: () -> Unit,
  onRequestCancelFarm: () -> Unit,
  onReactivateFarm: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (showBackButton) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to farms")
          }
        }
        Column {
          Text(
            text = farmer.displayFarmName,
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "${farmer.farmCode} • ${farmer.farmerName}",
            style = MonoMetricMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      FarmStatusBadge(status = farmer.statusEnum)
    }

    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        InfoRow(
          Icons.Rounded.Map,
          "Region / Zone",
          listOfNotNull(
            farmer.region.takeIf { it.isNotBlank() },
            farmer.zoneName.ifBlank { "Unassigned" },
          ).joinToString(" • "),
        )
        InfoRow(Icons.Rounded.Agriculture, "Farm Type", farmer.farmType.ifBlank { "Own Farm" })
        InfoRow(
          Icons.Rounded.LocationOn,
          "Village / Tehsil",
          listOfNotNull(
            farmer.village.takeIf { it.isNotBlank() },
            farmer.tehsil.takeIf { it.isNotBlank() }?.let { "Tehsil: $it" },
          ).joinToString(" • ").ifBlank { "Not specified" },
        )
        InfoRow(Icons.Rounded.Phone, "Phone", farmer.phone.ifBlank { "Not specified" })
        InfoRow(
          Icons.Rounded.SquareFoot,
          "Total Sq.Ft / Shed",
          "${formatNumber(farmer.effectiveSqft)} sq.ft • ${farmer.shedType} (${formatNumber(farmer.shedCapacity)} cap)",
        )
        InfoRow(Icons.Rounded.CheckCircle, "Contract Rate", "${formatCurrency(farmer.growingRatePerKg)} / kg")
        InfoRow(
          Icons.Rounded.Badge,
          "Aadhaar / PAN",
          listOfNotNull(
            farmer.aadhaarNumber.takeIf { it.isNotBlank() }?.let { "Aadhaar: $it" },
            farmer.panNumber.takeIf { it.isNotBlank() }?.let { "PAN: $it" },
          ).joinToString(" • ").ifBlank { "Not specified" },
        )
        InfoRow(
          Icons.Rounded.AccountBalance,
          "Bank Details",
          listOfNotNull(
            farmer.bankName.takeIf { it.isNotBlank() },
            farmer.accountNumber.takeIf { it.isNotBlank() },
            farmer.ifscCode.takeIf { it.isNotBlank() },
            farmer.accountType.takeIf { it.isNotBlank() },
          ).joinToString(" • ").ifBlank { "Not specified" },
        )
      }
    }

    if (linkedBatch != null) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Linked Batch: ${linkedBatch.batchCode}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = "${formatNumber(linkedBatch.survivingBirds)} surviving • ${formatDecimalOrDash(linkedBatch.effectiveFcr)} FCR",
            style = MaterialTheme.typography.bodySmall,
          )
          Button(onClick = onOpenLinkedBatch) {
            Text("Open Batch & Ledger")
          }
        }
      }
    }

    Card(
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.PictureAsPdf,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = "Farm-Wise Ledger Statement",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "$farmBatchCount batch(es) • Daily, Feed, Medicine, Inter-Feed, Bird Sales & GC",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onDownloadFarmLedgerPdf,
            modifier = Modifier.weight(1f).testTag("farm_download_ledger_pdf_btn"),
          ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Download Ledger PDF")
          }
          OutlinedButton(
            onClick = onShareFarmLedgerPdf,
            modifier = Modifier.weight(1f).testTag("farm_share_ledger_pdf_btn"),
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Share")
          }
        }
      }
    }

    if (farmer.statusEnum == FarmStatus.CANCELLED) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "In Cancel Farm List",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
          Text(
            text = farmer.cancelReason.ifBlank { "No reason recorded" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
    }

    if (canEditOrDelete) {
      HorizontalDivider()

      Text(
        text = "Farm Lifecycle Actions",
        style = MaterialTheme.typography.titleMedium,
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (farmer.statusEnum == FarmStatus.EMPTY) {
          Button(
            onClick = onCreateBatchClick,
            modifier = Modifier.fillMaxWidth().testTag("farm_place_batch_btn"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Create Batch on Empty Farm")
          }
        }

        if (farmer.statusEnum == FarmStatus.ACTIVE) {
          OutlinedButton(
            onClick = onMarkClosingFarm,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Rounded.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Mark as Closing Farm")
          }
        }

        if (farmer.statusEnum == FarmStatus.CLOSING) {
          OutlinedButton(
            onClick = onMarkEmptyFarm,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Mark Shed as Empty Farm")
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onEditFarmerClick,
            modifier = Modifier.weight(1f).testTag("edit_farmer_btn"),
          ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Edit Farmer")
          }

          if (farmer.statusEnum == FarmStatus.CANCELLED) {
            Button(
              onClick = onReactivateFarm,
              modifier = Modifier.weight(1f).testTag("restore_farm_btn"),
            ) {
              Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Restore Empty")
            }
          } else {
            OutlinedButton(
              onClick = onRequestCancelFarm,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.weight(1f).testTag("cancel_farm_btn"),
            ) {
              Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Cancel Farm")
            }
          }
        }

        if (linkedBatch == null) {
          OutlinedButton(
            onClick = onDeleteFarmerClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("delete_farmer_btn"),
          ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Delete Farmer Profile")
          }
        }
      }
    }
  }
}

@Composable
private fun InfoRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.35f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      modifier = Modifier.weight(0.65f),
    )
  }
}

private fun formatDecimalOrDash(value: Double): String =
  if (value > 0.0) String.format(java.util.Locale.US, "%.2f", value) else "—"
