package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.DEFAULT_SUPER_ADMIN_EMAIL
import com.example.data.model.OperatorAccount
import com.example.data.model.OperatorRole
import com.example.data.model.isValidMappedEmail
import com.example.data.model.normalizeMappedEmail
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.MonoMetricMedium

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffScreen(
  operators: List<OperatorAccount>,
  currentOperatorId: String,
  workspaceEmail: String,
  onCreateOperator: (
    empCode: String,
    fullName: String,
    role: OperatorRole,
    password: String,
    allowPreviousDateEntry: Boolean,
    mappedEmail: String,
  ) -> Unit,
  onUpdateOperator: (
    operatorId: String,
    fullName: String,
    role: OperatorRole,
    isActive: Boolean,
    newPassword: String?,
    allowPreviousDateEntry: Boolean,
    mappedEmail: String,
  ) -> Unit,
  onDeleteOperator: (OperatorAccount) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var editingOperator by remember { mutableStateOf<OperatorAccount?>(null) }

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
            text = "Staff & Email Permissions",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Manage Super Admin, Admin, User logins, mapped Google Emails & backdated entry permissions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = { showCreateDialog = true },
          modifier = Modifier.testTag("add_staff_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(6.dp))
          Text("Add Staff")
        }
      }
    }

    item {
      val cleanWorkspaceEmail = normalizeMappedEmail(workspaceEmail)
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "App Email ID Mapping for Super Admin & Staff",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Default Super Admin Email: $DEFAULT_SUPER_ADMIN_EMAIL",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
            if (cleanWorkspaceEmail.isNotBlank()) {
              Text(
                text = "Current Google Sign-In: $cleanWorkspaceEmail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = "Any active account with a Mapped Google Email ID auto-grants that role's permissions when signed into Google with that email.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (operators.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Security,
          title = "No Staff Accounts",
          description = "Create Super Admin, Admin, and User employee codes or map Google Email IDs.",
          actionLabel = "Add Staff",
          onAction = { showCreateDialog = true },
        )
      }
    } else {
      items(operators, key = { it.id }) { op ->
        val statusColors = LocalStatusColors.current
        val isSelf = op.id == currentOperatorId
        val isPrimarySuperAdminEmail = op.normalizedMappedEmail == DEFAULT_SUPER_ADMIN_EMAIL
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = op.empCode,
                  style = MonoMetricMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                StatusPill(
                  text = op.roleEnum.label,
                  background = when (op.roleEnum) {
                    OperatorRole.SUPER_ADMIN -> statusColors.closingBg
                    OperatorRole.ADMIN -> statusColors.activeBg
                    OperatorRole.USER -> statusColors.emptyBg
                  },
                  foreground = when (op.roleEnum) {
                    OperatorRole.SUPER_ADMIN -> statusColors.closingFg
                    OperatorRole.ADMIN -> statusColors.activeFg
                    OperatorRole.USER -> statusColors.emptyFg
                  },
                )
                if (op.canSelectPreviousDate) {
                  StatusPill(
                    text = "Prev Date Allowed",
                    background = MaterialTheme.colorScheme.tertiaryContainer,
                    foreground = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                }
                if (isPrimarySuperAdminEmail) {
                  StatusPill(
                    text = "Primary Super Admin Email",
                    background = MaterialTheme.colorScheme.primaryContainer,
                    foreground = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                if (!op.isActive) {
                  StatusPill(
                    text = "Disabled",
                    background = statusColors.cancelledBg,
                    foreground = statusColors.cancelledFg,
                  )
                }
              }
              Text(
                text = op.fullName + if (isSelf) " (You)" else "",
                style = MaterialTheme.typography.titleMedium,
              )
              if (op.mappedEmail.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "Mapped Email: ${op.mappedEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              Text(
                text = op.roleEnum.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row {
              IconButton(
                onClick = { editingOperator = op },
                modifier = Modifier.testTag("edit_staff_${op.empCode}"),
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${op.empCode}")
              }
              if (!isSelf) {
                IconButton(
                  onClick = { onDeleteOperator(op) },
                  modifier = Modifier.testTag("delete_staff_${op.empCode}"),
                ) {
                  Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${op.empCode}",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    OperatorFormDialog(
      existing = null,
      workspaceEmail = workspaceEmail,
      onDismiss = { showCreateDialog = false },
      onSave = { code, name, role, password, _, allowPrev, mappedEmail ->
        showCreateDialog = false
        onCreateOperator(code, name, role, password, allowPrev, mappedEmail)
      },
    )
  }

  editingOperator?.let { target ->
    OperatorFormDialog(
      existing = target,
      workspaceEmail = workspaceEmail,
      onDismiss = { editingOperator = null },
      onSave = { _, name, role, newPassword, isActive, allowPrev, mappedEmail ->
        editingOperator = null
        onUpdateOperator(
          target.id,
          name,
          role,
          isActive,
          newPassword.ifBlank { null },
          allowPrev,
          mappedEmail,
        )
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperatorFormDialog(
  existing: OperatorAccount?,
  workspaceEmail: String,
  onDismiss: () -> Unit,
  onSave: (
    empCode: String,
    fullName: String,
    role: OperatorRole,
    password: String,
    isActive: Boolean,
    allowPreviousDateEntry: Boolean,
    mappedEmail: String,
  ) -> Unit,
) {
  var empCode by remember { mutableStateOf(existing?.empCode ?: "") }
  var fullName by remember { mutableStateOf(existing?.fullName ?: "") }
  var role by remember { mutableStateOf(existing?.roleEnum ?: OperatorRole.USER) }
  var password by remember { mutableStateOf("") }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
  var allowPrevDate by remember { mutableStateOf(existing?.allowPreviousDateEntry ?: false) }
  var mappedEmail by remember { mutableStateOf(existing?.mappedEmail ?: "") }
  var roleExpanded by remember { mutableStateOf(false) }

  val cleanWorkspaceEmail = normalizeMappedEmail(workspaceEmail)
  val emailValid = isValidMappedEmail(mappedEmail)
  val isValid = (existing != null || empCode.trim().length >= 2) &&
    fullName.isNotBlank() &&
    emailValid &&
    (existing != null && password.isBlank() || password.length >= 4)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (existing == null) "Create Employee Account" else "Edit ${existing.empCode}")
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (existing == null) {
          OutlinedTextField(
            value = empCode,
            onValueChange = { empCode = it.uppercase() },
            label = { Text("Employee Code *") },
            placeholder = { Text("e.g., ADM-101 or USR-201") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().testTag("staff_emp_code_input"),
          )
        }
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("staff_name_input"),
        )
        ExposedDropdownMenuBox(
          expanded = roleExpanded,
          onExpandedChange = { roleExpanded = it },
        ) {
          OutlinedTextField(
            value = role.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Role *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = roleExpanded,
            onDismissRequest = { roleExpanded = false },
          ) {
            OperatorRole.entries.forEach { option ->
              DropdownMenuItem(
                text = { Text("${option.label} — ${option.subtitle}") },
                onClick = {
                  role = option
                  roleExpanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = mappedEmail,
          onValueChange = { mappedEmail = it.trim().lowercase() },
          label = { Text("Mapped Google Email ID (Optional)") },
          placeholder = { Text(DEFAULT_SUPER_ADMIN_EMAIL) },
          leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
          isError = !emailValid,
          supportingText = {
            if (!emailValid) {
              Text("Enter a valid Google Email ID (e.g., name@gmail.com)")
            } else {
              Text("Auto-grants ${role.label} permission when signed in with this Email ID")
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth().testTag("staff_mapped_email_input"),
        )
        if (cleanWorkspaceEmail.contains("@") || mappedEmail.isNotBlank()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (cleanWorkspaceEmail.contains("@") && normalizeMappedEmail(mappedEmail) != cleanWorkspaceEmail) {
              OutlinedButton(
                onClick = { mappedEmail = cleanWorkspaceEmail },
                modifier = Modifier.weight(1f),
              ) {
                Text("Use Signed-In Email")
              }
            }
            if (mappedEmail.isNotBlank()) {
              TextButton(onClick = { mappedEmail = "" }) {
                Text("Clear Email")
              }
            }
          }
        }
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(if (existing == null) "Password (min 4 chars) *" else "New Password (leave blank to keep)") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("staff_password_input"),
        )
        if (role == OperatorRole.SUPER_ADMIN) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Super Admin always has Previous Date Entry access and full App permissions.",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text("Allow Previous Date Entry (${role.label})", style = MaterialTheme.typography.bodyMedium)
              Text(
                text = "Must be enabled by Super Admin for ${role.label} to select past dates",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = allowPrevDate,
              onCheckedChange = { allowPrevDate = it },
              modifier = Modifier.testTag("staff_prev_date_switch"),
            )
          }
        }
        if (existing != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Account Active", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = isActive, onCheckedChange = { isActive = it })
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = {
          onSave(
            empCode,
            fullName,
            role,
            password,
            isActive,
            role == OperatorRole.SUPER_ADMIN || allowPrevDate,
            normalizeMappedEmail(mappedEmail),
          )
        },
        modifier = Modifier.testTag("save_staff_button"),
      ) {
        Text(if (existing == null) "Create Staff" else "Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
