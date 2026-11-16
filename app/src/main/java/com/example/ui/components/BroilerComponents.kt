package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchStatus
import com.example.data.model.FarmStatus
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.MonoMetricLarge
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FarmStatusBadge(
  status: FarmStatus,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusColors.current
  val (bg, fg) = when (status) {
    FarmStatus.ACTIVE -> palette.activeBg to palette.activeFg
    FarmStatus.CLOSING -> palette.closingBg to palette.closingFg
    FarmStatus.EMPTY -> palette.emptyBg to palette.emptyFg
    FarmStatus.CANCELLED -> palette.cancelledBg to palette.cancelledFg
  }
  StatusPill(text = status.label, background = bg, foreground = fg, modifier = modifier)
}

@Composable
fun BatchStatusBadge(
  status: BatchStatus,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusColors.current
  val (bg, fg) = when (status) {
    BatchStatus.ACTIVE -> palette.activeBg to palette.activeFg
    BatchStatus.CLOSING -> palette.closingBg to palette.closingFg
    BatchStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
  }
  StatusPill(text = status.label, background = bg, foreground = fg, modifier = modifier)
}

@Composable
fun StatusPill(
  text: String,
  background: Color,
  foreground: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = background,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = foreground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun CloudSyncPill(modifier: Modifier = Modifier) {
  val palette = LocalStatusColors.current
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = palette.activeBg,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.CloudDone,
        contentDescription = "Cloud sync active",
        tint = palette.activeFg,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = "Live Sync • Mobile & Desktop",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = palette.activeFg,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun KpiMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = accentColor.copy(alpha = 0.12f),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
              .padding(6.dp)
              .size(18.dp),
          )
        }
      }
      Text(
        text = value,
        style = MonoMetricLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier
            .padding(14.dp)
            .size(28.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onAction) {
          Text(actionLabel)
        }
      }
    }
  }
}

fun formatNumber(value: Long): String =
  NumberFormat.getIntegerInstance(Locale.US).format(value)

fun formatDecimal(value: Double, decimals: Int = 2): String =
  String.format(Locale.US, "%.${decimals}f", value)

fun formatCurrency(value: Double): String {
  val nf = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
  nf.maximumFractionDigits = 0
  return "₹${nf.format(value)}"
}

fun formatEpochDay(epochDay: Long): String {
  if (epochDay <= 0L) return "—"
  return try {
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
  } catch (_: Exception) {
    "Day $epochDay"
  }
}
