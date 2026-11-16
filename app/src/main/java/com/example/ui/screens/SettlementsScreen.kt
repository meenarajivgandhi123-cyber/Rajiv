package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.ClaimStatus
import com.example.data.model.DailyLog
import com.example.data.model.FarmLedgerCalculator
import com.example.data.model.FarmLedgerData
import com.example.data.model.Farmer
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedTransfer
import com.example.data.model.FeedType
import com.example.data.model.FlockBatch
import com.example.data.model.IntegrationZone
import com.example.data.model.KmClaim
import com.example.data.model.KmDailyEntry
import com.example.data.model.MedicineEntry
import com.example.data.model.SettlementCalculator
import com.example.data.model.ZoneDailyReportSummary
import com.example.data.model.ZoneReportCalculator
import com.example.ui.components.BatchStatusBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDecimal
import com.example.ui.components.formatEpochDay
import com.example.ui.components.formatEpochDayDdMmYyyy
import com.example.ui.components.formatNumber
import com.example.ui.components.parseDdMmYyyy
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall

enum class SettlementHubTab(val label: String) {
  GC_CLOSED("GC Closed PDFs"),
  ZONE_REPORT("Zone Daily PDF"),
  FARM_LEDGER("Farm Ledger PDF"),
  FEED_ENTRY("Feed Entry"),
  MEDICINE_ENTRY("Medicine"),
  INTER_FEED("Inter-Feed"),
  BIRD_SALES("Birds Sales"),
  KM_CLAIMS("KM Claims"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementsScreen(
  zones: List<IntegrationZone>,
  farmers: List<Farmer>,
  batches: List<FlockBatch>,
  dailyLogs: List<DailyLog>,
  feedDispatches: List<FeedDispatch>,
  birdSales: List<BirdSale>,
  feedTransfers: List<FeedTransfer>,
  medicineEntries: List<MedicineEntry>,
  kmEntries: List<KmDailyEntry>,
  kmClaims: List<KmClaim>,
  todayEpochDay: Long,
  canEditOrDelete: Boolean,
  canSelectPreviousDate: Boolean,
  selectedTab: SettlementHubTab,
  onTabChange: (SettlementHubTab) -> Unit,
  onOpenBatch: (String) -> Unit,
  onOpenClosingDialog: (FlockBatch) -> Unit,
  onDownloadGcPdf: (FlockBatch) -> Unit,
  onShareGcPdf: (FlockBatch) -> Unit,
  onDownloadZonePdf: (ZoneDailyReportSummary) -> Unit,
  onShareZonePdf: (ZoneDailyReportSummary) -> Unit,
  onDownloadFarmLedgerPdf: (FarmLedgerData) -> Unit,
  onShareFarmLedgerPdf: (FarmLedgerData) -> Unit,
  onAddFeedDispatchClick: () -> Unit,
  onEditFeedDispatchClick: (FeedDispatch) -> Unit,
  onDeleteFeedDispatchClick: (FeedDispatch) -> Unit,
  onAddMedicineEntryClick: () -> Unit,
  onEditMedicineEntryClick: (MedicineEntry) -> Unit,
  onDeleteMedicineEntryClick: (MedicineEntry) -> Unit,
  onAddFeedTransferClick: () -> Unit,
  onEditFeedTransferClick: (FeedTransfer) -> Unit,
  onDeleteFeedTransferClick: (FeedTransfer) -> Unit,
  onAddBirdSaleClick: () -> Unit,
  onEditBirdSaleClick: (BirdSale) -> Unit,
  onDeleteBirdSaleClick: (BirdSale) -> Unit,
  onAddDailyKmClick: () -> Unit,
  onEditDailyKmEntry: (KmDailyEntry) -> Unit,
  onDeleteDailyKmEntry: (KmDailyEntry) -> Unit,
  onApproveKmClaim: (KmClaim) -> Unit,
  onRejectKmClaim: (KmClaim) -> Unit,
  onEditMonthlyKmClaim: (KmClaim) -> Unit,
  onDeleteKmClaim: (KmClaim) -> Unit,
  onDownloadKmClaimPdf: (KmClaim) -> Unit,
  onShareKmClaimPdf: (KmClaim) -> Unit,
  onDownloadAllKmClaimsPdf: (List<KmClaim>, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val settlementBatches = batches.filter {
    it.statusEnum == BatchStatus.CLOSING || it.statusEnum == BatchStatus.CLOSED
  }
  val pendingClaimsCount = kmClaims.count { it.statusEnum == ClaimStatus.PENDING }

  var selectedZoneId by remember { mutableStateOf<String?>(zones.firstOrNull()?.id) }
  var zoneMenuExpanded by remember { mutableStateOf(false) }
  var reportEpochDay by remember { mutableLongStateOf(todayEpochDay) }
  var reportDateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(todayEpochDay)) }
  var selectedLedgerFarmId by remember { mutableStateOf<String?>(farmers.firstOrNull()?.id) }
  var ledgerFarmMenuExpanded by remember { mutableStateOf(false) }
  var selectedLedgerBatchId by remember { mutableStateOf<String?>(null) }
  var ledgerBatchMenuExpanded by remember { mutableStateOf(false) }
  var claimFilter by remember { mutableStateOf<ClaimStatus?>(null) }
  var showDailyKmLogsMode by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SettlementHubTab.entries.forEach { tab ->
        val badge = when (tab) {
          SettlementHubTab.GC_CLOSED -> settlementBatches.size
          SettlementHubTab.ZONE_REPORT -> zones.size
          SettlementHubTab.FARM_LEDGER -> farmers.size
          SettlementHubTab.FEED_ENTRY -> feedDispatches.size
          SettlementHubTab.MEDICINE_ENTRY -> medicineEntries.size
          SettlementHubTab.INTER_FEED -> feedTransfers.size
          SettlementHubTab.BIRD_SALES -> birdSales.size
          SettlementHubTab.KM_CLAIMS -> pendingClaimsCount
        }
        FilterChip(
          selected = selectedTab == tab,
          onClick = { onTabChange(tab) },
          label = { Text("${tab.label} ($badge)") },
          modifier = Modifier.testTag("hub_tab_${tab.name.lowercase()}"),
        )
      }
    }

