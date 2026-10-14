package com.example.nplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.nplayer.data.prefs.ThemeMode

/**
 * The app's own palette: deep near-black surfaces with a cool blue accent, so the
 * media thumbnails are the only bright thing on screen.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF002A55),
    primaryContainer = Color(0xFF123B66),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253141),
    secondaryContainer = Color(0xFF1B2531),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFF7FD4C8),
    background = Color(0xFF08080C),
    onBackground = Color(0xFFE4E2E9),
    surface = Color(0xFF0F0F15),
    onSurface = Color(0xFFE4E2E9),
    surfaceVariant = Color(0xFF1B1B23),
    onSurfaceVariant = Color(0xFFBFC6D4),
    outline = Color(0xFF3A3F4B),
    outlineVariant = Color(0xFF262A33),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C5C93),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001B37),
    secondary = Color(0xFF4C5C72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF0A1B2C),
    tertiary = Color(0xFF166B60),
    background = Color(0xFFFBFBFE),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFF4F4F9),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF454B57),
    outline = Color(0xFF757C89),
    outlineVariant = Color(0xFFC5C9D4),
)

/**
 * Slightly tighter, heavier headings than the Material defaults. The large styles
 * are used for the app's own headings, not long-form copy, so the tighter tracking
 * reads as deliberate.
 */
private val NPlayerTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/**
 * @param dynamicColor when true and the device supports it, the wallpaper palette
 *   replaces [DarkColors]/[LightColors]. Off by default so the app keeps its own
 *   identity; Settings can turn it back on.
 */
@Composable
fun NPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NPlayerTypography,
        content = content,
    )
}

/** Resolves [mode] against the device setting to a concrete light/dark choice. */
@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
