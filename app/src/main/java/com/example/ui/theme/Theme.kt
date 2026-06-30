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

private val DarkColorScheme =
  darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF003731),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFF72F8E7),
    secondary = PurpleSecondaryDark,
    onSecondary = Color(0xFF381E72),
    tertiary = NaturalSand,
    background = TealBackgroundDark,
    onBackground = NaturalSand,
    surface = Color(0xFF171E1C),
    onSurface = NaturalSand,
    surfaceVariant = Color(0xFF232D2B),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF455552)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = NaturalWhite,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF00201C),
    secondary = PurpleSecondaryLight,
    onSecondary = NaturalWhite,
    tertiary = NaturalSand,
    background = TealBackgroundLight,
    onBackground = NaturalTextMain,
    surface = NaturalWhite,
    onSurface = NaturalTextMain,
    surfaceVariant = NaturalCream,
    onSurfaceVariant = NaturalTextMuted,
    outline = NaturalSand
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to prioritize the "Natural Tones" theme
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
