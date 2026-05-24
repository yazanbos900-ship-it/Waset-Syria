package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
      primary = DarkGreenPrimary,
      secondary = DarkGreenSecondary,
      background = AccentBlack,
      surface = AccentBlack,
      onPrimary = AccentBlack,
      onSecondary = Color.White,
      onBackground = Color.White,
      onSurface = Color.White,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    background = OffWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = AccentBlack,
    onBackground = AccentBlack,
    onSurface = AccentBlack,
  )

@Composable
fun MyApplicationTheme(
  // Force dark theme for the premium marketplace look requested
  darkTheme: Boolean = true,
  // Forcing custom branding for this specific marketplace design
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
