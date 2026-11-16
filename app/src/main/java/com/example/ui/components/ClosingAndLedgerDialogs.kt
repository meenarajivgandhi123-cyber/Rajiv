package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedTransfer
import com.example.data.model.FeedType
import com.example.data.model.FlockBatch
import com.example.data.model.GcStatementDetails
import com.example.data.model.KmClaim
import com.example.data.model.KmDailyEntry
import com.example.data.model.MedicineCategory
import com.example.data.model.MedicineEntry
import com.example.data.model.SettlementCalculator
import com.example.ui.theme.MonoMetricMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDispatchDialog(
  openBatches: List<FlockBatch>,
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  preselectedBatch: FlockBatch? = null,
  existingDispatch: FeedDispatch? = null,
  onDismiss: () -> Unit,
  onSaveDispatch: (
    batch: FlockBatch?,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    challanNo: String,
    vehicleNo: String,
    remarks: String,
    requestedEpochDay: Long?,
  ) -> Unit,
) {
  val isEdit = existingDispatch != null
  var selectedBatch by remember { mutableStateOf(preselectedBatch ?: openBatches.firstOrNull()) }
  var batchExpanded by remember { mutableStateOf(false) }
  var feedType by remember { mutableStateOf(existingDispatch?.feedType ?: FeedType.GROWER.value) }
  var feedExpanded by remember { mutableStateOf(false) }
  val initialBags = existingDispatch?.bagsCount ?: 40L
  val initialWeight = existingDispatch?.weightKg ?: (initialBags * 50.0)
  var bagsText by remember { mutableStateOf(initialBags.toString()) }
  var weightText by remember {
    mutableStateOf(
      if (initialWeight % 1.0 == 0.0) initialWeight.toLong().toString() else initialWeight.toString()
    )
  }
  var challanNo by remember { mutableStateOf(existingDispatch?.challanNo ?: "CH-${(1000..9999).random()}") }
  var vehicleNo by remember { mutableStateOf(existingDispatch?.vehicleNo ?: "") }
  var remarks by remember { mutableStateOf(existingDispatch?.remarks ?: "") }

  var entryEpochDay by remember { mutableLongStateOf(existingDispatch?.dispatchEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingDispatch?.dispatchEpochDay ?: todayEpochDay)) }
  val dateValid = !canSelectPreviousDate ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)

  val bags = (bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
  val weightKg = (weightText.toDoubleOrNull() ?: (bags * 50.0)).coerceAtLeast(0.0)

  val destinationFarmLabel = if (isEdit) {
    existingDispatch.resolvedDestinationFarm.ifBlank { existingDispatch.farmerName }
  } else {
    selectedBatch?.let { "${it.farmCode} • ${it.displayFarmName}" } ?: "No farm selected"
  }
  val destinationBatchLabel = if (isEdit) {
    existingDispatch.batchCode
  } else {
    selectedBatch?.batchCode ?: "—"
  }
  val destinationFarmerName = if (isEdit) {
    existingDispatch.farmerName
  } else {
    selectedBatch?.farmerName ?: ""
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Update Feed Dispatch • ${existingDispatch.batchCode}" else "Feed Dispatch Entry") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!isEdit) {
          ExposedDropdownMenuBox(
            expanded = batchExpanded,
            onExpandedChange = { if (preselectedBatch == null) batchExpanded = it },
          ) {
            OutlinedTextField(
              value = selectedBatch?.let { "${it.farmCode} • ${it.displayFarmName} (${it.batchCode})" } ?: "No open farm / batch",
              onValueChange = {},
              readOnly = true,
              label = { Text("Destination Farm & Batch *") },
              trailingIcon = {
                if (preselectedBatch == null) {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded)
                }
              },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("feed_dispatch_farm_selector"),
            )
            ExposedDropdownMenu(
              expanded = batchExpanded,
              onDismissRequest = { batchExpanded = false },
            ) {
              openBatches.forEach { b ->
                DropdownMenuItem(
                  text = { Text("${b.farmCode} • ${b.displayFarmName} (${b.batchCode} • ${b.farmerName})") },
                  onClick = {
                    selectedBatch = b
                    batchExpanded = false
                  },
                )
              }
            }
          }
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth().testTag("feed_dispatch_destination_card"),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "Destination Farm",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = destinationFarmLabel,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Batch $destinationBatchLabel" + if (destinationFarmerName.isNotBlank()) " • Grower: $destinationFarmerName" else "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            )
          }
        }

        if (canSelectPreviousDate) {
          OutlinedTextField(
            value = dateText,
            onValueChange = { raw ->
              dateText = raw
              parseDdMmYyyy(raw)?.let { parsed ->
                if (parsed in 18000L..todayEpochDay) entryEpochDay = parsed
              }
            },
            label = { Text("Dispatch Date (DD-MM-YYYY)") },
            isError = !dateValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("feed_dispatch_date_input"),
          )
        } else {
          Text(
            text = "Dispatch Date: ${formatEpochDay(entryEpochDay)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        ExposedDropdownMenuBox(
          expanded = feedExpanded,
          onExpandedChange = { feedExpanded = it },
        ) {
          OutlinedTextField(
            value = feedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Feed Type / Phase *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feedExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("feed_dispatch_type_selector"),
          )
          ExposedDropdownMenu(
            expanded = feedExpanded,
            onDismissRequest = { feedExpanded = false },
          ) {
            FeedType.options.forEach { option ->
              DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                  feedType = option
                  feedExpanded = false
                },
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = bagsText,
            onValueChange = { raw ->
              bagsText = raw
              raw.toLongOrNull()?.coerceAtLeast(0L)?.let { parsedBags ->
                weightText = (parsedBags * 50L).toString()
              }
            },
            label = { Text("Bags (50 kg) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_bags_input"),
          )
          OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Quantity (kg) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_weight_input"),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = challanNo,
            onValueChange = { challanNo = it.uppercase() },
            label = { Text("Challan / Ref *") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("feed_dispatch_challan_input"),
          )
          OutlinedTextField(
            value = vehicleNo,
            onValueChange = { vehicleNo = it.uppercase() },
            label = { Text("Vehicle No") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_vehicle_input"),
          )
        }

        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Dispatch Notes / Remarks") },
          placeholder = { Text("e.g., Mill Gate 2 dispatch, tarpaulin covered") },
          modifier = Modifier.fillMaxWidth().testTag("feed_dispatch_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = (isEdit || selectedBatch != null) && bags >= 1L && weightKg >= 1.0 && challanNo.isNotBlank() && dateValid,
        onClick = {
          onSaveDispatch(
            selectedBatch,
            feedType,
            bags,
            weightKg,
            challanNo,
            vehicleNo,
            remarks,
            if (canSelectPreviousDate) entryEpochDay else null,
          )
        },
        modifier = Modifier.testTag("confirm_feed_dispatch_button"),
      ) {
        Text(if (isEdit) "Update Dispatch" else "Save Dispatch")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdSaleDialog(
  openBatches: List<FlockBatch>,
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  preselectedBatch: FlockBatch? = null,
  existingBatch: FlockBatch? = null,
  existingSale: BirdSale? = null,
  onDismiss: () -> Unit,
  onSaveBirdSale: (
    batch: FlockBatch?,
    buyerName: String,
    buyerPhone: String,
    vehicleNo: String,
    challanNo: String,
    birdsSold: Long,
    totalWeightKg: Double,
    ratePerKg: Double,
    remarks: String,
    requestedEpochDay: Long?,
  ) -> Unit,
) {
  val isEdit = existingSale != null
  var selectedBatch by remember { mutableStateOf(preselectedBatch ?: openBatches.firstOrNull()) }
  var batchExpanded by remember { mutableStateOf(false) }
  var buyerName by remember { mutableStateOf(existingSale?.buyerName ?: "") }
  var buyerPhone by remember { mutableStateOf(existingSale?.buyerPhone ?: "") }
  var vehicleNo by remember { mutableStateOf(existingSale?.vehicleNo ?: "") }
  var challanNo by remember { mutableStateOf(existingSale?.challanNo ?: "SL-${(1000..9999).random()}") }
  var remarks by remember { mutableStateOf(existingSale?.remarks ?: "") }
  var birdsSoldText by remember { mutableStateOf(existingSale?.birdsSold?.toString() ?: "") }
  var totalWeightText by remember { mutableStateOf(existingSale?.totalWeightKg?.toString() ?: "") }
  var ratePerKgText by remember { mutableStateOf((existingSale?.ratePerKg ?: 95.0).toString()) }

  var entryEpochDay by remember { mutableLongStateOf(existingSale?.saleEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingSale?.saleEpochDay ?: todayEpochDay)) }
  val dateValid = !canSelectPreviousDate ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)

  val birdsSold = (birdsSoldText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
  val totalWeightKg = (totalWeightText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  val ratePerKg = (ratePerKgText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  val saleAmount = totalWeightKg * ratePerKg

  val effectiveBatch = if (isEdit) {
    existingBatch ?: openBatches.firstOrNull { it.id == existingSale?.batchId }
  } else {
    selectedBatch
  }
  val autoAgeDays = effectiveBatch?.let { kotlin.math.max(0L, entryEpochDay - it.placementEpochDay) }
    ?: existingSale?.ageDays
    ?: 0L
  val autoAvgWeightKg = if (birdsSold > 0L && totalWeightKg > 0.0) {
    totalWeightKg / birdsSold.toDouble()
  } else {
    0.0
  }
  val autoAvgWeightGrams = kotlin.math.round(autoAvgWeightKg * 1000.0).toLong()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Update Bird Sale • ${existingSale.batchCode}" else "Daily Bird Sale Entry") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!isEdit) {
          ExposedDropdownMenuBox(
            expanded = batchExpanded,
            onExpandedChange = { if (preselectedBatch == null) batchExpanded = it },
          ) {
            OutlinedTextField(
              value = selectedBatch?.let { "${it.farmCode} • ${it.displayFarmName} (${it.batchCode} • ${it.survivingBirds} live)" } ?: "No open batch",
              onValueChange = {},
              readOnly = true,
              label = { Text("Source Farm / Batch *") },
              trailingIcon = {
                if (preselectedBatch == null) {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded)
                }
              },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = batchExpanded,
              onDismissRequest = { batchExpanded = false },
            ) {
              openBatches.forEach { b ->
                DropdownMenuItem(
                  text = { Text("${b.farmCode} • ${b.displayFarmName} (${b.batchCode} • ${b.survivingBirds} left)") },
                  onClick = {
                    selectedBatch = b
                    batchExpanded = false
                  },
                )
              }
            }
          }
        } else {
          Text(
            text = "${existingSale.farmCode} • ${existingSale.farmerName} (${existingSale.batchCode})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (canSelectPreviousDate) {
          OutlinedTextField(
            value = dateText,
            onValueChange = { raw ->
              dateText = raw
              parseDdMmYyyy(raw)?.let { parsed ->
                if (parsed in 18000L..todayEpochDay) entryEpochDay = parsed
              }
            },
            label = { Text("Sale Date (DD-MM-YYYY)") },
            isError = !dateValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bird_sale_date_input"),
          )
        } else {
          Text(
            text = "Sale Date: ${formatEpochDay(entryEpochDay)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Text(
          text = "Customer Details",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
          value = buyerName,
          onValueChange = { buyerName = it },
          label = { Text("Customer / Trader Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("sale_buyer_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = buyerPhone,
            onValueChange = { buyerPhone = it },
            label = { Text("Customer Phone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f).testTag("sale_buyer_phone_input"),
          )
          OutlinedTextField(
            value = vehicleNo,
            onValueChange = { vehicleNo = it.uppercase() },
            label = { Text("Vehicle No") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = challanNo,
          onValueChange = { challanNo = it.uppercase() },
          label = { Text("Slip / Challan No *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("sale_challan_input"),
        )

        Text(
          text = "Quantity Sold & Sale Price",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = birdsSoldText,
            onValueChange = { birdsSoldText = it },
            label = { Text("Quantity Sold (Birds) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("sale_birds_input"),
          )
          OutlinedTextField(
            value = totalWeightText,
            onValueChange = { totalWeightText = it },
            label = { Text("Total Wt (kg) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("sale_weight_input"),
          )
        }
        OutlinedTextField(
          value = ratePerKgText,
          onValueChange = { ratePerKgText = it },
          label = { Text("Sale Price (₹/kg)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("sale_rate_input"),
        )
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Age (Automatic)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                text = "Day $autoAgeDays",
                style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("sale_auto_age_text"),
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Avg Wt (Automatic)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                text = if (autoAvgWeightKg > 0.0) {
                  "${formatDecimal(autoAvgWeightKg, 3)} kg ($autoAvgWeightGrams g)"
                } else {
                  "—"
                },
                style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("sale_auto_avg_wt_text"),
              )
            }
          }
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Total Sale Amount", style = MaterialTheme.typography.titleSmall)
            Text(
              text = formatCurrency(saleAmount),
              style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Customer / Delivery Notes") },
          placeholder = { Text("e.g., Morning lift, cash settled on spot") },
          modifier = Modifier.fillMaxWidth().testTag("sale_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = (isEdit || selectedBatch != null) && buyerName.isNotBlank() && challanNo.isNotBlank() && birdsSold > 0L && totalWeightKg > 0.0 && dateValid,
        onClick = {
          onSaveBirdSale(
            selectedBatch,
            buyerName,
            buyerPhone,
            vehicleNo,
            challanNo,
            birdsSold,
            totalWeightKg,
            ratePerKg,
            remarks,
            if (canSelectPreviousDate) entryEpochDay else null,
          )
        },
        modifier = Modifier.testTag("confirm_bird_sale_button"),
      ) {
        Text(if (isEdit) "Update Sale" else "Save Bird Sale")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTransferDialog(
  openBatches: List<FlockBatch>,
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  preselectedFromBatch: FlockBatch? = null,
  existingTransfer: FeedTransfer? = null,
  onDismiss: () -> Unit,
  onSaveTransfer: (
    fromBatch: FlockBatch?,
    toBatch: FlockBatch?,
    feedType: String,
    bagsCount: Long,
    weightKg: Double,
    transferRef: String,
    remarks: String,
    requestedEpochDay: Long?,
  ) -> Unit,
) {
  val isEdit = existingTransfer != null
  var fromBatch by remember { mutableStateOf(preselectedFromBatch ?: openBatches.firstOrNull()) }
  val eligibleTargets = openBatches.filter { it.id != fromBatch?.id }
  var toBatch by remember { mutableStateOf(eligibleTargets.firstOrNull()) }
  var fromExpanded by remember { mutableStateOf(false) }
  var toExpanded by remember { mutableStateOf(false) }

  var feedType by remember { mutableStateOf(existingTransfer?.feedType ?: FeedType.GROWER.value) }
  var feedExpanded by remember { mutableStateOf(false) }
  var bagsText by remember { mutableStateOf((existingTransfer?.bagsCount ?: 10L).toString()) }
  var transferRef by remember { mutableStateOf(existingTransfer?.transferRef ?: "IFT-${(1000..9999).random()}") }
  var remarks by remember { mutableStateOf(existingTransfer?.remarks ?: "") }

  var entryEpochDay by remember { mutableLongStateOf(existingTransfer?.transferEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingTransfer?.transferEpochDay ?: todayEpochDay)) }
  val dateValid = !(canSelectPreviousDate && !isEdit) ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)

  val bags = (bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
  val weightKg = bags * 50.0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Update Inter-Feed Transfer" else "Inter-Feed Transfer Entry") },
    text = {
      if (!isEdit && openBatches.size < 2) {
        Text("At least two active/closing batches are required to transfer feed between farms.")
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (isEdit) {
            Text(
              text = "${existingTransfer.fromFarmCode} (${existingTransfer.fromBatchCode}) → ${existingTransfer.toFarmCode} (${existingTransfer.toBatchCode})",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          } else {
            ExposedDropdownMenuBox(
              expanded = fromExpanded,
              onExpandedChange = { if (preselectedFromBatch == null) fromExpanded = it },
            ) {
              OutlinedTextField(
                value = fromBatch?.let { "${it.batchCode} • ${it.farmCode} (${it.farmerName})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("From Farm / Batch *") },
                trailingIcon = {
                  if (preselectedFromBatch == null) ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded)
                },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(
                expanded = fromExpanded,
                onDismissRequest = { fromExpanded = false },
              ) {
                openBatches.forEach { b ->
                  DropdownMenuItem(
                    text = { Text("${b.batchCode} • ${b.farmCode} (${b.farmerName})") },
                    onClick = {
                      fromBatch = b
                      if (toBatch?.id == b.id) {
                        toBatch = openBatches.firstOrNull { it.id != b.id }
                      }
                      fromExpanded = false
                    },
                  )
                }
              }
            }

            ExposedDropdownMenuBox(
              expanded = toExpanded,
              onExpandedChange = { toExpanded = it },
            ) {
              OutlinedTextField(
                value = toBatch?.let { "${it.batchCode} • ${it.farmCode} (${it.farmerName})" } ?: "Select destination",
                onValueChange = {},
                readOnly = true,
                label = { Text("To Farm / Batch *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(
                expanded = toExpanded,
                onDismissRequest = { toExpanded = false },
              ) {
                eligibleTargets.forEach { b ->
                  DropdownMenuItem(
                    text = { Text("${b.batchCode} • ${b.farmCode} (${b.farmerName})") },
                    onClick = {
                      toBatch = b
                      toExpanded = false
                    },
                  )
                }
              }
            }
          }

          if (canSelectPreviousDate && !isEdit) {
            OutlinedTextField(
              value = dateText,
              onValueChange = { raw ->
                dateText = raw
                parseDdMmYyyy(raw)?.let { parsed ->
                  if (parsed in 18000L..todayEpochDay) entryEpochDay = parsed
                }
              },
              label = { Text("Transfer Date (DD-MM-YYYY)") },
              isError = !dateValid,
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("feed_transfer_date_input"),
            )
          } else {
            Text(
              text = "Transfer Date: ${formatEpochDay(entryEpochDay)}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          ExposedDropdownMenuBox(
            expanded = feedExpanded,
            onExpandedChange = { feedExpanded = it },
          ) {
            OutlinedTextField(
              value = feedType,
              onValueChange = {},
              readOnly = true,
              label = { Text("Feed Phase") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feedExpanded) },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = feedExpanded,
              onDismissRequest = { feedExpanded = false },
            ) {
              FeedType.options.forEach { opt ->
                DropdownMenuItem(
                  text = { Text(opt) },
                  onClick = {
                    feedType = opt
                    feedExpanded = false
                  },
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = bagsText,
              onValueChange = { bagsText = it },
              label = { Text("Bags (50kg) *") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = transferRef,
              onValueChange = { transferRef = it.uppercase() },
              label = { Text("Slip Ref *") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          Text(
            text = "Transfer Weight: ${formatDecimal(weightKg, 0)} kg",
            style = MonoMetricMedium,
            color = MaterialTheme.colorScheme.primary,
          )

          OutlinedTextField(
            value = remarks,
            onValueChange = { remarks = it },
            label = { Text("Remarks") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      Button(
        enabled = (isEdit || (fromBatch != null && toBatch != null && fromBatch?.id != toBatch?.id)) &&
          bags >= 1L && transferRef.isNotBlank() && dateValid,
        onClick = {
          onSaveTransfer(
            fromBatch,
            toBatch,
            feedType,
            bags,
            weightKg,
            transferRef,
            remarks,
            if (canSelectPreviousDate && !isEdit) entryEpochDay else null,
          )
        },
        modifier = Modifier.testTag("confirm_feed_transfer_button"),
      ) {
        Text(if (isEdit) "Update Transfer" else "Save Transfer")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun DailyKmEntryDialog(
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  existingEntry: KmDailyEntry? = null,
  onDismiss: () -> Unit,
  onSave: (
    requestedEpochDay: Long?,
    startKm: Double,
    endKm: Double,
    visitedFarms: String,
  ) -> Unit,
) {
  var entryEpochDay by remember { mutableLongStateOf(existingEntry?.entryEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingEntry?.entryEpochDay ?: todayEpochDay)) }
  var startKmText by remember { mutableStateOf(existingEntry?.startKm?.toString() ?: "") }
  var endKmText by remember { mutableStateOf(existingEntry?.endKm?.toString() ?: "") }
  var visitedFarms by remember { mutableStateOf(existingEntry?.visitedFarms ?: "") }

  val dateValid = !canSelectPreviousDate ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)
  val startKm = startKmText.toDoubleOrNull() ?: -1.0
  val endKm = endKmText.toDoubleOrNull() ?: -1.0
  val totalKm = (endKm - startKm).coerceAtLeast(0.0)
  val isValid = dateValid && startKm >= 0.0 && endKm > startKm

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existingEntry == null) "Daily KM Entry" else "Update Daily KM • ${existingEntry.empCode}") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (canSelectPreviousDate) {
          OutlinedTextField(
            value = dateText,
            onValueChange = { raw ->
              dateText = raw
              parseDdMmYyyy(raw)?.let { parsed ->
                if (parsed in 18000L..todayEpochDay) entryEpochDay = parsed
              }
            },
            label = { Text("Entry Date (DD-MM-YYYY)") },
            isError = !dateValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("km_entry_date_input"),
          )
        } else {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Entry Date: ${formatEpochDay(entryEpochDay)}",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = startKmText,
            onValueChange = { startKmText = it },
            label = { Text("Start KM *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("km_start_input"),
          )
          OutlinedTextField(
            value = endKmText,
            onValueChange = { endKmText = it },
            label = { Text("End KM *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("km_end_input"),
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text("Day's Distance", style = MaterialTheme.typography.labelSmall)
              Text(
                text = "Added to monthly travel total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
              )
            }
            Text(
              text = "${formatDecimal(totalKm, 1)} km",
              style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        OutlinedTextField(
          value = visitedFarms,
          onValueChange = { visitedFarms = it },
          label = { Text("Visited Farms / Route") },
          placeholder = { Text("e.g., FRM-101, FRM-104 (Morning Round)") },
          modifier = Modifier.fillMaxWidth().testTag("km_farms_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = {
          onSave(
            if (canSelectPreviousDate) entryEpochDay else null,
            startKm,
            endKm,
            visitedFarms,
          )
        },
        modifier = Modifier.testTag("save_daily_km_button"),
      ) {
        Text(if (existingEntry == null) "Save KM Entry" else "Update KM")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun EditMonthlyKmClaimDialog(
  claim: KmClaim,
  onDismiss: () -> Unit,
  onSave: (
    totalKm: Double,
    ratePerKm: Double,
    visitedFarms: String,
  ) -> Unit,
) {
  var totalKmText by remember { mutableStateOf(formatDecimal(claim.totalKm, 1)) }
  var rateText by remember { mutableStateOf(claim.ratePerKm.toString()) }
  var visitedFarms by remember { mutableStateOf(claim.visitedFarms) }

  val totalKm = totalKmText.toDoubleOrNull() ?: -1.0
  val ratePerKm = rateText.toDoubleOrNull() ?: -1.0
  val claimAmount = (totalKm.coerceAtLeast(0.0)) * (ratePerKm.coerceAtLeast(0.0))
  val isValid = totalKm >= 0.0 && ratePerKm >= 0.0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Update ${claim.resolvedMonthLabel} Claim • ${claim.empCode}") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "${claim.operatorName} • ${claim.dailyEntriesCount} daily KM entries combined",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = totalKmText,
            onValueChange = { totalKmText = it },
            label = { Text("Total Month KM *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("edit_monthly_km_total_input"),
          )
          OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            label = { Text("Rate (₹/km) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("edit_monthly_km_rate_input"),
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Claim Amount", style = MaterialTheme.typography.titleSmall)
            Text(
              text = formatCurrency(claimAmount),
              style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        OutlinedTextField(
          value = visitedFarms,
          onValueChange = { visitedFarms = it },
          label = { Text("Route / Remarks") },
          modifier = Modifier.fillMaxWidth().testTag("edit_monthly_km_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = { onSave(totalKm, ratePerKm, visitedFarms) },
        modifier = Modifier.testTag("save_monthly_km_claim_button"),
      ) {
        Text("Update Claim")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun ReviewKmClaimDialog(
  claim: KmClaim,
  approved: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (remarks: String) -> Unit,
) {
  var remarks by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (approved) "Approve Monthly KM Claim" else "Reject Monthly KM Claim") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${claim.operatorName} (${claim.empCode}) • ${claim.resolvedMonthLabel}",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = "${claim.dailyEntriesCount} daily logs • ${formatDecimal(claim.totalKm, 1)} km @ ₹${formatDecimal(claim.ratePerKm, 1)} • ${formatCurrency(claim.claimAmount)}",
          style = MonoMetricMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        if (claim.visitedFarms.isNotBlank()) {
          Text(
            text = "Route / Note: ${claim.visitedFarms}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text(if (approved) "Approval Note (Optional)" else "Rejection Reason *") },
          modifier = Modifier.fillMaxWidth().testTag("review_km_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = approved || remarks.isNotBlank(),
        onClick = { onConfirm(remarks) },
        colors = if (approved) {
          ButtonDefaults.buttonColors()
        } else {
          ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        },
        modifier = Modifier.testTag("confirm_review_km_button"),
      ) {
        Text(if (approved) "Approve Claim" else "Reject Claim")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineEntryDialog(
  openBatches: List<FlockBatch>,
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  preselectedBatch: FlockBatch? = null,
  existingEntry: MedicineEntry? = null,
  onDismiss: () -> Unit,
  onSaveMedicine: (
    batch: FlockBatch?,
    medicineName: String,
    category: String,
    dosageOrQty: String,
    totalCost: Double,
    invoiceOrSlipNo: String,
    remarks: String,
    requestedEpochDay: Long?,
  ) -> Unit,
) {
  val isEdit = existingEntry != null
  var selectedBatch by remember { mutableStateOf(preselectedBatch ?: openBatches.firstOrNull()) }
  var batchExpanded by remember { mutableStateOf(false) }

  var medicineName by remember { mutableStateOf(existingEntry?.medicineName ?: "") }
  var category by remember { mutableStateOf(existingEntry?.category ?: MedicineCategory.VACCINE.value) }
  var categoryExpanded by remember { mutableStateOf(false) }
  var dosageOrQty by remember { mutableStateOf(existingEntry?.dosageOrQty ?: "") }
  var totalCostText by remember { mutableStateOf(existingEntry?.totalCost?.toString() ?: "") }
  var invoiceOrSlipNo by remember { mutableStateOf(existingEntry?.invoiceOrSlipNo ?: "MED-${(1000..9999).random()}") }
  var remarks by remember { mutableStateOf(existingEntry?.remarks ?: "") }

  var entryEpochDay by remember { mutableLongStateOf(existingEntry?.entryEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingEntry?.entryEpochDay ?: todayEpochDay)) }
  val dateValid = !canSelectPreviousDate ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)

  val totalCost = totalCostText.toDoubleOrNull() ?: -1.0
  val isValid = (isEdit || selectedBatch != null) &&
    medicineName.isNotBlank() &&
    totalCost >= 0.0 &&
    invoiceOrSlipNo.isNotBlank() &&
    dateValid

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        if (isEdit) "Update Medicine Entry • ${existingEntry.batchCode}" else "Medicine Entry",
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!isEdit) {
          ExposedDropdownMenuBox(
            expanded = batchExpanded,
            onExpandedChange = { if (preselectedBatch == null) batchExpanded = it },
          ) {
            OutlinedTextField(
              value = selectedBatch?.let { "${it.batchCode} • ${it.farmCode} (${it.farmerName})" } ?: "No open batch",
              onValueChange = {},
              readOnly = true,
              label = { Text("Select Batch *") },
              trailingIcon = {
                if (preselectedBatch == null) {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded)
                }
              },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = batchExpanded,
              onDismissRequest = { batchExpanded = false },
            ) {
              openBatches.forEach { b ->
                DropdownMenuItem(
                  text = { Text("${b.batchCode} • ${b.farmCode} (${b.farmerName})") },
                  onClick = {
                    selectedBatch = b
                    batchExpanded = false
                  },
                )
              }
            }
          }
        } else {
          Text(
            text = "${existingEntry.farmCode} (${existingEntry.farmerName}) • Batch ${existingEntry.batchCode}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (canSelectPreviousDate) {
          OutlinedTextField(
            value = dateText,
            onValueChange = { raw ->
              dateText = raw
              parseDdMmYyyy(raw)?.let { parsed ->
                if (parsed in 18000L..todayEpochDay) entryEpochDay = parsed
              }
            },
            label = { Text("Entry Date (DD-MM-YYYY)") },
            isError = !dateValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("medicine_date_input"),
          )
        } else {
          Text(
            text = "Entry Date: ${formatEpochDay(entryEpochDay)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        ExposedDropdownMenuBox(
          expanded = categoryExpanded,
          onExpandedChange = { categoryExpanded = it },
        ) {
          OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = categoryExpanded,
            onDismissRequest = { categoryExpanded = false },
          ) {
            MedicineCategory.options.forEach { opt ->
              DropdownMenuItem(
                text = { Text(opt) },
                onClick = {
                  category = opt
                  categoryExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = medicineName,
          onValueChange = { medicineName = it },
          label = { Text("Medicine / Vaccine Name *") },
          placeholder = { Text("e.g., Lasota Vaccine / Enrofloxacin") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("medicine_name_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = dosageOrQty,
            onValueChange = { dosageOrQty = it },
            label = { Text("Dosage / Qty") },
            placeholder = { Text("e.g., 2 L / 5 vials") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("medicine_dosage_input"),
          )
          OutlinedTextField(
            value = totalCostText,
            onValueChange = { totalCostText = it },
            label = { Text("Total Cost (₹) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("medicine_cost_input"),
          )
        }

        OutlinedTextField(
          value = invoiceOrSlipNo,
          onValueChange = { invoiceOrSlipNo = it.uppercase() },
          label = { Text("Invoice / Slip Ref *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("medicine_slip_input"),
        )

        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks / Vet Note") },
          modifier = Modifier.fillMaxWidth().testTag("medicine_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = {
          onSaveMedicine(
            selectedBatch,
            medicineName,
            category,
            dosageOrQty,
            totalCost.coerceAtLeast(0.0),
            invoiceOrSlipNo,
            remarks,
            if (canSelectPreviousDate) entryEpochDay else null,
          )
        },
        modifier = Modifier.testTag("confirm_medicine_entry_button"),
      ) {
        Text(if (isEdit) "Update Medicine" else "Save Medicine")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun BatchClosingDialog(
  batch: FlockBatch,
  todayEpochDay: Long,
  operatorName: String,
  recordedMedicineTotal: Double = 0.0,
  onDismiss: () -> Unit,
  onMarkClosing: (
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    notes: String,
    gcDetails: GcStatementDetails,
  ) -> Unit,
  onConfirmCloseBatch: (
    liftedBirds: Long,
    liftedWeightKg: Double,
    returnedFeedKg: Double,
    notes: String,
    gcDetails: GcStatementDetails,
  ) -> Unit,
) {
  val existing = batch.gcDetails ?: GcStatementDetails(
    companyName = "SPF POULTRY FARM",
    medicineCost = recordedMedicineTotal,
    stdGcPerKg = batch.growingRatePerKg,
    meanAgeDays = batch.currentAgeDays(todayEpochDay).toDouble(),
    createdByName = operatorName,
    enteredByName = operatorName,
  )

  val defaultBirds = if (batch.liftedBirds > 0L) batch.liftedBirds else (batch.totalBirdsStarted - batch.cumulativeMortality)
  val defaultWeight = if (batch.liftedWeightKg > 0.0) batch.liftedWeightKg else batch.liveBiomassKg

  var companyName by remember { mutableStateOf(existing.companyName.ifBlank { "SPF POULTRY FARM" }) }
  var companyAddress by remember { mutableStateOf(existing.companyAddress) }
  var companyPhone by remember { mutableStateOf(existing.companyPhone) }
  var companyPanGst by remember { mutableStateOf(existing.companyPanGst) }

  var liftedBirdsText by remember { mutableStateOf(defaultBirds.coerceAtLeast(0L).toString()) }
  var liftedWeightText by remember { mutableStateOf(formatDecimal(defaultWeight.coerceAtLeast(0.0), 2)) }
  var returnedFeedText by remember { mutableStateOf(formatDecimal(batch.returnedFeedKg, 1)) }
  var shortageBirdsText by remember { mutableStateOf(existing.shortageBirds.toString()) }
  var shortageBagsText by remember { mutableStateOf(existing.shortageFeedBags.toString()) }
  var meanAgeText by remember {
    mutableStateOf(
      formatDecimal(
        existing.meanAgeDays.takeIf { it > 0.0 } ?: batch.currentAgeDays(todayEpochDay).toDouble(),
        2,
      )
    )
  }

  var chickRateText by remember { mutableStateOf(existing.chickRate.toString()) }
  var feedRateText by remember { mutableStateOf(existing.feedRatePerKg.toString()) }
  var adminRateText by remember { mutableStateOf(existing.adminRatePerChick.toString()) }
  var medicineCostText by remember { mutableStateOf(existing.medicineCost.toString()) }
  var otherExpText by remember { mutableStateOf(existing.otherExpCost.toString()) }
  var stdProCostText by remember { mutableStateOf(existing.stdProductionCost.toString()) }
  var stdGcText by remember { mutableStateOf(existing.stdGcPerKg.toString()) }
  var saleRateText by remember { mutableStateOf(existing.saleRatePerKg.toString()) }
  var saleIncentiveText by remember { mutableStateOf(existing.saleRateIncentive.toString()) }
  var lowCostIncentiveText by remember { mutableStateOf(existing.lowCostIncentive.toString()) }
  var feedUnloadingText by remember { mutableStateOf(existing.feedUnloadingAmount.toString()) }
  var miscText by remember { mutableStateOf(existing.miscAmount.toString()) }
  var miscDetail by remember { mutableStateOf(existing.miscDetail) }
  var cfcrWaiverText by remember { mutableStateOf(existing.cfcrWaiverAmount.toString()) }
  var highCostDedText by remember { mutableStateOf(existing.highCostDeduction.toString()) }
  var shortageDedText by remember { mutableStateOf(existing.birdsShortageDeduction.toString()) }
  var otherDedText by remember { mutableStateOf(existing.otherDeduction.toString()) }
  var otherDedDetail by remember { mutableStateOf(existing.otherDeductionDetail) }
  var waiverIfAnyText by remember { mutableStateOf(existing.waiverIfAny.toString()) }
  var tdsPctText by remember { mutableStateOf(existing.tdsPercent.toString()) }
  var grade by remember { mutableStateOf(existing.grade) }

  var createdBy by remember { mutableStateOf(existing.createdByName.ifBlank { operatorName }) }
  var approvedBy by remember { mutableStateOf(existing.approvedByName) }
  var enteredBy by remember { mutableStateOf(existing.enteredByName.ifBlank { operatorName }) }
  var paymentBy by remember { mutableStateOf(existing.paymentByName) }
  var notes by remember { mutableStateOf(batch.closingNotes) }

  val liftedBirds = (liftedBirdsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
  val liftedWeightKg = (liftedWeightText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  val returnedFeedKg = (returnedFeedText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

  val gcDetails = GcStatementDetails(
    companyName = companyName,
    companyAddress = companyAddress,
    companyPhone = companyPhone,
    companyPanGst = companyPanGst,
    chickRate = chickRateText.toDoubleOrNull() ?: 35.0,
    feedRatePerKg = feedRateText.toDoubleOrNull() ?: 40.0,
    adminRatePerChick = adminRateText.toDoubleOrNull() ?: 2.0,
    medicineCost = medicineCostText.toDoubleOrNull() ?: 0.0,
    otherExpCost = otherExpText.toDoubleOrNull() ?: 0.0,
    stdProductionCost = stdProCostText.toDoubleOrNull() ?: 82.0,
    stdGcPerKg = stdGcText.toDoubleOrNull() ?: batch.growingRatePerKg,
    saleRatePerKg = saleRateText.toDoubleOrNull() ?: 85.0,
    saleRateIncentive = saleIncentiveText.toDoubleOrNull() ?: 0.0,
    lowCostIncentive = lowCostIncentiveText.toDoubleOrNull() ?: 0.0,
    feedUnloadingAmount = feedUnloadingText.toDoubleOrNull() ?: 0.0,
    miscAmount = miscText.toDoubleOrNull() ?: 0.0,
    cfcrWaiverAmount = cfcrWaiverText.toDoubleOrNull() ?: 0.0,
    highCostDeduction = highCostDedText.toDoubleOrNull() ?: 0.0,
    birdsShortageDeduction = shortageDedText.toDoubleOrNull() ?: 0.0,
    otherDeduction = otherDedText.toDoubleOrNull() ?: 0.0,
    waiverIfAny = waiverIfAnyText.toDoubleOrNull() ?: 0.0,
    tdsPercent = (tdsPctText.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 30.0),
    shortageBirds = shortageBirdsText.toLongOrNull() ?: 0L,
    shortageFeedBags = shortageBagsText.toDoubleOrNull() ?: 0.0,
    grade = grade,
    meanAgeDays = meanAgeText.toDoubleOrNull() ?: batch.currentAgeDays(todayEpochDay).toDouble(),
    miscDetail = miscDetail,
    otherDeductionDetail = otherDedDetail,
    createdByName = createdBy,
    approvedByName = approvedBy,
    enteredByName = enteredBy,
    paymentByName = paymentBy,
  )

  val summary = SettlementCalculator.computeGcStatement(
    batch = batch,
    liftedBirds = liftedBirds,
    liftedWeightKg = liftedWeightKg,
    returnedFeedKg = returnedFeedKg,
    details = gcDetails,
    todayEpochDay = todayEpochDay,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("GC Closing Statement • ${batch.batchCode}") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Save as Closing Farm while lifting continues, or tap GC Closed to finalize settlement PDF and set ${batch.farmCode} to Empty Farm.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = companyName,
          onValueChange = { companyName = it },
          label = { Text("Company Header Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = companyPhone,
            onValueChange = { companyPhone = it },
            label = { Text("Company Phone") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = companyPanGst,
            onValueChange = { companyPanGst = it },
            label = { Text("PAN / GST") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = companyAddress,
          onValueChange = { companyAddress = it },
          label = { Text("Company Address") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = liftedBirdsText,
            onValueChange = { liftedBirdsText = it },
            label = { Text("Birds Sold") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = liftedWeightText,
            onValueChange = { liftedWeightText = it },
            label = { Text("Birds Wt (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = returnedFeedText,
            onValueChange = { returnedFeedText = it },
            label = { Text("Ret. Feed (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = meanAgeText,
            onValueChange = { meanAgeText = it },
            label = { Text("M.Age (Auto)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = shortageBirdsText,
            onValueChange = { shortageBirdsText = it },
            label = { Text("Shortage Birds") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = shortageBagsText,
            onValueChange = { shortageBagsText = it },
            label = { Text("Shortage Feed Bag") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = chickRateText,
            onValueChange = { chickRateText = it },
            label = { Text("Chick Rate") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = feedRateText,
            onValueChange = { feedRateText = it },
            label = { Text("Feed ₹/kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = adminRateText,
            onValueChange = { adminRateText = it },
            label = { Text("Admin") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = medicineCostText,
            onValueChange = { medicineCostText = it },
            label = { Text("Medicine Cost") },
            supportingText = if (recordedMedicineTotal > 0.0) {
              { Text("Ledger total: ${formatCurrency(recordedMedicineTotal)}") }
            } else {
              null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = otherExpText,
            onValueChange = { otherExpText = it },
            label = { Text("Other Exp") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = stdProCostText,
            onValueChange = { stdProCostText = it },
            label = { Text("Std Pro.Cost") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = stdGcText,
            onValueChange = { stdGcText = it },
            label = { Text("Std GC ₹/kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = saleRateText,
            onValueChange = { saleRateText = it },
            label = { Text("Sale Rate") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = lowCostIncentiveText,
            onValueChange = { lowCostIncentiveText = it },
            label = { Text("Low Cost Inc.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = saleIncentiveText,
            onValueChange = { saleIncentiveText = it },
            label = { Text("Sale Rate Inc.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = feedUnloadingText,
            onValueChange = { feedUnloadingText = it },
            label = { Text("Feed Unload") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = miscText,
            onValueChange = { miscText = it },
            label = { Text("*Misc") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = cfcrWaiverText,
            onValueChange = { cfcrWaiverText = it },
            label = { Text("CFCR Waiver") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = highCostDedText,
            onValueChange = { highCostDedText = it },
            label = { Text("High Cost Ded.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = shortageDedText,
            onValueChange = { shortageDedText = it },
            label = { Text("Shortage Ded.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = otherDedText,
            onValueChange = { otherDedText = it },
            label = { Text("**Other Ded.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = waiverIfAnyText,
            onValueChange = { waiverIfAnyText = it },
            label = { Text("Waiver If Any") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = tdsPctText,
            onValueChange = { tdsPctText = it },
            label = { Text("TDS %") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = grade,
            onValueChange = { grade = it.uppercase() },
            label = { Text("Grade") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
          OutlinedTextField(
            value = miscDetail,
            onValueChange = { miscDetail = it },
            label = { Text("*Misc Detail") },
            singleLine = true,
            modifier = Modifier.weight(1.5f),
          )
        }
        OutlinedTextField(
          value = otherDedDetail,
          onValueChange = { otherDedDetail = it },
          label = { Text("**Other Deduction Detail") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = createdBy,
            onValueChange = { createdBy = it },
            label = { Text("Created By") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = approvedBy,
            onValueChange = { approvedBy = it },
            label = { Text("Approved By") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = enteredBy,
            onValueChange = { enteredBy = it },
            label = { Text("Entered By") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = paymentBy,
            onValueChange = { paymentBy = it },
            label = { Text("Payment By") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Closing / Lifting Notes") },
          modifier = Modifier.fillMaxWidth(),
        )

        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = "Live GC Statement Preview",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            PreviewLine("Pro.Cost / Std Pro.Cost", "${formatDecimal(summary.productionCostPerKg)} / ${formatDecimal(summary.stdProductionCostPerKg)}")
            PreviewLine("FCR / CFCR / EEF", "${formatDecimal(summary.fcr)} / ${formatDecimal(summary.cfcr)} / ${formatDecimal(summary.eef, 0)}")
            PreviewLine("Std GC Amount", formatCurrency(summary.stdGcAmount))
            PreviewLine("Total GC (Before TDS)", formatCurrency(summary.totalGc))
            PreviewLine("TDS (${formatDecimal(gcDetails.tdsPercent, 1)}%)", formatCurrency(summary.tdsAmount))
            PreviewLine("Total Payable", formatCurrency(summary.totalPayable), bold = true)
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (batch.statusEnum != BatchStatus.CLOSED) {
          OutlinedButton(
            onClick = { onMarkClosing(liftedBirds, liftedWeightKg, returnedFeedKg, notes, gcDetails) },
            modifier = Modifier.testTag("mark_closing_batch_button"),
          ) {
            Text("Closing Farm")
          }
          Button(
            onClick = { onConfirmCloseBatch(liftedBirds, liftedWeightKg, returnedFeedKg, notes, gcDetails) },
            modifier = Modifier.testTag("confirm_close_batch_button"),
          ) {
            Text("GC Closed")
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Back") }
    },
  )
}

@Composable
private fun PreviewLine(label: String, value: String, bold: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
    )
    Text(
      text = value,
      style = MonoMetricMedium.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
      color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
  }
}
