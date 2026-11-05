package com.example.memora.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C3B20),
    secondary = Color(0xFFF8F3E8),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFCFAF4),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF2C3B20),
    onSurface = Color(0xFF2C3B20)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF216853),
    secondary = Color(0xFF233631),
    background = Color(0xFF153431),
    surface = Color(0xFF0E2220),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE5F3EC),
    onSurface = Color(0xFFE5F3EC)
)

@Composable
fun MemoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep false so the app keeps its own green/teal palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
