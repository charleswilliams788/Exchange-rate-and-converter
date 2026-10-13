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

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBluePrimaryDark,
    onPrimary = ElectricBlueOnPrimaryDark,
    primaryContainer = ElectricBluePrimaryContainerDark,
    onPrimaryContainer = ElectricBlueOnPrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    background = MidnightNavyBackgroundDark,
    onBackground = MidnightNavyOnBackgroundDark,
    surface = MidnightNavySurfaceDark,
    onSurface = MidnightNavyOnSurfaceDark,
    surfaceVariant = MidnightNavySurfaceVariantDark,
    onSurfaceVariant = MidnightNavyOnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBluePrimaryLight,
    onPrimary = ElectricBlueOnPrimaryLight,
    primaryContainer = ElectricBluePrimaryContainerLight,
    onPrimaryContainer = ElectricBlueOnPrimaryContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    background = CoolWhiteBackgroundLight,
    onBackground = CoolWhiteOnBackgroundLight,
    surface = WhiteSurfaceLight,
    onSurface = WhiteOnSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand Electric Blue & Midnight Navy colors by default
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
