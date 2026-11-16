package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchStatus
import com.example.data.model.ClaimStatus
import com.example.data.model.DailyLog
import com.example.data.model.FarmStatus
import com.example.data.model.Farmer
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedTransfer
import com.example.data.model.FlockBatch
import com.example.data.model.KmClaim
import com.example.data.model.KmDailyEntry
import com.example.data.model.KmMonthHelper
import com.example.data.model.OperatorAccount
import com.example.ui.components.BatchStatusBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDecimal
import com.example.ui.components.formatEpochDay
import com.example.ui.components.formatNumber
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall

@Composable
fun UserDailyEntryScreen(
  farmers: List<Farmer>,
  batches: List<FlockBatch>,
  dailyLogs: List<DailyLog>,
  todayEpochDay: Long,
  onLogDailyClick: (FlockBatch) -> Unit,
  modifier: Modifier = Modifier,
) {
  val farmerById = farmers.associateBy { it.id }
  val openBatches = batches.filter { batch ->
    val linkedFarmer = farmerById[batch.farmerId]
    batch.statusEnum != BatchStatus.CLOSED &&
      linkedFarmer?.statusEnum != FarmStatus.EMPTY &&
      linkedFarmer?.statusEnum != FarmStatus.CANCELLED
  }
  val openBatchIds = openBatches.map { it.id }.toSet()
  val activeDailyLogs = dailyLogs.filter { it.batchId in openBatchIds }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Daily Entry (Supervisor)",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "Hatch date starts at Day 0, next day Day 1 automatically. Closed batches are hidden from Daily Entry.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (openBatches.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Pets,
          title = "No Active Farms Assigned",
          description = "No active or closing batches are currently open for daily entry.",
        )
      }
    } else {
      items(openBatches, key = { "u_bch_${it.id}" }) { batch ->
        val autoAge = batch.currentAgeDays(todayEpochDay)
        val farmTitle = farmerById[batch.farmerId]?.displayFarmName ?: batch.farmerName
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onLogDailyClick(batch) }
            .testTag("user_daily_batch_${batch.batchCode}"),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${batch.farmCode} • $farmTitle",
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${batch.batchCode} • ${batch.farmerName} • Hatch ${formatEpochDay(batch.placementEpochDay)}",
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
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Auto Age: Day $autoAge",
                  style = MonoMetricMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "${formatNumber(batch.survivingBirds)} live • ${batch.latestAvgWeightGrams.toInt()} g",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(
                onClick = { onLogDailyClick(batch) },
                modifier = Modifier.testTag("user_log_btn_${batch.batchCode}"),
              ) {
                Icon(Icons.Rounded.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Daily Entry")
              }
            }
          }
        }
      }
    }

    if (activeDailyLogs.isNotEmpty()) {
      item {
        Text(
          text = "Recent Open Farm Entries (${activeDailyLogs.size})",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 6.dp),
        )
      }
      items(activeDailyLogs.take(20), key = { "u_log_${it.id}" }) { log ->
        val matchedBatch = openBatches.firstOrNull { it.id == log.batchId }
        val farmLabel = matchedBatch?.let {
          val farmName = farmerById[it.farmerId]?.displayFarmName ?: it.farmerName
          "${it.farmCode} • $farmName (${it.batchCode})"
        } ?: log.batchId
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                text = "$farmLabel • Day ${log.dayNumber}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = formatEpochDay(log.logEpochDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
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
          }
        }
      }
    }
  }
}

