package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    onPrimary = CosmicDark,
    primaryContainer = CosmicSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = StarWhite,
    secondaryContainer = CosmicSurfaceVariant,
    onSecondaryContainer = NeonPurple,
    tertiary = NeonPink,
    onTertiary = StarWhite,
    background = CosmicDark,
    onBackground = StarWhite,
    surface = CosmicSurface,
    onSurface = StarWhite,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = StarSilver,
    error = AlertRed,
    onError = StarWhite,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CosmicColorScheme,
    typography = Typography,
    content = content
  )
}

