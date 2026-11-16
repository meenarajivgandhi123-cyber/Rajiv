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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountTypeOption
import com.example.data.model.DailyLog
import com.example.data.model.FarmTypeOption
import com.example.data.model.Farmer
import com.example.data.model.FlockBatch
import com.example.data.model.IntegrationZone
import com.example.data.model.ShedType
import com.example.ui.theme.MonoMetricMedium
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun ZoneFormDialog(
  existingZone: IntegrationZone? = null,
  onDismiss: () -> Unit,
  onSave: (zoneCode: String, zoneName: String, region: String, supervisorName: String) -> Unit,
) {
  var zoneCode by remember { mutableStateOf(existingZone?.zoneCode ?: "ZN-${(10..99).random()}") }
  var zoneName by remember { mutableStateOf(existingZone?.zoneName ?: "") }
  var region by remember { mutableStateOf(existingZone?.region ?: "") }
  var supervisorName by remember { mutableStateOf(existingZone?.supervisorName ?: "") }

  val isValid = zoneCode.isNotBlank() && zoneName.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existingZone == null) "Create Integration Zone" else "Edit Zone") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = zoneCode,
          onValueChange = { zoneCode = it.uppercase() },
          label = { Text("Zone Code *") },
          placeholder = { Text("e.g., HR-01") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          modifier = Modifier.fillMaxWidth().testTag("zone_code_input"),
        )
        OutlinedTextField(
          value = zoneName,
          onValueChange = { zoneName = it.uppercase() },
          label = { Text("Zone Name *") },
          placeholder = { Text("e.g., HR ZONE") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          modifier = Modifier.fillMaxWidth().testTag("zone_name_input"),
        )
        OutlinedTextField(
          value = region,
          onValueChange = { region = it.uppercase() },
          label = { Text("Region / State") },
          placeholder = { Text("e.g., HARYANA") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          modifier = Modifier.fillMaxWidth().testTag("zone_region_input"),
        )
        OutlinedTextField(
          value = supervisorName,
          onValueChange = { supervisorName = it },
          label = { Text("Zone Supervisor") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = { onSave(zoneCode, zoneName, region, supervisorName) },
        modifier = Modifier.testTag("save_zone_button"),
      ) {
        Text(if (existingZone == null) "Save Zone" else "Update")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerFormDialog(
  zones: List<IntegrationZone>,
  existingFarmer: Farmer? = null,
  onDismiss: () -> Unit,
  onSave: (
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
  ) -> Unit,
) {
  var farmCode by remember { mutableStateOf(existingFarmer?.farmCode ?: "FRM-${(101..999).random()}") }
  var farmerName by remember { mutableStateOf(existingFarmer?.farmerName ?: "") }
  var farmName by remember { mutableStateOf(existingFarmer?.farmName ?: "") }
  var phone by remember { mutableStateOf(existingFarmer?.phone ?: "") }
  var village by remember { mutableStateOf(existingFarmer?.village ?: "") }
  var tehsil by remember { mutableStateOf(existingFarmer?.tehsil ?: "") }
  val matchedZone = zones.firstOrNull {
    it.id == existingFarmer?.zoneId ||
      (existingFarmer?.zoneName?.isNotBlank() == true && it.zoneName.equals(existingFarmer.zoneName, ignoreCase = true))
  }
  var selectedZone by remember { mutableStateOf(matchedZone) }
  var zoneExpanded by remember { mutableStateOf(false) }
  var region by remember { mutableStateOf(existingFarmer?.region ?: matchedZone?.region ?: "") }
  var farmType by remember { mutableStateOf(existingFarmer?.farmType?.ifBlank { FarmTypeOption.OWN_FARM.value } ?: FarmTypeOption.OWN_FARM.value) }
  var farmTypeExpanded by remember { mutableStateOf(false) }

  var panNumber by remember { mutableStateOf(existingFarmer?.panNumber ?: "") }
  var aadhaarNumber by remember { mutableStateOf(existingFarmer?.aadhaarNumber ?: "") }
  var bankName by remember { mutableStateOf(existingFarmer?.bankName ?: "") }
  var accountNumber by remember { mutableStateOf(existingFarmer?.accountNumber ?: "") }
  var ifscCode by remember { mutableStateOf(existingFarmer?.ifscCode ?: "") }
  var accountType by remember { mutableStateOf(existingFarmer?.accountType?.ifBlank { AccountTypeOption.SAVING.value } ?: AccountTypeOption.SAVING.value) }
  var accountTypeExpanded by remember { mutableStateOf(false) }

  var shedType by remember { mutableStateOf(existingFarmer?.shedType ?: ShedType.OPEN_SIDED.value) }
  var totalSqftText by remember {
    mutableStateOf(
      (existingFarmer?.totalSqft?.takeIf { it > 0L } ?: existingFarmer?.shedCapacity ?: 6000L).toString()
    )
  }
  var shedCapacityText by remember { mutableStateOf((existingFarmer?.shedCapacity ?: 6000L).toString()) }
  var rateText by remember { mutableStateOf((existingFarmer?.growingRatePerKg ?: 9.0).toString()) }
  var shedExpanded by remember { mutableStateOf(false) }

  val isValid = farmCode.isNotBlank() &&
    farmerName.isNotBlank() &&
    (shedCapacityText.toLongOrNull() ?: 0L) >= 100L &&
    (rateText.toDoubleOrNull() ?: -1.0) >= 0.0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingFarmer == null) "Create Farmer Profile" else "Edit Farmer Profile",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = farmCode,
          onValueChange = { farmCode = it.uppercase() },
          label = { Text("Farm Code *") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          modifier = Modifier.fillMaxWidth().testTag("farmer_code_input"),
        )
        OutlinedTextField(
          value = farmerName,
          onValueChange = { farmerName = it },
          label = { Text("Farmer Full Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("farmer_name_input"),
        )
        OutlinedTextField(
          value = farmName,
          onValueChange = { farmName = it },
          label = { Text("Farm Name") },
          placeholder = { Text("e.g., Balaji Poultry Farm") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = region,
            onValueChange = { region = it.uppercase() },
            label = { Text("Region") },
            placeholder = { Text("e.g., UP, HR, RJ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(0.42f).testTag("farmer_region_input"),
          )
          ExposedDropdownMenuBox(
            expanded = farmTypeExpanded,
            onExpandedChange = { farmTypeExpanded = it },
            modifier = Modifier.weight(0.58f),
          ) {
            OutlinedTextField(
              value = farmType,
              onValueChange = {},
              readOnly = true,
              label = { Text("Farm Type") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = farmTypeExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("farmer_farm_type_input"),
            )
            ExposedDropdownMenu(
              expanded = farmTypeExpanded,
              onDismissRequest = { farmTypeExpanded = false },
            ) {
              FarmTypeOption.options.forEach { option ->
                DropdownMenuItem(
                  text = { Text(option) },
                  onClick = {
                    farmType = option
                    farmTypeExpanded = false
                  },
                )
              }
            }
          }
        }
        ExposedDropdownMenuBox(
          expanded = zoneExpanded,
          onExpandedChange = { zoneExpanded = it },
        ) {
          OutlinedTextField(
            value = selectedZone?.let { "${it.zoneCode} • ${it.zoneName}" }
              ?: existingFarmer?.zoneName?.takeIf { it.isNotBlank() }
              ?: "Unassigned",
            onValueChange = {},
            readOnly = true,
            label = { Text("Branch / Integration Zone") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = zoneExpanded,
            onDismissRequest = { zoneExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("Unassigned") },
              onClick = {
                selectedZone = null
                zoneExpanded = false
              },
            )
            zones.forEach { z ->
              DropdownMenuItem(
                text = { Text("${z.zoneCode} • ${z.zoneName} (${z.region})") },
                onClick = {
                  selectedZone = z
                  if (region.isBlank()) region = z.region
                  zoneExpanded = false
                },
              )
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = village,
            onValueChange = { village = it },
            label = { Text("Village") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = tehsil,
            onValueChange = { tehsil = it },
            label = { Text("Tehsil") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("farmer_tehsil_input"),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone No") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = totalSqftText,
            onValueChange = { raw ->
              totalSqftText = raw
              val parsed = raw.toLongOrNull()
              if (parsed != null && parsed >= 100L) {
                shedCapacityText = parsed.coerceAtMost(500000L).toString()
              }
            },
            label = { Text("Total Sq.Ft") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("farmer_total_sqft_input"),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = aadhaarNumber,
            onValueChange = { aadhaarNumber = it },
            label = { Text("Aadhaar No") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("farmer_aadhaar_input"),
          )
          OutlinedTextField(
            value = panNumber,
            onValueChange = { panNumber = it.uppercase() },
            label = { Text("PAN No") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Bank Name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = { Text("Account No") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = ifscCode,
            onValueChange = { ifscCode = it.uppercase() },
            label = { Text("IFSC Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(1f),
          )
          ExposedDropdownMenuBox(
            expanded = accountTypeExpanded,
            onExpandedChange = { accountTypeExpanded = it },
            modifier = Modifier.weight(1f),
          ) {
            OutlinedTextField(
              value = accountType,
              onValueChange = {},
              readOnly = true,
              label = { Text("A/C Type") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountTypeExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("farmer_account_type_input"),
            )
            ExposedDropdownMenu(
              expanded = accountTypeExpanded,
              onDismissRequest = { accountTypeExpanded = false },
            ) {
              AccountTypeOption.options.forEach { option ->
                DropdownMenuItem(
                  text = { Text(option) },
                  onClick = {
                    accountType = option
                    accountTypeExpanded = false
                  },
                )
              }
            }
          }
        }
        ExposedDropdownMenuBox(
          expanded = shedExpanded,
          onExpandedChange = { shedExpanded = it },
        ) {
          OutlinedTextField(
            value = shedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Shed Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shedExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = shedExpanded,
            onDismissRequest = { shedExpanded = false },
          ) {
            ShedType.options.forEach { option ->
              DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                  shedType = option
                  shedExpanded = false
                },
              )
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = shedCapacityText,
            onValueChange = { shedCapacityText = it },
            label = { Text("Bird Capacity *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            label = { Text("Rate ₹/kg *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = {
          val resolvedCap = shedCapacityText.toLongOrNull()?.coerceIn(100L, 500000L) ?: 5000L
          val resolvedSqft = (totalSqftText.toLongOrNull() ?: resolvedCap).coerceIn(0L, 500000L)
          onSave(
            farmCode,
            farmerName,
            farmName,
            phone,
            village,
            tehsil,
            region,
            farmType,
            selectedZone?.id ?: "",
            selectedZone?.zoneName ?: (existingFarmer?.zoneName ?: ""),
            panNumber,
            aadhaarNumber,
            accountNumber,
            bankName,
            ifscCode,
            accountType,
            shedType,
            resolvedCap,
            resolvedSqft,
            rateText.toDoubleOrNull() ?: 9.0,
          )
        },
        modifier = Modifier.testTag("save_farmer_button"),
      ) {
        Text(if (existingFarmer == null) "Save Farmer" else "Update")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun CancelFarmDialog(
  farmer: Farmer,
  onDismiss: () -> Unit,
  onConfirmCancel: (reason: String) -> Unit,
) {
  var reason by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Move to Cancel Farm List") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Cancelling ${farmer.farmerName} (${farmer.farmCode}) moves this shed into the Cancel Farm List.",
          style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Cancellation Reason") },
          placeholder = { Text("e.g., Biosecurity rest, contract ended, roof repair") },
          modifier = Modifier.fillMaxWidth().testTag("cancel_farm_reason_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmCancel(reason.ifBlank { "Contract paused by integrator" }) },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_cancel_farm_button"),
      ) {
        Text("Cancel Farm")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Back") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchDialog(
  eligibleFarmers: List<Farmer>,
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  preselectedFarmer: Farmer? = null,
  existingBatch: FlockBatch? = null,
  onDismiss: () -> Unit,
  onCreateBatch: (
    farmer: Farmer?,
    batchCode: String,
    breed: String,
    chicksPlaced: Long,
    freeChicks: Long,
    requestedPlacementEpochDay: Long?,
    targetMarketAgeDays: Long,
    targetAvgWeightKg: Double,
    standardFcr: Double,
    growingRatePerKg: Double,
    region: String,
    zoneName: String,
    farmName: String,
    weekNumber: String,
    challanNo: String,
  ) -> Unit,
) {
  var selectedFarmer by remember { mutableStateOf(preselectedFarmer ?: eligibleFarmers.firstOrNull()) }
  var farmerDropdownExpanded by remember { mutableStateOf(false) }
  var batchCode by remember { mutableStateOf(existingBatch?.batchCode ?: "BCH-${(1000..9999).random()}") }
  var farmName by remember {
    mutableStateOf(
      existingBatch?.displayFarmName
        ?: (preselectedFarmer ?: eligibleFarmers.firstOrNull())?.displayFarmName
        ?: ""
    )
  }
  var region by remember {
    mutableStateOf(
      existingBatch?.region?.ifBlank { (preselectedFarmer ?: eligibleFarmers.firstOrNull())?.region ?: "" }
        ?: (preselectedFarmer ?: eligibleFarmers.firstOrNull())?.region
        ?: ""
    )
  }
  var zoneName by remember {
    mutableStateOf(
      existingBatch?.zoneName?.ifBlank { (preselectedFarmer ?: eligibleFarmers.firstOrNull())?.zoneName ?: "" }
        ?: (preselectedFarmer ?: eligibleFarmers.firstOrNull())?.zoneName
        ?: ""
    )
  }
  var weekNumber by remember { mutableStateOf(existingBatch?.weekNumber ?: "") }
  var challanNo by remember { mutableStateOf(existingBatch?.challanNo ?: "") }
  var breed by remember { mutableStateOf(existingBatch?.breed ?: "Cobb 500") }
  var chicksText by remember { mutableStateOf((existingBatch?.chicksPlaced ?: selectedFarmer?.shedCapacity ?: 5000L).toString()) }
  var freeChicksText by remember { mutableStateOf((existingBatch?.freeChicks ?: 100L).toString()) }
  var placementEpochDay by remember { mutableLongStateOf(existingBatch?.placementEpochDay ?: todayEpochDay) }
  var hatchDateText by remember {
    mutableStateOf(formatEpochDayDdMmYyyy(existingBatch?.placementEpochDay ?: todayEpochDay))
  }
  var targetAgeText by remember { mutableStateOf((existingBatch?.targetMarketAgeDays ?: 38L).toString()) }
  var targetWeightText by remember { mutableStateOf((existingBatch?.targetAvgWeightKg ?: 2.2).toString()) }
  var standardFcrText by remember { mutableStateOf((existingBatch?.standardFcr ?: 1.62).toString()) }
  var rateText by remember { mutableStateOf((existingBatch?.growingRatePerKg ?: selectedFarmer?.growingRatePerKg ?: 9.0).toString()) }

  val computedAutoAge = max(0L, todayEpochDay - placementEpochDay)
  val isEdit = existingBatch != null
  val dateValid = !canSelectPreviousDate || (parseDdMmYyyy(hatchDateText)?.let { it <= todayEpochDay } == true)
  val doldVal = chicksText.toLongOrNull() ?: 0L
  val freeVal = freeChicksText.toLongOrNull() ?: 0L
  val totalHoused = (doldVal + freeVal).coerceAtLeast(0L)
  val isValid = (isEdit || selectedFarmer != null) &&
    batchCode.isNotBlank() &&
    breed.isNotBlank() &&
    (chicksText.toLongOrNull()?.let { it >= 0L } == true) &&
    dateValid

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEdit) "Update Batch • ${existingBatch.batchCode}" else "Create Broiler Batch",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      if (!isEdit && eligibleFarmers.isEmpty()) {
        Text(
          text = "No Empty Farms available for placement. Create a farmer or mark a farm Empty first.",
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (!isEdit) {
            ExposedDropdownMenuBox(
              expanded = farmerDropdownExpanded,
              onExpandedChange = { farmerDropdownExpanded = it },
            ) {
              OutlinedTextField(
                value = selectedFarmer?.let { "${it.farmCode} • ${it.displayFarmName}" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Empty Farm *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = farmerDropdownExpanded) },
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth(),
              )
              ExposedDropdownMenu(
                expanded = farmerDropdownExpanded,
                onDismissRequest = { farmerDropdownExpanded = false },
              ) {
                eligibleFarmers.forEach { farmer ->
                  DropdownMenuItem(
                    text = { Text("${farmer.farmCode} • ${farmer.displayFarmName} (${farmer.shedCapacity} cap)") },
                    onClick = {
                      selectedFarmer = farmer
                      farmName = farmer.displayFarmName
                      region = farmer.region
                      zoneName = farmer.zoneName
                      chicksText = farmer.shedCapacity.toString()
                      rateText = farmer.growingRatePerKg.toString()
                      farmerDropdownExpanded = false
                    },
                  )
                }
              }
            }
          }
          val displayFarmCode = existingBatch?.farmCode ?: selectedFarmer?.farmCode ?: ""
          val displayFarmTitle = farmName.ifBlank { existingBatch?.displayFarmName ?: selectedFarmer?.displayFarmName ?: "" }
          if (displayFarmCode.isNotBlank() || displayFarmTitle.isNotBlank()) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Farm ID: $displayFarmCode • Farm Name: $displayFarmTitle",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(10.dp).testTag("batch_farm_identity_text"),
              )
            }
          }
          OutlinedTextField(
            value = batchCode,
            onValueChange = { batchCode = it.uppercase() },
            label = { Text("Batch / Flock ID *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("batch_code_input"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = region,
              onValueChange = { region = it.uppercase() },
              label = { Text("Region") },
              placeholder = { Text("e.g., UP, UKKH") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              modifier = Modifier.weight(0.4f).testTag("batch_region_input"),
            )
            OutlinedTextField(
              value = zoneName,
              onValueChange = { zoneName = it.uppercase() },
              label = { Text("Branch / Zone") },
              placeholder = { Text("e.g., ROORKEE") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              modifier = Modifier.weight(0.6f).testTag("batch_branch_input"),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = weekNumber,
              onValueChange = { weekNumber = it.uppercase() },
              label = { Text("Week No. (WEEK N)") },
              placeholder = { Text("e.g., WEEK 43") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              modifier = Modifier.weight(1f).testTag("batch_week_input"),
            )
            OutlinedTextField(
              value = challanNo,
              onValueChange = { challanNo = it },
              label = { Text("Challan No.") },
              placeholder = { Text("e.g., 143") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("batch_challan_input"),
            )
          }
          if (canSelectPreviousDate) {
            OutlinedTextField(
              value = hatchDateText,
              onValueChange = { raw ->
                hatchDateText = raw
                parseDdMmYyyy(raw)?.let { parsed ->
                  if (parsed <= todayEpochDay) {
                    placementEpochDay = parsed
                  }
                }
              },
              label = { Text("Hatch / Placement Date (DD-MM-YYYY)") },
              supportingText = {
                Text(
                  if (dateValid) "Auto-Calculated Flock Age: Day $computedAutoAge"
                  else "Enter valid DD-MM-YYYY date not after today"
                )
              },
              isError = !dateValid,
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("batch_hatch_date_input"),
            )
          } else {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Placement Date: ${formatEpochDay(placementEpochDay)} • Auto Age: Day $computedAutoAge",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
          OutlinedTextField(
            value = breed,
            onValueChange = { breed = it },
            label = { Text("Chick Strain / Breed *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = chicksText,
              onValueChange = { chicksText = it },
              label = { Text("D.O.L.D. Chicks *") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = freeChicksText,
              onValueChange = { freeChicksText = it },
              label = { Text("Free Chicks") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
          }
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Total Housed: $totalHoused birds (D.O.L.D. $doldVal + Free $freeVal)",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(10.dp).testTag("batch_total_housed_text"),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = targetAgeText,
              onValueChange = { targetAgeText = it },
              label = { Text("Target Age (d)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = targetWeightText,
              onValueChange = { targetWeightText = it },
              label = { Text("Target Wt (kg)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = standardFcrText,
              onValueChange = { standardFcrText = it },
              label = { Text("Std FCR") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = rateText,
              onValueChange = { rateText = it },
              label = { Text("Rate ₹/kg") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    },
    confirmButton = {
      if (isEdit || eligibleFarmers.isNotEmpty()) {
        Button(
          enabled = isValid,
          onClick = {
            onCreateBatch(
              selectedFarmer,
              batchCode,
              breed,
              (chicksText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
              (freeChicksText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
              if (canSelectPreviousDate) placementEpochDay else null,
              (targetAgeText.toLongOrNull() ?: 38L).coerceIn(21L, 70L),
              (targetWeightText.toDoubleOrNull() ?: 2.2).coerceIn(1.0, 5.0),
              (standardFcrText.toDoubleOrNull() ?: 1.62).coerceIn(1.1, 2.5),
              (rateText.toDoubleOrNull() ?: selectedFarmer?.growingRatePerKg ?: 9.0).coerceAtLeast(0.0),
              region,
              zoneName,
              farmName.ifBlank { selectedFarmer?.displayFarmName ?: "" },
              weekNumber,
              challanNo,
            )
          },
          modifier = Modifier.testTag("confirm_create_batch_button"),
        ) {
          Text(if (isEdit) "Update Batch" else "Place Batch")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun DailyLogDialog(
  batch: FlockBatch,
  todayEpochDay: Long,
  canSelectPreviousDate: Boolean = false,
  existingLog: DailyLog? = null,
  onDismiss: () -> Unit,
  onSaveLog: (
    requestedEpochDay: Long?,
    mortalityCount: Long,
    cullCount: Long,
    feedConsumedKg: Double,
    avgWeightGrams: Double,
    shedTempCelsius: Double,
    looseBirds: Long,
    feederStock: Long,
    notes: String,
  ) -> Unit,
) {
  var entryEpochDay by remember { mutableLongStateOf(existingLog?.logEpochDay ?: todayEpochDay) }
  var dateText by remember { mutableStateOf(formatEpochDayDdMmYyyy(existingLog?.logEpochDay ?: todayEpochDay)) }
  val autoAgeDays = existingLog?.dayNumber ?: batch.currentAgeDays(entryEpochDay)

  var mortalityText by remember { mutableStateOf((existingLog?.mortalityCount ?: 0L).toString()) }
  var cullsText by remember { mutableStateOf((existingLog?.cullCount ?: 0L).toString()) }
  var feedKgText by remember { mutableStateOf((existingLog?.feedConsumedKg ?: 150.0).toString()) }
  var weightGramsText by remember {
    mutableStateOf((existingLog?.avgWeightGrams ?: batch.latestAvgWeightGrams).toInt().toString())
  }
  var tempText by remember { mutableStateOf((existingLog?.shedTempCelsius ?: 28.5).toString()) }
  var looseBirdsText by remember { mutableStateOf((existingLog?.looseBirds ?: 0L).toString()) }
  var feederStockText by remember { mutableStateOf((existingLog?.feederStock ?: 0L).toString()) }
  var notes by remember { mutableStateOf(existingLog?.notes ?: "") }

  val dateValid = !(canSelectPreviousDate && existingLog == null) ||
    (parseDdMmYyyy(dateText)?.let { it in 18000L..todayEpochDay } == true)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existingLog == null) "Daily Entry • ${batch.batchCode}" else "Update Day ${existingLog.dayNumber} Entry") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
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
              Text(
                text = "Auto-Calculated Age",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "Date: ${formatEpochDay(entryEpochDay)} • Hatch: ${formatEpochDay(batch.placementEpochDay)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Text(
              text = "Day $autoAgeDays",
              style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
        if (canSelectPreviousDate && existingLog == null) {
          OutlinedTextField(
            value = dateText,
            onValueChange = { raw ->
              dateText = raw
              parseDdMmYyyy(raw)?.let { parsed ->
                if (parsed in 18000L..todayEpochDay) {
                  entryEpochDay = parsed
                }
              }
            },
            label = { Text("Entry Date (DD-MM-YYYY)") },
            supportingText = {
              Text(
                if (dateValid) "Previous date permitted by Super Admin"
                else "Enter valid DD-MM-YYYY date (today or earlier)"
              )
            },
            isError = !dateValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("daily_entry_date_input"),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = mortalityText,
            onValueChange = { mortalityText = it },
            label = { Text("Mortality") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = cullsText,
            onValueChange = { cullsText = it },
            label = { Text("Culls") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = feedKgText,
            onValueChange = { feedKgText = it },
            label = { Text("Feed Consumed (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = weightGramsText,
            onValueChange = { weightGramsText = it },
            label = { Text("Avg Wt (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = looseBirdsText,
            onValueChange = { looseBirdsText = it },
            label = { Text("Loose Birds") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = feederStockText,
            onValueChange = { feederStockText = it },
            label = { Text("Feeder Stock") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = tempText,
          onValueChange = { tempText = it },
          label = { Text("Shed Temp (°C)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Observation / Medication Notes") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = dateValid,
        onClick = {
          onSaveLog(
            if (canSelectPreviousDate && existingLog == null) entryEpochDay else null,
            (mortalityText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
            (cullsText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
            (feedKgText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            (weightGramsText.toDoubleOrNull() ?: batch.latestAvgWeightGrams).coerceAtLeast(0.0),
            (tempText.toDoubleOrNull() ?: 28.0).coerceIn(0.0, 60.0),
            (looseBirdsText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
            (feederStockText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
            notes,
          )
        },
        modifier = Modifier.testTag("save_daily_log_button"),
      ) {
        Text(if (existingLog == null) "Save Daily Entry" else "Update Entry")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun ConfirmDeleteDialog(
  title: String,
  message: String,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text("Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

internal fun parseDdMmYyyy(input: String): Long? {
  return try {
    val parsed = LocalDate.parse(input.trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US))
    parsed.toEpochDay()
  } catch (_: Exception) {
    null
  }
}

internal fun formatEpochDayDdMmYyyy(epochDay: Long): String {
  return try {
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US))
  } catch (_: Exception) {
    epochDay.toString()
  }
}
