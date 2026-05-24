package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AnimeDarkColorScheme = darkColorScheme(
  primary = AnimePrimary,
  secondary = AnimeSecondary,
  tertiary = AnimeAccent,
  background = AnimeDarkBg,
  surface = AnimeCardBg,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color.White,
  onSurface = Color.White,
)

private val AnimeLightColorScheme = lightColorScheme(
  primary = AnimePrimary,
  secondary = AnimeSecondary,
  tertiary = AnimeAccent,
  background = Color(0xFFF9F6FF),
  surface = Color.White,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onBackground = Color(0xFF13111C),
  onSurface = Color(0xFF13111C),
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default for premium anime vibe
  dynamicColor: Boolean = false, // Set false to preserve our gorgeous hand-crafted gradients
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AnimeDarkColorScheme else AnimeLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