@Composable
fun UserFeedDispatchScreen(
  farmers: List<Farmer>,
  batches: List<FlockBatch>,
  feedDispatches: List<FeedDispatch>,
  todayEpochDay: Long,
  onAddFeedDispatchClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val farmerById = farmers.associateBy { it.id }
  val openBatches = batches.filter { batch ->
    val linkedFarmer = farmerById[batch.farmerId]
    batch.statusEnum != BatchStatus.CLOSED &&
      linkedFarmer?.statusEnum != FarmStatus.EMPTY &&
      linkedFarmer?.statusEnum != FarmStatus.CANCELLED
  }
  val todayBags = feedDispatches.filter { it.dispatchEpochDay == todayEpochDay }.sumOf { it.bagsCount }
  val totalBags = feedDispatches.sumOf { it.bagsCount }
  val totalKg = feedDispatches.sumOf { it.weightKg }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Feed Dispatch Entry",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Record feed type, quantity & destination farm • Synced to cloud",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onAddFeedDispatchClick,
          enabled = openBatches.isNotEmpty(),
          modifier = Modifier.testTag("user_new_feed_dispatch_btn"),
        ) {
          Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(6.dp))
          Text("Dispatch")
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KmSummaryChip("Today's Bags", "$todayBags bags", Modifier.weight(1f))
        KmSummaryChip("Total Dispatches", "${feedDispatches.size}", Modifier.weight(1f))
        KmSummaryChip("Total Issued", "${formatDecimal(totalKg, 0)} kg ($totalBags b)", Modifier.weight(1f))
      }
    }

    if (feedDispatches.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Inventory2,
          title = "No Feed Dispatches Recorded",
          description = if (openBatches.isNotEmpty()) {
            "Record feed type, bag/kg quantity, and destination farm to sync challans with Firestore."
          } else {
            "An Active or Closing batch is required before dispatching feed."
          },
          actionLabel = if (openBatches.isNotEmpty()) "Record Feed Dispatch" else null,
          onAction = if (openBatches.isNotEmpty()) onAddFeedDispatchClick else null,
        )
      }
    } else {
      items(feedDispatches, key = { "u_fd_${it.id}" }) { dispatch ->
        val matchedBatch = batches.firstOrNull { it.id == dispatch.batchId }
        val farmLabel = dispatch.resolvedDestinationFarm.ifBlank {
          matchedBatch?.let { "${it.farmCode} • ${it.displayFarmName}" } ?: dispatch.farmerName
        }
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${dispatch.feedType} • ${dispatch.challanNo}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              )
              Text(
                text = "${dispatch.bagsCount} bags (${formatDecimal(dispatch.weightKg, 0)} kg)",
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = "To: $farmLabel (${dispatch.batchCode})",
              style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              text = listOfNotNull(
                formatEpochDay(dispatch.dispatchEpochDay),
                dispatch.vehicleNo.takeIf { it.isNotBlank() }?.let { "Veh $it" },
                dispatch.remarks.takeIf { it.isNotBlank() },
              ).joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun UserFeedTransferScreen(
  batches: List<FlockBatch>,
  feedTransfers: List<FeedTransfer>,
  onAddFeedTransferClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val openBatches = batches.filter { it.statusEnum != BatchStatus.CLOSED }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Inter-Feed Transfer",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Transfer feed bags between active sheds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onAddFeedTransferClick,
          enabled = openBatches.size >= 2,
          modifier = Modifier.testTag("user_new_transfer_btn"),
        ) {
          Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(6.dp))
          Text("New Transfer")
        }
      }
    }

    if (feedTransfers.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.SwapHoriz,
          title = "No Inter-Feed Transfers Yet",
          description = if (openBatches.size >= 2) {
            "Record bag transfers between farms when shifting leftover feed bags."
          } else {
            "At least two active/closing batches are required for inter-feed transfers."
          },
          actionLabel = if (openBatches.size >= 2) "Record Transfer" else null,
          onAction = if (openBatches.size >= 2) onAddFeedTransferClick else null,
        )
      }
    } else {
      items(feedTransfers, key = { "u_ift_${it.id}" }) { tr ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${tr.transferRef} • ${tr.feedType}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              )
              Text(
                text = "${tr.bagsCount} bags (${formatDecimal(tr.weightKg, 0)} kg)",
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = "From: ${tr.fromFarmCode} (${tr.fromBatchCode}) → To: ${tr.toFarmCode} (${tr.toBatchCode})",
              style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              text = formatEpochDay(tr.transferEpochDay) + if (tr.remarks.isNotBlank()) " • ${tr.remarks}" else "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun UserKmClaimScreen(
  activeOperator: OperatorAccount,
  kmEntries: List<KmDailyEntry>,
  kmClaims: List<KmClaim>,
  todayEpochDay: Long,
  onAddDailyKmClick: () -> Unit,
  onSubmitMonthlyClaim: (monthKey: String, ratePerKm: Double, remarks: String) -> Unit,
  onDownloadKmClaimPdf: (KmClaim) -> Unit,
  onShareKmClaimPdf: (KmClaim) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentMonthKey = KmMonthHelper.monthKeyFromEpochDay(todayEpochDay)
  var selectedMonthKey by remember { mutableStateOf(currentMonthKey) }
  var rateText by remember { mutableStateOf("4.0") }
  var claimRemarks by remember { mutableStateOf("") }

  val myEntries = kmEntries.filter {
    it.operatorId == activeOperator.id || it.empCode.equals(activeOperator.empCode, ignoreCase = true)
  }
  val myClaims = kmClaims.filter {
    it.operatorId == activeOperator.id || it.empCode.equals(activeOperator.empCode, ignoreCase = true)
  }

  val monthLabel = KmMonthHelper.monthLabelFromKey(selectedMonthKey)
  val monthEntries = myEntries
    .filter { it.resolvedMonthKey == selectedMonthKey }
    .sortedByDescending { it.entryEpochDay }
  val monthTotalKm = monthEntries.sumOf { it.totalKm }
  val monthClaim = myClaims.firstOrNull { it.resolvedMonthKey == selectedMonthKey }

  val ratePerKm = rateText.toDoubleOrNull() ?: -1.0
  val projectedAmount = monthTotalKm * ratePerKm.coerceAtLeast(0.0)
  val statusColors = LocalStatusColors.current

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Daily KM & Monthly Claim",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "${activeOperator.empCode} • Log KM daily, claim once a month",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onAddDailyKmClick,
          modifier = Modifier.testTag("user_add_km_entry_btn"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("KM Entry")
        }
      }
    }

    // Month switcher
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { selectedMonthKey = KmMonthHelper.shiftMonthKey(selectedMonthKey, -1) },
            modifier = Modifier.testTag("km_prev_month_btn"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = monthLabel,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "${monthEntries.size} daily log(s) • ${formatDecimal(monthTotalKm, 1)} km",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            enabled = selectedMonthKey < currentMonthKey,
            onClick = { selectedMonthKey = KmMonthHelper.shiftMonthKey(selectedMonthKey, 1) },
            modifier = Modifier.testTag("km_next_month_btn"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
          }
        }
      }
    }

    // Monthly Claim Status / Submit Card (Monthly Once)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (monthClaim == null) {
            Text(
              text = "Monthly Claim • $monthLabel (Submit Once)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              KmSummaryChip("Daily Entries", monthEntries.size.toString(), Modifier.weight(1f))
              KmSummaryChip("Month Total KM", "${formatDecimal(monthTotalKm, 1)} km", Modifier.weight(1f))
              KmSummaryChip("Projected Claim", formatCurrency(projectedAmount), Modifier.weight(1f))
            }
            if (monthEntries.isNotEmpty()) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = rateText,
                  onValueChange = { rateText = it },
                  label = { Text("Rate ₹/km *") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.weight(0.4f).testTag("monthly_claim_rate_input"),
                )
                OutlinedTextField(
                  value = claimRemarks,
                  onValueChange = { claimRemarks = it },
                  label = { Text("Note (Optional)") },
                  singleLine = true,
                  modifier = Modifier.weight(0.6f).testTag("monthly_claim_note_input"),
                )
              }
            } else {
              Text(
                text = "Log your daily start & end odometer KM readings during $monthLabel. At month end (or after logging), submit the monthly claim once here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = {
                onSubmitMonthlyClaim(selectedMonthKey, ratePerKm.coerceAtLeast(0.0), claimRemarks)
              },
              enabled = monthEntries.isNotEmpty() && ratePerKm >= 0.0,
              modifier = Modifier.fillMaxWidth().testTag("submit_monthly_claim_btn"),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Submit Monthly Claim ($monthLabel)")
            }
          } else {
            val (bg, fg) = when (monthClaim.statusEnum) {
              ClaimStatus.PENDING -> statusColors.closingBg to statusColors.closingFg
              ClaimStatus.APPROVED -> statusColors.activeBg to statusColors.activeFg
              ClaimStatus.REJECTED -> statusColors.cancelledBg to statusColors.cancelledFg
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "$monthLabel Monthly Claim",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                  text = "Already claimed for $monthLabel (Monthly once limit)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              StatusPill(text = monthClaim.statusEnum.label, background = bg, foreground = fg)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              KmSummaryChip("Logs Claimed", "${monthClaim.dailyEntriesCount}", Modifier.weight(1f))
              KmSummaryChip("Total KM", "${formatDecimal(monthClaim.totalKm, 1)} km", Modifier.weight(1f))
              KmSummaryChip("Claim Amt", formatCurrency(monthClaim.claimAmount), Modifier.weight(1f))
            }
            Text(
              text = "Rate: ₹${formatDecimal(monthClaim.ratePerKm, 1)}/km • Submitted ${formatEpochDay(monthClaim.claimEpochDay)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (monthClaim.visitedFarms.isNotBlank()) {
              Text(
                text = "Route / Note: ${monthClaim.visitedFarms}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (monthClaim.statusEnum != ClaimStatus.PENDING && monthClaim.reviewedByEmpCode.isNotBlank()) {
              Text(
                text = "${monthClaim.statusEnum.label} by ${monthClaim.reviewedByName} (${monthClaim.reviewedByEmpCode})" +
                  if (monthClaim.reviewRemarks.isNotBlank()) " — ${monthClaim.reviewRemarks}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = fg,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { onDownloadKmClaimPdf(monthClaim) },
                modifier = Modifier.weight(1f).testTag("user_download_km_claim_pdf_btn"),
              ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Download Claim PDF")
              }
              OutlinedButton(
                onClick = { onShareKmClaimPdf(monthClaim) },
                modifier = Modifier.weight(1f).testTag("user_share_km_claim_pdf_btn"),
              ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Share")
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Daily KM Logs • $monthLabel (${monthEntries.size})",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    if (monthEntries.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.DirectionsCar,
          title = "No Daily KM Entries in $monthLabel",
          description = "Tap KM Entry to record today's start and end odometer reading.",
          actionLabel = "Add Daily KM Entry",
          onAction = onAddDailyKmClick,
        )
      }
    } else {
      items(monthEntries, key = { "u_kme_${it.id}" }) { entry ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = formatEpochDay(entry.entryEpochDay),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              )
              Text(
                text = "${entry.startKm.toInt()} → ${entry.endKm.toInt()} km",
                style = MaterialTheme.typography.bodySmall,
              )
              if (entry.visitedFarms.isNotBlank()) {
                Text(
                  text = "Visited: ${entry.visitedFarms}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = "${formatDecimal(entry.totalKm, 1)} km",
              style = MonoMetricMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun KmSummaryChip(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
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
        style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
