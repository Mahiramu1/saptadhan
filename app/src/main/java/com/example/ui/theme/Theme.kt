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

private val DarkColorScheme = darkColorScheme(
    primary = BankPrimaryContainer,
    onPrimary = BankOnPrimaryContainer,
    primaryContainer = BankPrimary,
    onPrimaryContainer = BankPrimaryContainer,
    secondary = BankSecondaryContainer,
    onSecondary = BankOnSecondaryContainer,
    tertiary = BankTertiaryContainer,
    onTertiary = BankOnTertiaryContainer,
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C3)
)

private val LightColorScheme = lightColorScheme(
    primary = BankPrimary,
    onPrimary = BankOnPrimary,
    primaryContainer = BankPrimaryContainer,
    onPrimaryContainer = BankOnPrimaryContainer,
    secondary = BankSecondary,
    onSecondary = BankOnSecondary,
    secondaryContainer = BankSecondaryContainer,
    onSecondaryContainer = BankOnSecondaryContainer,
    tertiary = BankTertiary,
    onTertiary = BankOnTertiary,
    tertiaryContainer = BankTertiaryContainer,
    onTertiaryContainer = BankOnTertiaryContainer,
    background = BankBackground,
    onBackground = BankOnBackground,
    surface = BankSurface,
    onSurface = BankOnSurface,
    surfaceVariant = BankSurfaceVariant,
    onSurfaceVariant = BankOnSurfaceVariant,
    outline = BankOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our polished Wanaparthy banking palette for strong identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
