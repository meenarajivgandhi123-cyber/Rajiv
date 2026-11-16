package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          )
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      val isWide = maxWidth >= 760.dp

      if (isWide) {
        Row(
          modifier = Modifier
            .widthIn(max = 1040.dp)
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(32.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AuthIllustrationCard(
            modifier = Modifier.weight(1.1f),
          )
          AuthActionColumn(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onSignInClick = {
              isLoading = true
              errorMessage = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false },
              )
            },
            modifier = Modifier.weight(0.9f),
          )
        }
      } else {
        Column(
          modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AuthIllustrationCard(modifier = Modifier.fillMaxWidth())
          AuthActionColumn(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onSignInClick = {
              isLoading = true
              errorMessage = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false },
              )
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun AuthIllustrationCard(modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_auth_illustration),
        contentDescription = "Broiler farm synced across mobile and computer",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .clip(MaterialTheme.shapes.large),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FeatureBadge(
          icon = Icons.Rounded.Devices,
          label = "Mobile & Computer",
          modifier = Modifier.weight(1f),
        )
        FeatureBadge(
          icon = Icons.Rounded.CloudSync,
          label = "Real-Time Cloud",
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun FeatureBadge(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun AuthActionColumn(
  isLoading: Boolean,
  errorMessage: String?,
  onSignInClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_spf_logo),
          contentDescription = "SPF Logo",
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .size(64.dp)
            .padding(6.dp),
        )
      }
      Column {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = "SPF POULTRY INTEGRATION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "SPF",
          style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    Text(
      text = "Manage zones, contract farmers, active & closing batches, GC Closed PDFs, zone-wise daily reports, feed dispatches, inter-feed transfers, and bird sales across mobile & desktop.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (!errorMessage.isNullOrBlank()) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
      onClick = onSignInClick,
      enabled = !isLoading,
      shape = MaterialTheme.shapes.medium,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("google_sign_in_button"),
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.5.dp,
        )
      } else {
        Text(
          text = "Sign in with Google",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
    }
  }
}
