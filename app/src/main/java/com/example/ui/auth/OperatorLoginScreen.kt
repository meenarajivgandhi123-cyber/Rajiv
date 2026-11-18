package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DEFAULT_SUPER_ADMIN_EMAIL
import com.example.data.model.OperatorAccount
import com.example.data.model.OperatorRole
import com.example.data.model.findOperatorForEmailMapping
import com.example.data.model.isValidMappedEmail
import com.example.data.model.normalizeMappedEmail

@Composable
fun OperatorLoginScreen(
  rawEmail: String?,
  workspaceEmail: String,
  operators: List<OperatorAccount>,
  isSaving: Boolean,
  loginError: String?,
  onEmpCodeLogin: (role: OperatorRole, empCode: String, password: String) -> Unit,
  onQuickEmailLogin: (OperatorAccount) -> Unit,
  onBootstrapSuperAdmin: (empCode: String, fullName: String, password: String, mappedEmail: String) -> Unit,
  onClearError: () -> Unit,
  onDisconnectGoogle: () -> Unit,
) {
  val isFirstSetup = operators.isEmpty()
  val cleanSignedInEmail = normalizeMappedEmail(rawEmail)
  val mappedOperator = remember(operators, rawEmail) {
    findOperatorForEmailMapping(operators, rawEmail)
  }

  var selectedRole by remember { mutableStateOf(OperatorRole.SUPER_ADMIN) }
  var empCode by remember { mutableStateOf(if (isFirstSetup) "SA-001" else "") }
  var fullName by remember { mutableStateOf("") }
  var mappedEmail by remember(cleanSignedInEmail) { mutableStateOf(cleanSignedInEmail) }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var localValidationError by remember { mutableStateOf<String?>(null) }

  val displayedError = localValidationError ?: loginError

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          )
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      contentAlignment = Alignment.Center,
    ) {
      val isWide = maxWidth >= 780.dp

      Card(
        modifier = Modifier.widthIn(max = if (isWide) 820.dp else 480.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_spf_logo),
                contentDescription = "SPF Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .size(60.dp)
                  .padding(6.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = if (isFirstSetup) "FIRST-TIME WORKSPACE SETUP" else "SPF STAFF PORTAL LOGIN",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (isFirstSetup) "Create Super Admin Account" else "SPF Portal",
                style = MaterialTheme.typography.headlineMedium,
              )
              Text(
                text = if (isFirstSetup) {
                  "Set your Super Admin Emp Code, Password & Email mapping. You can then create Admin and User employee codes."
                } else {
                  "Select Super Admin, Admin, or User and sign in with Emp Code & Password"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (!isFirstSetup && mappedOperator != null) {
            Surface(
              shape = MaterialTheme.shapes.large,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Mapped Email Permission Detected",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = "${mappedOperator.normalizedMappedEmail} -> ${mappedOperator.roleEnum.label} (${mappedOperator.empCode})",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Button(
                  onClick = { onQuickEmailLogin(mappedOperator) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_email_login_button"),
                ) {
                  Text("Quick Enter as ${mappedOperator.roleEnum.label} (${mappedOperator.empCode})")
                }
              }
            }
          }

          if (!isFirstSetup) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Select Access Role",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (isWide) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  OperatorRole.entries.forEach { role ->
                    RoleChoiceCard(
                      role = role,
                      selected = selectedRole == role,
                      count = operators.count { it.roleEnum == role && it.isActive },
                      onClick = {
                        selectedRole = role
                        localValidationError = null
                        onClearError()
                      },
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              } else {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OperatorRole.entries.forEach { role ->
                    RoleChoiceCard(
                      role = role,
                      selected = selectedRole == role,
                      count = operators.count { it.roleEnum == role && it.isActive },
                      onClick = {
                        selectedRole = role
                        localValidationError = null
                        onClearError()
                      },
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }
          }

          OutlinedTextField(
            value = empCode,
            onValueChange = {
              empCode = it.uppercase()
              localValidationError = null
              onClearError()
            },
            label = { Text("Employee Code (Emp Code)") },
            placeholder = { Text(if (selectedRole == OperatorRole.SUPER_ADMIN) "e.g., SA-001" else "e.g., EMP-101") },
            leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("emp_code_input"),
          )

          if (isFirstSetup) {
            OutlinedTextField(
              value = fullName,
              onValueChange = {
                fullName = it
                localValidationError = null
              },
              label = { Text("Super Admin Full Name") },
              leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("super_admin_name_input"),
            )

            OutlinedTextField(
              value = mappedEmail,
              onValueChange = {
                mappedEmail = it.trim().lowercase()
                localValidationError = null
              },
              label = { Text("Super Admin Mapped Email ID (Optional)") },
              placeholder = { Text(DEFAULT_SUPER_ADMIN_EMAIL) },
              leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
              supportingText = {
                Text("Default Super Admin email $DEFAULT_SUPER_ADMIN_EMAIL is auto-granted permission.")
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("super_admin_email_input"),
            )
          }

          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              localValidationError = null
              onClearError()
            },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password",
                )
              }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("emp_password_input"),
          )

          if (isFirstSetup) {
            OutlinedTextField(
              value = confirmPassword,
              onValueChange = {
                confirmPassword = it
                localValidationError = null
              },
              label = { Text("Confirm Password") },
              leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("super_admin_confirm_password_input"),
            )
          }

          if (!displayedError.isNullOrBlank()) {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = displayedError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Button(
            enabled = !isSaving,
            onClick = {
              if (isFirstSetup) {
                when {
                  empCode.trim().length < 2 -> localValidationError = "Emp Code must be at least 2 characters."
                  fullName.isBlank() -> localValidationError = "Enter Super Admin full name."
                  !isValidMappedEmail(mappedEmail) -> localValidationError = "Enter a valid Email ID or leave blank."
                  password.length < 4 -> localValidationError = "Password must be at least 4 characters."
                  password != confirmPassword -> localValidationError = "Passwords do not match."
                  else -> onBootstrapSuperAdmin(empCode, fullName, password, mappedEmail)
                }
              } else {
                onEmpCodeLogin(selectedRole, empCode, password)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("emp_login_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text = if (isFirstSetup) "Create Super Admin & Enter" else "Login as ${selectedRole.label}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Cloud Workspace: $workspaceEmail",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDisconnectGoogle) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("Switch Google")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RoleChoiceCard(
  role: OperatorRole,
  selected: Boolean,
  count: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val icon: ImageVector = when (role) {
    OperatorRole.SUPER_ADMIN -> Icons.Rounded.Security
    OperatorRole.ADMIN -> Icons.Rounded.AdminPanelSettings
    OperatorRole.USER -> Icons.Rounded.Person
  }
  val title = when (role) {
    OperatorRole.SUPER_ADMIN -> "Super Admin"
    OperatorRole.ADMIN -> "Admin"
    OperatorRole.USER -> "User"
  }

  Surface(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("role_card_${role.value}"),
    shape = MaterialTheme.shapes.medium,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "$count active",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
