package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class SemanticStatusColors(
  val activeBg: Color,
  val activeFg: Color,
  val closingBg: Color,
  val closingFg: Color,
  val emptyBg: Color,
  val emptyFg: Color,
  val cancelledBg: Color,
  val cancelledFg: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
  SemanticStatusColors(
    activeBg = SoftSage,
    activeFg = OnSoftSage,
    closingBg = WarmGold,
    closingFg = OnWarmGold,
    emptyBg = CoolSkyContainer,
    emptyFg = OnCoolSky,
    cancelledBg = SoftTerracotta,
    cancelledFg = OnSoftTerracotta,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = ForestPine,
  onPrimary = Color.White,
  primaryContainer = SoftSage,
  onPrimaryContainer = OnSoftSage,
  secondary = HarvestAmber,
  onSecondary = Color.White,
  secondaryContainer = WarmGold,
  onSecondaryContainer = OnWarmGold,
  tertiary = SlateTeal,
  onTertiary = Color.White,
  tertiaryContainer = CoolSkyContainer,
  onTertiaryContainer = OnCoolSky,
  error = Terracotta,
  onError = Color.White,
  errorContainer = SoftTerracotta,
  onErrorContainer = OnSoftTerracotta,
  background = WarmLinen,
  onBackground = CharcoalInk,
  surface = CrispSurface,
  onSurface = CharcoalInk,
  surfaceVariant = WarmOat,
  onSurfaceVariant = MutedSlate,
  surfaceContainerLow = WarmLinen,
  surfaceContainer = WarmOat,
  surfaceContainerHigh = SoftClay,
  outline = WarmOutline,
  outlineVariant = SoftClay,
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkSagePrimary,
  onPrimary = DeepPine,
  primaryContainer = DarkPineContainer,
  onPrimaryContainer = DarkSagePrimary,
  secondary = DarkAmber,
  onSecondary = Color(0xFF2B1C00),
  secondaryContainer = DarkAmberContainer,
  onSecondaryContainer = DarkAmber,
  tertiary = DarkSky,
  onTertiary = Color(0xFF00273B),
  tertiaryContainer = DarkSkyContainer,
  onTertiaryContainer = DarkSky,
  error = DarkTerracotta,
  onError = Color(0xFF331100),
  errorContainer = DarkTerracottaContainer,
  onErrorContainer = DarkTerracotta,
  background = DarkPineBg,
  onBackground = DarkInk,
  surface = DarkPineSurface,
  onSurface = DarkInk,
  surfaceVariant = DarkPineVariant,
  onSurfaceVariant = DarkMuted,
  surfaceContainerLow = DarkPineBg,
  surfaceContainer = DarkPineSurface,
  surfaceContainerHigh = DarkPineVariant,
  outline = DarkOutline,
  outlineVariant = DarkPineVariant,
)

private val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BroilerPulseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val statusColors = if (darkTheme) {
    SemanticStatusColors(
      activeBg = DarkPineContainer,
      activeFg = DarkSagePrimary,
      closingBg = DarkAmberContainer,
      closingFg = DarkAmber,
      emptyBg = DarkSkyContainer,
      emptyFg = DarkSky,
      cancelledBg = DarkTerracottaContainer,
      cancelledFg = DarkTerracotta,
    )
  } else {
    SemanticStatusColors(
      activeBg = SoftSage,
      activeFg = OnSoftSage,
      closingBg = WarmGold,
      closingFg = OnWarmGold,
      emptyBg = CoolSkyContainer,
      emptyFg = OnCoolSky,
      cancelledBg = SoftTerracotta,
      cancelledFg = OnSoftTerracotta,
    )
  }

  CompositionLocalProvider(LocalStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
