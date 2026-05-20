package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.Track

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekDarkPrimary,
    onPrimary = SleekDarkOnPrimary,
    primaryContainer = SleekDarkPrimaryContainer,
    onPrimaryContainer = SleekDarkOnPrimaryContainer,
    secondary = SleekDarkSecondary,
    secondaryContainer = SleekDarkSecondaryContainer,
    onSecondaryContainer = SleekDarkOnSecondaryContainer,
    background = SleekDarkBackground,
    onBackground = SleekDarkOnBackground,
    surface = SleekDarkSurface,
    onSurface = SleekDarkOnSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurfaceVariant = SleekDarkOnSurfaceVariant,
    outline = SleekDarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekLightPrimary,
    onPrimary = SleekLightOnPrimary,
    primaryContainer = SleekLightPrimaryContainer,
    onPrimaryContainer = SleekLightOnPrimaryContainer,
    secondary = SleekLightSecondary,
    secondaryContainer = SleekLightSecondaryContainer,
    onSecondaryContainer = SleekLightOnSecondaryContainer,
    background = SleekLightBackground,
    onBackground = SleekLightOnBackground,
    surface = SleekLightSurface,
    onSurface = SleekLightOnSurface,
    surfaceVariant = SleekLightSurfaceVariant,
    onSurfaceVariant = SleekLightOnSurfaceVariant,
    outline = SleekLightOutline
  )

private fun getDynamicSeedColor(title: String, artist: String): Color {
    val h = kotlin.math.abs((title + artist).hashCode() % 360).toFloat()
    return hsvToColor(h, 0.65f, 0.55f)
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val x = c * (1 - kotlin.math.abs((hue / 60) % 2 - 1))
    val m = value - c
    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

private fun makeColorSchemeForSeed(seedColor: Color, isDark: Boolean): ColorScheme {
    val hash = (seedColor.red * 255 + seedColor.green * 127 + seedColor.blue * 63).toInt()
    val h = kotlin.math.abs(hash % 360).toFloat()
    
    return if (isDark) {
        darkColorScheme(
            primary = seedColor,
            onPrimary = Color(0xFF001D36),
            primaryContainer = hsvToColor(h, 0.70f, 0.25f),
            onPrimaryContainer = hsvToColor(h, 0.30f, 0.95f),
            secondary = hsvToColor((h + 30) % 360, 0.40f, 0.75f),
            secondaryContainer = hsvToColor((h + 30) % 360, 0.35f, 0.28f),
            background = Color(0xFF141618),
            onBackground = Color(0xFFE3E2E6),
            surface = Color(0xFF1B1D1F),
            onSurface = Color(0xFFE3E2E6),
            surfaceVariant = Color(0xFF2C3034),
            onSurfaceVariant = Color(0xFFC3C6CF),
            outline = Color(0xFF8A929A)
        )
    } else {
        lightColorScheme(
            primary = seedColor,
            onPrimary = Color.White,
            primaryContainer = hsvToColor(h, 0.15f, 0.96f),
            onPrimaryContainer = hsvToColor(h, 0.85f, 0.15f),
            secondary = hsvToColor((h + 30) % 360, 0.50f, 0.55f),
            secondaryContainer = hsvToColor((h + 30) % 360, 0.20f, 0.92f),
            background = Color(0xFFFCFDFE),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFF2F4F7),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFE5E8EC),
            onSurfaceVariant = Color(0xFF43474E),
            outline = Color(0xFF73777F)
        )
    }
}

@Composable
fun MyApplicationTheme(
  selectedTrack: Track? = null,
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      selectedTrack != null -> {
          val seed = getDynamicSeedColor(selectedTrack.title, selectedTrack.artist)
          makeColorSchemeForSeed(seed, darkTheme)
      }
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
