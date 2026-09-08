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

private val DarkColorScheme =
  darkColorScheme(
    primary = Indigo400,
    onPrimary = Slate900,
    primaryContainer = Indigo600,
    onPrimaryContainer = Indigo100,
    secondary = Teal400,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Teal100,
    tertiary = Amber400,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Indigo600,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo600,
    secondary = Teal600,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal600,
    tertiary = Amber500,
    background = Slate50,
    onBackground = Slate900,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
