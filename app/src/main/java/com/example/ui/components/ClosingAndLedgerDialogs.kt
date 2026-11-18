package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.MasterMedicineCatalog
import com.example.data.model.BatchStatus
import com.example.data.model.BirdSale
import com.example.data.model.FeedDispatch
import com.example.data.model.FeedRateBasis
import com.example.data.model.FeedTransfer
import com.example.data.model.FeedType
import com.example.data.model.FlockBatch
import com.example.data.model.GcStatementDetails
import com.example.data.model.KmClaim
import com.example.data.model.KmDailyEntry
import com.example.data.model.MedicineCatalogItem
import com.example.data.model.MedicineCategory
import com.example.data.model.MedicineEntry
import com.example.data.model.SettlementCalculator
import com.example.data.model.computeFeedDispatchTotalAmount
import com.example.data.model.computeMedicineTotalCost
import com.example.data.model.extractMedicineQuantity
import com.example.data.model.filterAndRankMedicineCatalog
import com.example.ui.theme.MonoMetricMedium
import java.util.Locale
import kotlin.math.abs

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
    unitRate: Double,
    rateBasis: String,
    totalAmount: Double,
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
  var rateBasis by remember {
    mutableStateOf(existingDispatch?.rateBasis?.ifBlank { FeedRateBasis.PER_BAG.value } ?: FeedRateBasis.PER_BAG.value)
  }
  var unitRateText by remember {
    mutableStateOf(
      existingDispatch?.unitRate?.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""
    )
  }
  val initialResolvedTotal = existingDispatch?.resolvedTotalAmount ?: 0.0
  var totalAmountText by remember {
    mutableStateOf(
      if (initialResolvedTotal > 0.0) String.format(Locale.US, "%.2f", initialResolvedTotal) else ""
    )
  }
  var totalAmountManuallyEdited by remember {
    mutableStateOf(existingDispatch != null && existingDispatch.totalAmount > 0.0 && existingDispatch.unitRate <= 0.0)
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
  val unitRateValid = unitRateText.isBlank() || (unitRateText.toDoubleOrNull()?.let { it >= 0.0 } == true)
  val totalAmountValid = totalAmountText.isBlank() || (totalAmountText.toDoubleOrNull()?.let { it >= 0.0 } == true)
  val unitRate = (unitRateText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  val computedAmount = computeFeedDispatchTotalAmount(bags, weightKg, unitRate, rateBasis)
  val resolvedTotalAmount = if (totalAmountText.isNotBlank()) {
    (totalAmountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  } else {
    computedAmount
  }

  fun syncAutoTotal(nextBags: Long, nextWeightKg: Double, nextRate: Double, nextBasis: String) {
    if (!totalAmountManuallyEdited) {
      totalAmountText = if (nextRate > 0.0) {
        String.format(Locale.US, "%.2f", computeFeedDispatchTotalAmount(nextBags, nextWeightKg, nextRate, nextBasis))
      } else {
        ""
      }
    }
  }

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
              val nextBags = raw.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
              val nextWeight = if (raw.toLongOrNull() != null) (nextBags * 50L).toDouble() else weightKg
              if (raw.toLongOrNull() != null) {
                weightText = (nextBags * 50L).toString()
              }
              syncAutoTotal(nextBags, nextWeight, unitRate, rateBasis)
            },
            label = { Text("Bags (50 kg) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_bags_input"),
          )
          OutlinedTextField(
            value = weightText,
            onValueChange = { raw ->
              weightText = raw
              val nextWeight = raw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
              syncAutoTotal(bags, nextWeight, unitRate, rateBasis)
            },
            label = { Text("Quantity (kg) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_weight_input"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Rate Basis:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FeedRateBasis.entries.forEach { basis ->
            FilterChip(
              selected = rateBasis == basis.value,
              onClick = {
                rateBasis = basis.value
                totalAmountManuallyEdited = false
                syncAutoTotal(bags, weightKg, unitRate, basis.value)
              },
              label = { Text(basis.label) },
              modifier = Modifier.testTag("feed_dispatch_rate_basis_${basis.value}"),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = unitRateText,
            onValueChange = { raw ->
              unitRateText = raw
              totalAmountManuallyEdited = false
              val nextRate = (raw.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
              syncAutoTotal(bags, weightKg, nextRate, rateBasis)
            },
            label = {
              Text(if (rateBasis == FeedRateBasis.PER_KG.value) "Unit Rate (₹/kg)" else "Unit Rate (₹/bag)")
            },
            placeholder = {
              Text(if (rateBasis == FeedRateBasis.PER_KG.value) "e.g., 42.00" else "e.g., 2100.00")
            },
            isError = !unitRateValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_unit_rate_input"),
          )
          OutlinedTextField(
            value = totalAmountText,
            onValueChange = { raw ->
              totalAmountText = raw
              totalAmountManuallyEdited = raw.isNotBlank()
            },
            label = { Text("Total Amount (₹)") },
            placeholder = { Text("Auto or custom") },
            isError = !totalAmountValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("feed_dispatch_total_amount_input"),
          )
        }

        if (resolvedTotalAmount > 0.0 || unitRate > 0.0) {
          val basisEnum = FeedRateBasis.fromValue(rateBasis)
          val calcSummary = if (unitRate > 0.0 && !totalAmountManuallyEdited) {
            val qtyStr = if (basisEnum == FeedRateBasis.PER_KG) {
              "${formatDecimal(weightKg, 0)} kg"
            } else {
              "$bags bags"
            }
            "$qtyStr × ${formatCurrency(unitRate)}${basisEnum.unitSuffix} = ${formatCurrency(resolvedTotalAmount)}"
          } else {
            val equivPerBag = if (bags > 0L) resolvedTotalAmount / bags.toDouble() else 0.0
            val equivPerKg = if (weightKg > 0.0) resolvedTotalAmount / weightKg else 0.0
            "Total: ${formatCurrency(resolvedTotalAmount)} (≈ ${formatCurrency(equivPerBag)}/bag • ${formatCurrency(equivPerKg)}/kg)"
          }
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().testTag("feed_dispatch_amount_summary"),
          ) {
            Text(
              text = calcSummary,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
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
        enabled = (isEdit || selectedBatch != null) &&
          bags >= 1L &&
          weightKg >= 1.0 &&
          unitRateValid &&
          totalAmountValid &&
          challanNo.isNotBlank() &&
          dateValid,
        onClick = {
          onSaveDispatch(
            selectedBatch,
            feedType,
            bags,
            weightKg,
            unitRate,
            rateBasis,
            resolvedTotalAmount,
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
  medicineCatalog: List<MedicineCatalogItem> = emptyList(),
  preselectedCatalogItem: MedicineCatalogItem? = null,
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
  val context = LocalContext.current
  val effectiveCatalog = remember(medicineCatalog) {
    if (medicineCatalog.isNotEmpty()) {
      medicineCatalog
    } else {
      runCatching {
        MasterMedicineCatalog.loadFromAssets(context).map { row ->
          MedicineCatalogItem(
            id = row.docId,
            medicineCode = row.medicineCode,
            medicineName = row.medicineName,
            category = row.category,
            packing = row.packing,
            unit = row.unit,
            rate = row.rate,
            perKgOrLtr = row.perKgOrLtr,
          )
        }
      }.getOrDefault(emptyList())
    }
  }

  val isEdit = existingEntry != null
  var selectedBatch by remember { mutableStateOf(preselectedBatch ?: openBatches.firstOrNull()) }
  var batchExpanded by remember { mutableStateOf(false) }

  val initialMatchedCatalog = remember(preselectedCatalogItem, existingEntry, effectiveCatalog) {
    preselectedCatalogItem ?: existingEntry?.medicineName?.let { rawName ->
      val codePrefix = rawName.substringBefore("•").trim().replace(" ", "").uppercase()
      effectiveCatalog.firstOrNull {
        it.medicineCode.trim().replace(" ", "").uppercase() == codePrefix ||
          it.medicineName.equals(rawName.trim(), ignoreCase = true)
      }
    }
  }

  var selectedCatalogItem by remember { mutableStateOf(initialMatchedCatalog) }
  var catalogSearchQuery by remember {
    mutableStateOf(preselectedCatalogItem?.medicineCode ?: "")
  }
  var medicineListExpanded by remember { mutableStateOf(false) }
  var medicineName by remember {
    mutableStateOf(
      existingEntry?.medicineName
        ?: preselectedCatalogItem?.let { "${it.medicineCode} • ${it.medicineName}" }
        ?: "",
    )
  }
  var category by remember {
    mutableStateOf(
      existingEntry?.category
        ?: preselectedCatalogItem?.category
        ?: MedicineCategory.VACCINE.value,
    )
  }
  var categoryExpanded by remember { mutableStateOf(false) }
  var lastAutoDosage by remember {
    mutableStateOf(
      preselectedCatalogItem?.let { item ->
        if (item.unit.isNotBlank()) "1 ${item.unit}" else "1"
      } ?: "",
    )
  }
  var dosageOrQty by remember {
    mutableStateOf(
      existingEntry?.dosageOrQty
        ?: preselectedCatalogItem?.let { item ->
          if (item.unit.isNotBlank()) "1 ${item.unit}" else "1"
        }
        ?: "1",
    )
  }
  var usePerKgOrLtrRate by remember {
    mutableStateOf(
      initialMatchedCatalog != null &&
        initialMatchedCatalog.rate <= 0.0 &&
        initialMatchedCatalog.perKgOrLtr > 0.0,
    )
  }
  val initialCatalogRate = initialMatchedCatalog?.let { item ->
    if (usePerKgOrLtrRate) item.perKgOrLtr else if (item.rate > 0.0) item.rate else item.perKgOrLtr
  } ?: 0.0
  var unitRateText by remember {
    mutableStateOf(
      if (initialCatalogRate > 0.0) String.format(Locale.US, "%.2f", initialCatalogRate) else "",
    )
  }
  var totalCostManuallyEdited by remember {
    mutableStateOf(existingEntry != null)
  }
  var totalCostText by remember {
    mutableStateOf(
      existingEntry?.totalCost?.let { String.format(Locale.US, "%.2f", it) }
        ?: computeMedicineTotalCost(dosageOrQty, initialCatalogRate)?.let { String.format(Locale.US, "%.2f", it) }
        ?: "",
    )
  }
  var invoiceOrSlipNo by remember { mutableStateOf(existingEntry?.invoiceOrSlipNo ?: "MED-${(1000..9999).random()}") }
  var remarks by remember { mutableStateOf(existingEntry?.remarks ?: "") }

  var entryEpochDay by remember { mutableLongStateOf(existingEntry?.entryEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingEntry?.entryEpochDay ?: todayEpochDay)) }
  val dateValid = !canSelectPreviousDate ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)

  val catalogMatches = remember(effectiveCatalog, catalogSearchQuery) {
    filterAndRankMedicineCatalog(effectiveCatalog, catalogSearchQuery)
  }

  val unitRate = (unitRateText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  val unitRateValid = unitRateText.isBlank() || (unitRateText.toDoubleOrNull()?.let { it >= 0.0 } == true)

  fun recalculateAutoCost(nextDosage: String, nextRate: Double) {
    if (!totalCostManuallyEdited) {
      val computed = computeMedicineTotalCost(nextDosage, nextRate)
      totalCostText = if (computed != null) {
        String.format(Locale.US, "%.2f", computed)
      } else if (nextRate > 0.0 && nextDosage.isBlank()) {
        ""
      } else {
        totalCostText
      }
    }
  }

  fun applyCatalogItem(item: MedicineCatalogItem) {
    selectedCatalogItem = item
    medicineName = "${item.medicineCode} • ${item.medicineName}"
    category = item.category
    val autoDose = if (item.unit.isNotBlank()) "1 ${item.unit}" else "1"
    val nextDose = if (dosageOrQty.isBlank() || dosageOrQty == "1" || dosageOrQty == lastAutoDosage) {
      dosageOrQty = autoDose
      lastAutoDosage = autoDose
      autoDose
    } else {
      dosageOrQty
    }
    val preferPerUnit = item.rate <= 0.0 && item.perKgOrLtr > 0.0
    usePerKgOrLtrRate = preferPerUnit
    val nextRate = if (preferPerUnit) item.perKgOrLtr else if (item.rate > 0.0) item.rate else item.perKgOrLtr
    unitRateText = if (nextRate > 0.0) String.format(Locale.US, "%.2f", nextRate) else ""
    totalCostManuallyEdited = false
    val computed = computeMedicineTotalCost(nextDose, nextRate)
    totalCostText = if (computed != null) String.format(Locale.US, "%.2f", computed) else ""
  }

  val totalCost = totalCostText.toDoubleOrNull() ?: -1.0
  val isValid = (isEdit || selectedBatch != null) &&
    medicineName.isNotBlank() &&
    unitRateValid &&
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

        if (effectiveCatalog.isNotEmpty()) {
          OutlinedTextField(
            value = catalogSearchQuery,
            onValueChange = { raw ->
              catalogSearchQuery = raw
              val norm = raw.trim().replace(" ", "").uppercase()
              effectiveCatalog.firstOrNull {
                it.medicineCode.trim().replace(" ", "").uppercase() == norm
              }?.let { exact ->
                applyCatalogItem(exact)
              }
            },
            label = { Text("Search Medicine Code / Name (${effectiveCatalog.size})") },
            placeholder = { Text("Type M008, V001, MENTOFIN — or use dropdown below") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
              if (catalogSearchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { catalogSearchQuery = "" },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Rounded.Close, contentDescription = "Clear medicine search")
                }
              }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().testTag("medicine_catalog_search_input"),
          )

          ExposedDropdownMenuBox(
            expanded = medicineListExpanded,
            onExpandedChange = { medicineListExpanded = it },
          ) {
            OutlinedTextField(
              value = selectedCatalogItem?.let { "${it.medicineCode} • ${it.medicineName}" } ?: "",
              onValueChange = {},
              readOnly = true,
              label = {
                Text(
                  if (catalogSearchQuery.isBlank()) {
                    "Automatic Medicine List (${effectiveCatalog.size}) *"
                  } else {
                    "Filtered Medicine List (${catalogMatches.size}) *"
                  }
                )
              },
              placeholder = { Text("Tap to select from ${effectiveCatalog.size} medicines & vaccines") },
              leadingIcon = { Icon(Icons.Rounded.Medication, contentDescription = null) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = medicineListExpanded) },
              singleLine = true,
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("medicine_list_dropdown"),
            )
            ExposedDropdownMenu(
              expanded = medicineListExpanded,
              onDismissRequest = { medicineListExpanded = false },
              modifier = Modifier.heightIn(max = 280.dp),
            ) {
              if (catalogMatches.isEmpty()) {
                DropdownMenuItem(
                  text = { Text("No matching medicines found") },
                  onClick = { medicineListExpanded = false },
                )
              } else {
                catalogMatches.forEach { item ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(
                          text = "${item.medicineCode} • ${item.medicineName}",
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedCatalogItem?.id == item.id) FontWeight.Bold else FontWeight.Medium,
                          ),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "${item.category} • Pack ${item.packingDisplay} ${item.unitDisplay} • Rate ${item.rateDisplay}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    },
                    onClick = {
                      applyCatalogItem(item)
                      medicineListExpanded = false
                    },
                  )
                }
              }
            }
          }

          if (catalogSearchQuery.isNotBlank() && catalogMatches.isNotEmpty()) {
            val visibleCatalog = catalogMatches.take(25)
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth().heightIn(max = 156.dp),
            ) {
              Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                visibleCatalog.forEachIndexed { index, item ->
                  val selected = selectedCatalogItem?.id == item.id
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .clickable { applyCatalogItem(item) }
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${item.medicineCode} • ${item.medicineName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "Pack ${item.packingDisplay} ${item.unitDisplay} • Rate ${item.rateDisplay} • ${item.perKgOrLtrDisplay}/kg-L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    if (selected) {
                      Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected medicine",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                  if (index < visibleCatalog.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                  }
                }
              }
            }
          }

          selectedCatalogItem?.let { sel ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Auto-filled ${sel.medicineCode}: Pack ${sel.packingDisplay} ${sel.unitDisplay} • Rate ${sel.rateDisplay} • ${sel.perKgOrLtrDisplay}/kg-L",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
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

        selectedCatalogItem?.takeIf {
          it.rate > 0.0 && it.perKgOrLtr > 0.0 && abs(it.rate - it.perKgOrLtr) > 0.01
        }?.let { sel ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Rate Mode:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterChip(
              selected = !usePerKgOrLtrRate,
              onClick = {
                usePerKgOrLtrRate = false
                unitRateText = String.format(Locale.US, "%.2f", sel.rate)
                totalCostManuallyEdited = false
                recalculateAutoCost(dosageOrQty, sel.rate)
              },
              label = { Text("Pack (${formatCurrency(sel.rate)})") },
              modifier = Modifier.testTag("medicine_rate_mode_pack"),
            )
            val perUnitSuffix = sel.unit.ifBlank { "Kg/L" }
            FilterChip(
              selected = usePerKgOrLtrRate,
              onClick = {
                usePerKgOrLtrRate = true
                unitRateText = String.format(Locale.US, "%.2f", sel.perKgOrLtr)
                totalCostManuallyEdited = false
                recalculateAutoCost(dosageOrQty, sel.perKgOrLtr)
              },
              label = { Text("Per $perUnitSuffix (${formatCurrency(sel.perKgOrLtr)})") },
              modifier = Modifier.testTag("medicine_rate_mode_per_unit"),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = dosageOrQty,
            onValueChange = { raw ->
              dosageOrQty = raw
              totalCostManuallyEdited = false
              recalculateAutoCost(raw, unitRate)
            },
            label = { Text("Qty / Dosage") },
            placeholder = { Text("e.g., 2 LTR / 5 PCS") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("medicine_dosage_input"),
          )
          OutlinedTextField(
            value = unitRateText,
            onValueChange = { raw ->
              unitRateText = raw
              totalCostManuallyEdited = false
              val nextRate = (raw.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
              recalculateAutoCost(dosageOrQty, nextRate)
            },
            label = { Text("Unit Rate (₹)") },
            placeholder = { Text("Auto from list") },
            isError = !unitRateValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("medicine_unit_rate_input"),
          )
        }

        OutlinedTextField(
          value = totalCostText,
          onValueChange = { raw ->
            totalCostText = raw
            totalCostManuallyEdited = raw.isNotBlank()
          },
          label = { Text("Total Cost (₹) — Auto *") },
          placeholder = { Text("Calculated automatically") },
          trailingIcon = {
            val parsedQty = extractMedicineQuantity(dosageOrQty)
            if (totalCostManuallyEdited && unitRate > 0.0 && parsedQty != null) {
              TextButton(
                onClick = {
                  totalCostManuallyEdited = false
                  recalculateAutoCost(dosageOrQty, unitRate)
                },
                modifier = Modifier.testTag("medicine_cost_auto_reset_btn"),
              ) {
                Text("Auto")
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("medicine_cost_input"),
        )

        val parsedQty = extractMedicineQuantity(dosageOrQty)
        if ((parsedQty != null && unitRate > 0.0) || totalCost > 0.0) {
          val summaryText = when {
            !totalCostManuallyEdited && parsedQty != null && unitRate > 0.0 && totalCost >= 0.0 -> {
              val qtyLabel = if (parsedQty % 1.0 == 0.0) parsedQty.toLong().toString() else formatDecimal(parsedQty, 2)
              "Auto Total Cost: $qtyLabel × ${formatCurrency(unitRate)} = ${formatCurrency(totalCost)}"
            }
            totalCostManuallyEdited && totalCost > 0.0 -> {
              "Manual Total Cost: ${formatCurrency(totalCost)}"
            }
            else -> null
          }
          summaryText?.let { msg ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
              modifier = Modifier.fillMaxWidth().testTag("medicine_total_cost_summary"),
            ) {
              Text(
                text = msg,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineCatalogDialog(
  existingItem: MedicineCatalogItem? = null,
  onDismiss: () -> Unit,
  onSaveItem: (
    medicineCode: String,
    medicineName: String,
    category: String,
    packing: Double,
    unit: String,
    rate: Double,
    perKgOrLtr: Double,
  ) -> Unit,
) {
  val isEdit = existingItem != null
  var code by remember { mutableStateOf(existingItem?.medicineCode ?: "") }
  var medicineName by remember { mutableStateOf(existingItem?.medicineName ?: "") }
  var category by remember { mutableStateOf(existingItem?.category ?: MedicineCategory.ANTIBIOTIC.value) }
  var categoryExpanded by remember { mutableStateOf(false) }
  var packingText by remember {
    mutableStateOf(existingItem?.packing?.takeIf { it > 0.0 }?.toString() ?: "1.00")
  }
  var unit by remember { mutableStateOf(existingItem?.unit ?: "LTR") }
  var unitExpanded by remember { mutableStateOf(false) }
  var rateText by remember {
    mutableStateOf(existingItem?.rate?.takeIf { it > 0.0 }?.toString() ?: "")
  }
  var perKgOrLtrText by remember {
    mutableStateOf(existingItem?.perKgOrLtr?.takeIf { it > 0.0 }?.toString() ?: "")
  }
  var perKgManuallyEdited by remember { mutableStateOf(isEdit) }

  val unitOptions = listOf("LTR", "KG", "ML", "GRM", "PCS", "TAB", "DOSE")

  fun recomputePerKgIfAuto(packingStr: String, rateStr: String) {
    if (perKgManuallyEdited) return
    val p = packingStr.toDoubleOrNull() ?: 0.0
    val r = rateStr.toDoubleOrNull() ?: 0.0
    perKgOrLtrText = if (p > 0.0 && r > 0.0) {
      String.format(Locale.US, "%.2f", r / p)
    } else if (r > 0.0) {
      String.format(Locale.US, "%.2f", r)
    } else {
      "0.00"
    }
  }

  val packing = (packingText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100000.0)
  val rate = (rateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10000000.0)
  val perKgOrLtr = (perKgOrLtrText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10000000.0)
  val isValid = code.trim().isNotEmpty() && medicineName.trim().isNotEmpty()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEdit) "Update Medicine Item • ${existingItem.medicineCode}" else "Create Medicine / Vaccine Item",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = code,
            onValueChange = { raw ->
              val upper = raw.uppercase()
              code = upper
              if (!isEdit && upper.startsWith("V")) {
                category = MedicineCategory.VACCINE.value
              }
            },
            label = { Text("Code *") },
            placeholder = { Text("e.g., M093, V041") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(0.42f).testTag("med_catalog_code_input"),
          )
          OutlinedTextField(
            value = medicineName,
            onValueChange = { medicineName = it },
            label = { Text("Medicine Name *") },
            placeholder = { Text("e.g., MENTOFIN") },
            singleLine = true,
            modifier = Modifier.weight(0.58f).testTag("med_catalog_name_input"),
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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = packingText,
            onValueChange = { raw ->
              packingText = raw
              recomputePerKgIfAuto(raw, rateText)
            },
            label = { Text("Packing") },
            placeholder = { Text("1.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("med_catalog_packing_input"),
          )
          ExposedDropdownMenuBox(
            expanded = unitExpanded,
            onExpandedChange = { unitExpanded = it },
            modifier = Modifier.weight(1f),
          ) {
            OutlinedTextField(
              value = unit,
              onValueChange = { unit = it.uppercase() },
              label = { Text("Unit") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .testTag("med_catalog_unit_input"),
            )
            ExposedDropdownMenu(
              expanded = unitExpanded,
              onDismissRequest = { unitExpanded = false },
            ) {
              unitOptions.forEach { u ->
                DropdownMenuItem(
                  text = { Text(u) },
                  onClick = {
                    unit = u
                    unitExpanded = false
                  },
                )
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = rateText,
            onValueChange = { raw ->
              rateText = raw
              recomputePerKgIfAuto(packingText, raw)
            },
            label = { Text("Rate (₹)") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("med_catalog_rate_input"),
          )
          OutlinedTextField(
            value = perKgOrLtrText,
            onValueChange = { raw ->
              perKgOrLtrText = raw
              perKgManuallyEdited = true
            },
            label = { Text("Per Kg/Ltr (₹)") },
            placeholder = { Text("Auto") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("med_catalog_per_unit_input"),
          )
        }
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = {
          onSaveItem(
            code.trim().uppercase(),
            medicineName.trim(),
            category,
            packing,
            unit.trim().uppercase(),
            rate,
            perKgOrLtr,
          )
        },
        modifier = Modifier.testTag("confirm_med_catalog_button"),
      ) {
        Text(if (isEdit) "Update Item" else "Save Medicine Item")
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