    when (selectedTab) {
      SettlementHubTab.GC_CLOSED -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text("GC Closing & Closed Statements", style = MaterialTheme.typography.headlineSmall)
          }
          if (settlementBatches.isEmpty()) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.CurrencyRupee,
                title = "No Closing or GC Closed Batches",
                description = "Move an active batch to Closing Farm or mark it GC Closed to generate grower settlement PDFs.",
              )
            }
          } else {
            items(settlementBatches, key = { "stl_${it.id}" }) { batch ->
              SettlementBatchCard(
                batch = batch,
                canEditOrDelete = canEditOrDelete,
                onClick = { onOpenBatch(batch.id) },
                onEditGc = { onOpenClosingDialog(batch) },
                onDownloadPdf = { onDownloadGcPdf(batch) },
                onSharePdf = { onShareGcPdf(batch) },
              )
            }
          }
        }
      }

      SettlementHubTab.ZONE_REPORT -> {
        val activeZone = zones.firstOrNull { it.id == selectedZoneId }
        val effectiveReportDay = if (canSelectPreviousDate) reportEpochDay else todayEpochDay
        val reportSummary = ZoneReportCalculator.buildZoneDailyReport(
          zone = activeZone,
          zoneLabelFallback = activeZone?.zoneName ?: "ALL ZONES",
          regionFallback = activeZone?.region ?: "ALL REGIONS",
          farmers = farmers,
          batches = batches,
          dailyLogs = dailyLogs,
          feedDispatches = feedDispatches,
          feedTransfers = feedTransfers,
          reportEpochDay = effectiveReportDay,
        )

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text("Zone-Wise Daily Report PDF", style = MaterialTheme.typography.headlineSmall)
          }
          item {
            Card(
              shape = MaterialTheme.shapes.medium,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                ExposedDropdownMenuBox(
                  expanded = zoneMenuExpanded,
                  onExpandedChange = { zoneMenuExpanded = it },
                ) {
                  OutlinedTextField(
                    value = activeZone?.let { "${it.zoneCode} • ${it.zoneName} (${it.region})" } ?: "All Zones Combined",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Zone") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneMenuExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                  )
                  ExposedDropdownMenu(
                    expanded = zoneMenuExpanded,
                    onDismissRequest = { zoneMenuExpanded = false },
                  ) {
                    DropdownMenuItem(
                      text = { Text("All Zones Combined") },
                      onClick = {
                        selectedZoneId = null
                        zoneMenuExpanded = false
                      },
                    )
                    zones.forEach { z ->
                      DropdownMenuItem(
                        text = { Text("${z.zoneCode} • ${z.zoneName} (${z.region})") },
                        onClick = {
                          selectedZoneId = z.id
                          zoneMenuExpanded = false
                        },
                      )
                    }
                  }
                }

                if (canSelectPreviousDate) {
                  val dateValid = parseDdMmYyyy(reportDateText)?.let { it in 18000L..todayEpochDay } == true
                  OutlinedTextField(
                    value = reportDateText,
                    onValueChange = { raw ->
                      reportDateText = raw
                      parseDdMmYyyy(raw)?.let { parsed ->
                        if (parsed in 18000L..todayEpochDay) reportEpochDay = parsed
                      }
                    },
                    label = { Text("Report Date (DD-MM-YYYY)") },
                    isError = !dateValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                  )
                } else {
                  Text(
                    text = "Report Date: ${formatEpochDay(todayEpochDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = { onDownloadZonePdf(reportSummary) },
                    enabled = reportSummary.rows.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("download_zone_pdf_btn"),
                  ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Download PDF")
                  }
                  OutlinedButton(
                    onClick = { onShareZonePdf(reportSummary) },
                    enabled = reportSummary.rows.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("share_zone_pdf_btn"),
                  ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Open / Share")
                  }
                }
              }
            }
          }

          if (reportSummary.rows.isEmpty()) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.PictureAsPdf,
                title = "No Active Farms in Selected Zone",
                description = "Assign Active or Closing farms to ${reportSummary.zoneName} to populate the daily PDF sheet.",
              )
            }
          } else {
            item {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text("${reportSummary.rows.size} Farms", style = MaterialTheme.typography.titleSmall)
                    Text("Strength: ${formatNumber(reportSummary.totalStrength)}", style = MonoMetricSmall)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      "Mort: ${reportSummary.totalDailyMort} (${formatDecimal(reportSummary.overallDailyMortPercent, 2)}%)",
                      style = MonoMetricSmall,
                      color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                      "Bal Birds: ${formatNumber(reportSummary.totalBalanceBirds)} • Feed: ${formatNumber(reportSummary.totalReceivedFeedBags)} bags",
                      style = MonoMetricSmall,
                    )
                  }
                }
              }
            }
            items(reportSummary.rows, key = { "zr_${it.farmCode}_${it.batchCode}" }) { row ->
              Card(
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "${row.serialNo}. ${row.farmCode} • ${row.farmName}",
                      style = MaterialTheme.typography.titleSmall,
                      modifier = Modifier.weight(1f),
                    )
                    Text("Age ${row.ageDays}d", style = MonoMetricSmall, color = MaterialTheme.colorScheme.primary)
                  }
                  Text(
                    text = "Bal: ${row.balanceBirds} • Mort: ${row.dailyMort} (${formatDecimal(row.dailyMortPercent, 2)}%) • Recd: ${row.receivedFeedBags}b • Intake: ${formatDecimal(row.feedIntakeBags, 1)}b • ABW: ${formatDecimal(row.bodyWeightKg, 2)}kg • FCR: ${formatDecimal(row.fcr, 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      SettlementHubTab.FARM_LEDGER -> {
        val activeFarmer = farmers.firstOrNull { it.id == selectedLedgerFarmId } ?: farmers.firstOrNull()
        val farmBatches = if (activeFarmer != null) {
          batches.filter { it.farmerId == activeFarmer.id }.sortedByDescending { it.placementEpochDay }
        } else {
          emptyList()
        }
        val activeSpecificBatch = farmBatches.firstOrNull { it.id == selectedLedgerBatchId }
        val ledgerData = activeFarmer?.let { farmer ->
          FarmLedgerCalculator.build(
            farmer = farmer,
            allBatches = batches,
            dailyLogs = dailyLogs,
            feedDispatches = feedDispatches,
            medicineEntries = medicineEntries,
            feedTransfers = feedTransfers,
            birdSales = birdSales,
            specificBatch = activeSpecificBatch,
          )
        }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text("Farm-Wise Ledger PDF", style = MaterialTheme.typography.headlineSmall)
          }
          if (activeFarmer == null || ledgerData == null) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.Agriculture,
                title = "No Registered Farms",
                description = "Register a farmer shed first to generate and download Farm-Wise Ledger PDFs.",
              )
            }
          } else {
            item {
              Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ExposedDropdownMenuBox(
                    expanded = ledgerFarmMenuExpanded,
                    onExpandedChange = { ledgerFarmMenuExpanded = it },
                  ) {
                    OutlinedTextField(
                      value = "${activeFarmer.farmCode} • ${activeFarmer.displayFarmName} (${activeFarmer.farmerName})",
                      onValueChange = {},
                      readOnly = true,
                      label = { Text("Select Farm *") },
                      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ledgerFarmMenuExpanded) },
                      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                      expanded = ledgerFarmMenuExpanded,
                      onDismissRequest = { ledgerFarmMenuExpanded = false },
                    ) {
                      farmers.forEach { f ->
                        DropdownMenuItem(
                          text = { Text("${f.farmCode} • ${f.displayFarmName} (${f.farmerName})") },
                          onClick = {
                            selectedLedgerFarmId = f.id
                            selectedLedgerBatchId = null
                            ledgerFarmMenuExpanded = false
                          },
                        )
                      }
                    }
                  }

                  ExposedDropdownMenuBox(
                    expanded = ledgerBatchMenuExpanded,
                    onExpandedChange = { ledgerBatchMenuExpanded = it },
                  ) {
                    OutlinedTextField(
                      value = activeSpecificBatch?.let {
                        "${it.batchCode} (${it.statusEnum.label} • Hatch ${formatEpochDay(it.placementEpochDay)})"
                      } ?: "All Batches on Farm (${farmBatches.size})",
                      onValueChange = {},
                      readOnly = true,
                      label = { Text("Batch Scope") },
                      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ledgerBatchMenuExpanded) },
                      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                      expanded = ledgerBatchMenuExpanded,
                      onDismissRequest = { ledgerBatchMenuExpanded = false },
                    ) {
                      DropdownMenuItem(
                        text = { Text("All Batches on Farm (${farmBatches.size})") },
                        onClick = {
                          selectedLedgerBatchId = null
                          ledgerBatchMenuExpanded = false
                        },
                      )
                      farmBatches.forEach { b ->
                        DropdownMenuItem(
                          text = { Text("${b.batchCode} (${b.statusEnum.label} • Hatch ${formatEpochDay(b.placementEpochDay)})") },
                          onClick = {
                            selectedLedgerBatchId = b.id
                            ledgerBatchMenuExpanded = false
                          },
                        )
                      }
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = { onDownloadFarmLedgerPdf(ledgerData) },
                      modifier = Modifier.weight(1f).testTag("download_farm_ledger_pdf_btn"),
                    ) {
                      Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.size(4.dp))
                      Text("Download Ledger PDF")
                    }
                    OutlinedButton(
                      onClick = { onShareFarmLedgerPdf(ledgerData) },
                      modifier = Modifier.weight(1f).testTag("share_farm_ledger_pdf_btn"),
                    ) {
                      Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.size(4.dp))
                      Text("Open / Share")
                    }
                  }
                }
              }
            }

            item {
              Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "Cumulative Farm Ledger Preview • ${activeFarmer.farmCode}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  )
                  LedgerRow("Placed Chicks (+Free)", "${formatNumber(ledgerData.totalChicksPlaced)} (+${formatNumber(ledgerData.totalFreeChicks)})")
                  LedgerRow("Mortality / Surviving", "${formatNumber(ledgerData.totalMortality)} / ${formatNumber(ledgerData.totalSurvivingBirds)}")
                  LedgerRow("Feed Dispatched / Consumed", "${formatNumber(ledgerData.totalFeedDispatchedBags)} bags / ${formatDecimal(ledgerData.totalFeedConsumedKg, 0)} kg")
                  LedgerRow("Inter-Feed Net", "+${ledgerData.totalFeedTransferredInBags}b in / -${ledgerData.totalFeedTransferredOutBags}b out")
                  LedgerRow("Medicine Cost", formatCurrency(ledgerData.totalMedicineCost))
                  LedgerRow("Birds Sold (${formatNumber(ledgerData.totalBirdsSold)})", "${formatDecimal(ledgerData.totalBirdsSoldWeightKg, 1)} kg • ${formatCurrency(ledgerData.totalBirdsSaleAmount)}")
                  LedgerRow("GC Settlement Payable", formatCurrency(ledgerData.totalSettlementPayable))
                  LedgerRow(
                    "Statement Entries",
                    "${ledgerData.dailyLogs.size} logs • ${ledgerData.feedDispatches.size} feed • ${ledgerData.medicineEntries.size} med • ${ledgerData.feedTransfers.size} IFT • ${ledgerData.birdSales.size} sales",
                  )
                }
              }
            }

            if (ledgerData.batches.isNotEmpty()) {
              items(ledgerData.batches, key = { "fl_b_${it.id}" }) { batch ->
                Card(
                  shape = MaterialTheme.shapes.small,
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onOpenBatch(batch.id) },
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${batch.batchCode} • Hatch ${formatEpochDay(batch.placementEpochDay)}",
                        style = MaterialTheme.typography.titleSmall,
                      )
                      Text(
                        text = "Placed ${formatNumber(batch.totalBirdsStarted)} • Mort ${batch.cumulativeMortality} • Sold ${formatNumber(batch.liftedBirds)} (${formatDecimal(batch.liftedWeightKg, 1)}kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    BatchStatusBadge(status = batch.statusEnum)
                  }
                }
              }
            }
          }
        }
      }

      SettlementHubTab.FEED_ENTRY -> {
        var feedSearchQuery by remember { mutableStateOf("") }
        var selectedFeedTypeFilter by remember { mutableStateOf<String?>(null) }
        val filteredDispatches = feedDispatches.filter { dispatch ->
          val matchesType = selectedFeedTypeFilter == null ||
            dispatch.feedType.equals(selectedFeedTypeFilter, ignoreCase = true)
          val query = feedSearchQuery.trim().lowercase()
          val matchesQuery = query.isEmpty() ||
            dispatch.challanNo.lowercase().contains(query) ||
            dispatch.farmerName.lowercase().contains(query) ||
            dispatch.farmName.lowercase().contains(query) ||
            dispatch.farmCode.lowercase().contains(query) ||
            dispatch.batchCode.lowercase().contains(query) ||
            dispatch.vehicleNo.lowercase().contains(query)
          matchesType && matchesQuery
        }
        val totalBags = filteredDispatches.sumOf { it.bagsCount }
        val totalWeightKg = filteredDispatches.sumOf { it.weightKg }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
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
                Text("Feed Dispatch Ledger", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = "${filteredDispatches.size} dispatches • $totalBags bags • ${formatDecimal(totalWeightKg, 0)} kg",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(
                onClick = onAddFeedDispatchClick,
                modifier = Modifier.testTag("hub_add_feed_btn"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Feed Dispatch")
              }
            }
          }
          item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = feedSearchQuery,
                onValueChange = { feedSearchQuery = it },
                placeholder = { Text("Search destination farm, challan, vehicle or batch...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search feed dispatches") },
                trailingIcon = {
                  if (feedSearchQuery.isNotBlank()) {
                    IconButton(onClick = { feedSearchQuery = "" }) {
                      Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("hub_feed_search_input"),
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = selectedFeedTypeFilter == null,
                  onClick = { selectedFeedTypeFilter = null },
                  label = { Text("All (${feedDispatches.size})") },
                )
                FeedType.entries.forEach { ft ->
                  val count = feedDispatches.count { it.feedType.equals(ft.value, ignoreCase = true) }
                  FilterChip(
                    selected = selectedFeedTypeFilter == ft.value,
                    onClick = {
                      selectedFeedTypeFilter = if (selectedFeedTypeFilter == ft.value) null else ft.value
                    },
                    label = { Text("${ft.value} ($count)") },
                  )
                }
              }
            }
          }
          if (filteredDispatches.isEmpty()) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.Inventory2,
                title = if (feedDispatches.isEmpty()) "No Feed Dispatches" else "No Matching Dispatches",
                description = "Record feed type, bags/kg quantity, destination farm, and vehicle details.",
              )
            }
          } else {
            items(filteredDispatches, key = { "fd_${it.id}" }) { dispatch ->
              val destinationLabel = dispatch.resolvedDestinationFarm.ifBlank { dispatch.farmerName }
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${dispatch.feedType} • ${dispatch.challanNo}", style = MaterialTheme.typography.titleSmall)
                    Text(
                      "To: $destinationLabel (${dispatch.batchCode})",
                      style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                      listOfNotNull(
                        formatEpochDay(dispatch.dispatchEpochDay),
                        dispatch.vehicleNo.takeIf { it.isNotBlank() }?.let { "Veh $it" },
                        dispatch.remarks.takeIf { it.isNotBlank() },
                      ).joinToString(" • "),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                      Text("${dispatch.bagsCount} bags", style = MonoMetricMedium, color = MaterialTheme.colorScheme.primary)
                      Text("${formatDecimal(dispatch.weightKg, 0)} kg", style = MonoMetricSmall)
                    }
                    if (canEditOrDelete) {
                      IconButton(onClick = { onEditFeedDispatchClick(dispatch) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit feed dispatch", modifier = Modifier.size(18.dp))
                      }
                      IconButton(onClick = { onDeleteFeedDispatchClick(dispatch) }) {
                        Icon(
                          Icons.Rounded.DeleteOutline,
                          contentDescription = "Delete feed dispatch",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(18.dp),
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

      SettlementHubTab.MEDICINE_ENTRY -> {
        val totalMedicineSpend = medicineEntries.sumOf { it.totalCost }
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
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
                Text("Medicine Entry Ledger", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = "${medicineEntries.size} entries • Total ${formatCurrency(totalMedicineSpend)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (canEditOrDelete) {
                Button(
                  onClick = onAddMedicineEntryClick,
                  modifier = Modifier.testTag("hub_add_medicine_btn"),
                ) {
                  Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(4.dp))
                  Text("Medicine")
                }
              }
            }
          }
          if (medicineEntries.isEmpty()) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.Medication,
                title = "No Medicine Entries",
                description = "Record vaccines, antibiotics, supplements, and sanitizers issued to batches for automatic GC cost tracking.",
              )
            }
          } else {
            items(medicineEntries, key = { "med_${it.id}" }) { med ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${med.medicineName} • ${med.category}", style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${med.farmCode} (${med.batchCode}) • ${med.invoiceOrSlipNo} • ${formatEpochDay(med.entryEpochDay)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (med.dosageOrQty.isNotBlank() || med.remarks.isNotBlank()) {
                      Text(
                        listOfNotNull(
                          med.dosageOrQty.takeIf { it.isNotBlank() },
                          med.remarks.takeIf { it.isNotBlank() },
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = formatCurrency(med.totalCost),
                      style = MonoMetricMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    if (canEditOrDelete) {
                      IconButton(onClick = { onEditMedicineEntryClick(med) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit medicine entry", modifier = Modifier.size(18.dp))
                      }
                      IconButton(onClick = { onDeleteMedicineEntryClick(med) }) {
                        Icon(
                          Icons.Rounded.DeleteOutline,
                          contentDescription = "Delete medicine entry",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(18.dp),
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

      SettlementHubTab.INTER_FEED -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Inter-Feed Transfer Ledger", style = MaterialTheme.typography.headlineSmall)
              Button(
                onClick = onAddFeedTransferClick,
                modifier = Modifier.testTag("hub_add_transfer_btn"),
              ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Transfer")
              }
            }
          }
          if (feedTransfers.isEmpty()) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.SwapHoriz,
                title = "No Inter-Feed Transfers",
                description = "Track feed bags moved between active farms.",
              )
            }
          } else {
            items(feedTransfers, key = { "ift_${it.id}" }) { tr ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${tr.transferRef} • ${tr.feedType}", style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${tr.fromFarmCode} (${tr.fromBatchCode}) → ${tr.toFarmCode} (${tr.toBatchCode})",
                      style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                      formatEpochDay(tr.transferEpochDay) + if (tr.remarks.isNotBlank()) " • ${tr.remarks}" else "",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                      Text("${tr.bagsCount} bags", style = MonoMetricMedium, color = MaterialTheme.colorScheme.primary)
                      Text("${formatDecimal(tr.weightKg, 0)} kg", style = MonoMetricSmall)
                    }
                    if (canEditOrDelete) {
                      IconButton(onClick = { onEditFeedTransferClick(tr) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit transfer", modifier = Modifier.size(18.dp))
                      }
                      IconButton(onClick = { onDeleteFeedTransferClick(tr) }) {
                        Icon(
                          Icons.Rounded.DeleteOutline,
                          contentDescription = "Delete transfer",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(18.dp),
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

      SettlementHubTab.BIRD_SALES -> {
        var salesSearchQuery by remember { mutableStateOf("") }
        var todaySalesOnly by remember { mutableStateOf(false) }

        val todaySales = birdSales.filter { it.saleEpochDay == todayEpochDay }
        val todayBirds = todaySales.sumOf { it.birdsSold }
        val todayWeightKg = todaySales.sumOf { it.totalWeightKg }
        val todayAmount = todaySales.sumOf { it.saleAmount }

        val filteredSales = birdSales.filter { sale ->
          val matchesDate = !todaySalesOnly || sale.saleEpochDay == todayEpochDay
          val query = salesSearchQuery.trim().lowercase()
          val matchesQuery = query.isEmpty() ||
            sale.buyerName.lowercase().contains(query) ||
            sale.buyerPhone.lowercase().contains(query) ||
            sale.farmCode.lowercase().contains(query) ||
            sale.batchCode.lowercase().contains(query) ||
            sale.challanNo.lowercase().contains(query) ||
            sale.vehicleNo.lowercase().contains(query)
          matchesDate && matchesQuery
        }
        val filteredBirds = filteredSales.sumOf { it.birdsSold }
        val filteredWeight = filteredSales.sumOf { it.totalWeightKg }
        val filteredRevenue = filteredSales.sumOf { it.saleAmount }
        val filteredAvgRate = if (filteredWeight > 0.0) filteredRevenue / filteredWeight else 0.0

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
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
                Text("Daily Birds Sales", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = "${filteredSales.size} sales • ${formatNumber(filteredBirds)} birds • Avg ₹${formatDecimal(filteredAvgRate, 1)}/kg",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (canEditOrDelete) {
                Button(
                  onClick = onAddBirdSaleClick,
                  modifier = Modifier.testTag("hub_add_bird_sale_btn"),
                ) {
                  Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(4.dp))
                  Text("Birds Sale")
                }
              }
            }
          }
          item {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    "Today (${formatEpochDay(todayEpochDay)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    "${formatNumber(todayBirds)} birds • ${formatDecimal(todayWeightKg, 1)} kg",
                    style = MaterialTheme.typography.titleSmall,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(formatCurrency(todayAmount), style = MonoMetricMedium, color = MaterialTheme.colorScheme.primary)
                  Text("Total ${formatCurrency(filteredRevenue)}", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
          item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = salesSearchQuery,
                onValueChange = { salesSearchQuery = it },
                placeholder = { Text("Search customer, phone, slip no, vehicle or farm...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search bird sales") },
                trailingIcon = {
                  if (salesSearchQuery.isNotBlank()) {
                    IconButton(onClick = { salesSearchQuery = "" }) {
                      Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("hub_sales_search_input"),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                  selected = !todaySalesOnly,
                  onClick = { todaySalesOnly = false },
                  label = { Text("All Sales (${birdSales.size})") },
                )
                FilterChip(
                  selected = todaySalesOnly,
                  onClick = { todaySalesOnly = true },
                  label = { Text("Today (${todaySales.size})") },
                )
              }
            }
          }
          if (filteredSales.isEmpty()) {
            item {
              EmptyStateCard(
                icon = Icons.Rounded.Sell,
                title = if (birdSales.isEmpty()) "No Birds Sales Recorded" else "No Matching Bird Sales",
                description = "Record quantity sold, live weight, sale rate/kg, and customer contact details.",
              )
            }
          } else {
            items(filteredSales, key = { "bs_${it.id}" }) { sale ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val linkedBatch = batches.firstOrNull { it.id == sale.batchId }
                    val saleAge = sale.resolvedAgeDays(linkedBatch?.placementEpochDay)
                    val saleAvgWt = sale.resolvedAvgWeightKg()
                    val customerTitle = buildString {
                      append(sale.buyerName)
                      if (sale.buyerPhone.isNotBlank()) append(" (${sale.buyerPhone})")
                      append(" • ${sale.challanNo}")
                    }
                    Text(customerTitle, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${sale.farmCode} (${sale.batchCode}) • Age D$saleAge • ${formatEpochDay(sale.saleEpochDay)}" +
                        (if (saleAvgWt > 0.0) " • Avg ${formatDecimal(saleAvgWt, 3)} kg" else "") +
                        (if (sale.vehicleNo.isNotBlank()) " • ${sale.vehicleNo}" else ""),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (sale.remarks.isNotBlank()) {
                      Text(
                        sale.remarks,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        "${formatNumber(sale.birdsSold)} • ${formatDecimal(sale.totalWeightKg, 1)} kg",
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Text(
                        "${formatCurrency(sale.saleAmount)} @ ${formatDecimal(sale.ratePerKg, 1)}/kg",
                        style = MonoMetricSmall,
                      )
                    }
                    if (canEditOrDelete) {
                      IconButton(onClick = { onEditBirdSaleClick(sale) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit bird sale", modifier = Modifier.size(18.dp))
                      }
                      IconButton(onClick = { onDeleteBirdSaleClick(sale) }) {
                        Icon(
                          Icons.Rounded.DeleteOutline,
                          contentDescription = "Delete bird sale",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(18.dp),
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

      SettlementHubTab.KM_CLAIMS -> {
        val statusColors = LocalStatusColors.current
        val filteredClaims = kmClaims.filter { claimFilter == null || it.statusEnum == claimFilter }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
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
                Text("KM Logs & Monthly Claims", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = "$pendingClaimsCount pending • ${kmClaims.count { it.statusEnum == ClaimStatus.APPROVED }} approved • ${kmEntries.size} daily logs",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!showDailyKmLogsMode && filteredClaims.isNotEmpty()) {
                  OutlinedButton(
                    onClick = {
                      val label = claimFilter?.label ?: "All"
                      onDownloadAllKmClaimsPdf(filteredClaims, label)
                    },
                    modifier = Modifier.testTag("hub_download_all_km_claims_pdf_btn"),
                  ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("All PDF")
                  }
                }
                Button(
                  onClick = onAddDailyKmClick,
                  modifier = Modifier.testTag("hub_add_km_entry_btn"),
                ) {
                  Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(4.dp))
                  Text("KM Entry")
                }
              }
            }
          }

          item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = !showDailyKmLogsMode,
                onClick = { showDailyKmLogsMode = false },
                label = { Text("Monthly Claims (${kmClaims.size})") },
                modifier = Modifier.testTag("hub_km_monthly_claims_tab"),
              )
              FilterChip(
                selected = showDailyKmLogsMode,
                onClick = { showDailyKmLogsMode = true },
                label = { Text("Daily KM Logs (${kmEntries.size})") },
                modifier = Modifier.testTag("hub_km_daily_logs_tab"),
              )
            }
          }

          if (!showDailyKmLogsMode) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = claimFilter == null,
                  onClick = { claimFilter = null },
                  label = { Text("All (${kmClaims.size})") },
                )
                ClaimStatus.entries.forEach { st ->
                  val cnt = kmClaims.count { it.statusEnum == st }
                  FilterChip(
                    selected = claimFilter == st,
                    onClick = { claimFilter = st },
                    label = { Text("${st.label} ($cnt)") },
                  )
                }
              }
            }

            if (filteredClaims.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Rounded.DirectionsCar,
                  title = "No Monthly KM Claims Found",
                  description = "Field supervisors log daily odometer readings and submit a monthly travel claim once per month for approval.",
                )
              }
            } else {
              items(filteredClaims, key = { "kmc_${it.id}" }) { claim ->
                val (bg, fg) = when (claim.statusEnum) {
                  ClaimStatus.PENDING -> statusColors.closingBg to statusColors.closingFg
                  ClaimStatus.APPROVED -> statusColors.activeBg to statusColors.activeFg
                  ClaimStatus.REJECTED -> statusColors.cancelledBg to statusColors.cancelledFg
                }
                Card(
                  modifier = Modifier.fillMaxWidth(),
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
                          text = "${claim.empCode} • ${claim.operatorName}",
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                          text = "${claim.resolvedMonthLabel} • ${claim.dailyEntriesCount} daily logs (Submitted ${formatEpochDay(claim.claimEpochDay)})",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(text = claim.statusEnum.label, background = bg, foreground = fg)
                        if (canEditOrDelete) {
                          IconButton(onClick = { onEditMonthlyKmClaim(claim) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit monthly KM claim", modifier = Modifier.size(18.dp))
                          }
                          IconButton(onClick = { onDeleteKmClaim(claim) }) {
                            Icon(
                              Icons.Rounded.DeleteOutline,
                              contentDescription = "Delete monthly KM claim",
                              tint = MaterialTheme.colorScheme.error,
                              modifier = Modifier.size(18.dp),
                            )
                          }
                        }
                      }
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = "${formatDecimal(claim.totalKm, 1)} km @ ₹${formatDecimal(claim.ratePerKm, 1)}/km",
                        style = MaterialTheme.typography.bodyMedium,
                      )
                      Text(
                        text = formatCurrency(claim.claimAmount),
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    if (claim.visitedFarms.isNotBlank()) {
                      Text(
                        text = "Route / Note: ${claim.visitedFarms}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    if (claim.statusEnum != ClaimStatus.PENDING && claim.reviewedByEmpCode.isNotBlank()) {
                      Text(
                        text = "${claim.statusEnum.label} by ${claim.reviewedByName} (${claim.reviewedByEmpCode})" +
                          if (claim.reviewRemarks.isNotBlank()) " — ${claim.reviewRemarks}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = fg,
                      )
                    }
                    if (canEditOrDelete && claim.statusEnum == ClaimStatus.PENDING) {
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                          onClick = { onApproveKmClaim(claim) },
                          modifier = Modifier.weight(1f).testTag("approve_km_${claim.empCode}"),
                        ) {
                          Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.size(4.dp))
                          Text("Approve")
                        }
                        OutlinedButton(
                          onClick = { onRejectKmClaim(claim) },
                          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                          modifier = Modifier.weight(1f).testTag("reject_km_${claim.empCode}"),
                        ) {
                          Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.size(4.dp))
                          Text("Reject")
                        }
                      }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      OutlinedButton(
                        onClick = { onDownloadKmClaimPdf(claim) },
                        modifier = Modifier.weight(1f).testTag("download_km_pdf_${claim.empCode}"),
                      ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Claim PDF")
                      }
                      OutlinedButton(
                        onClick = { onShareKmClaimPdf(claim) },
                        modifier = Modifier.weight(1f).testTag("share_km_pdf_${claim.empCode}"),
                      ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Share")
                      }
                    }
                  }
                }
              }
            }
          } else {
            if (kmEntries.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Rounded.DirectionsCar,
                  title = "No Daily KM Logs Yet",
                  description = "Daily start and end odometer readings logged by supervisors appear here.",
                  actionLabel = "Add KM Entry",
                  onAction = onAddDailyKmClick,
                )
              }
            } else {
              items(kmEntries, key = { "kme_${it.id}" }) { entry ->
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = MaterialTheme.shapes.medium,
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text("${entry.empCode} • ${entry.operatorName}", style = MaterialTheme.typography.titleSmall)
                      Text(
                        text = "${formatEpochDay(entry.entryEpochDay)} • ${entry.startKm.toInt()} → ${entry.endKm.toInt()} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      if (entry.visitedFarms.isNotBlank()) {
                        Text(
                          text = "Visited: ${entry.visitedFarms}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = "${formatDecimal(entry.totalKm, 1)} km",
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      if (canEditOrDelete) {
                        IconButton(onClick = { onEditDailyKmEntry(entry) }) {
                          Icon(Icons.Rounded.Edit, contentDescription = "Edit daily KM entry", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteDailyKmEntry(entry) }) {
                          Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete daily KM entry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
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
    }
  }
}

@Composable
private fun SettlementBatchCard(
  batch: FlockBatch,
  canEditOrDelete: Boolean,
  onClick: () -> Unit,
  onEditGc: () -> Unit,
  onDownloadPdf: () -> Unit,
  onSharePdf: () -> Unit,
) {
  val breakdown = SettlementCalculator.calculate(
    batch = batch,
    liftedBirds = batch.liftedBirds,
    liftedWeightKg = batch.liftedWeightKg,
    returnedFeedKg = batch.returnedFeedKg,
  )
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(batch.batchCode, style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${batch.farmCode} • ${batch.farmerName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        BatchStatusBadge(status = batch.statusEnum)
      }

      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          LedgerRow("Lifted", "${formatNumber(batch.liftedBirds)} birds (${formatDecimal(breakdown.totalLiveWeightKg, 1)} kg)")
          LedgerRow("Actual vs Std FCR", "${formatDecimal(breakdown.actualFcr)} vs ${formatDecimal(breakdown.standardFcr)}")
          LedgerRow("Base Growing Charge", formatCurrency(breakdown.baseGrowingCharge))
          LedgerRow("Incentives / Deductions", formatCurrency(breakdown.fcrBonusOrPenalty + breakdown.mortalityAdjustment))
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (batch.statusEnum == BatchStatus.CLOSED) "Finalized Payout" else "Projected Payout",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = formatCurrency(batch.settlementPayout.takeIf { it > 0.0 } ?: breakdown.netPayable),
          style = MonoMetricLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onDownloadPdf,
          modifier = Modifier.weight(1f).testTag("hub_download_gc_${batch.batchCode}"),
        ) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("GC PDF")
        }
        OutlinedButton(
          onClick = onSharePdf,
          modifier = Modifier.weight(1f).testTag("hub_share_gc_${batch.batchCode}"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("Share")
        }
        if (canEditOrDelete && batch.statusEnum != BatchStatus.CLOSED) {
          OutlinedButton(onClick = onEditGc) {
            Text("Close")
          }
        }
      }
    }
  }
}

@Composable
private fun LedgerRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MonoMetricSmall)
  }
}
