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
    primary = Color(0xFF93C5FD), // Soft Sky Blue
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0369A1),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF78350F),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlueContainer,
    onPrimaryContainer = RoyalBlueDark,
    secondary = RoyalBlueLight,
    onSecondary = Color.White,
    tertiary = AmberGold,
    onTertiary = Color.White,
    background = LightGrayBackground,
    onBackground = DarkText,
    surface = Color.White,
    onSurface = DarkText,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = DarkText,
    outline = BorderSlate,
    error = ErrorRed,
    onError = Color.White
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
