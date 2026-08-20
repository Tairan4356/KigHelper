package com.ziegler.kighelper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.materialkolor.rememberDynamicColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color(0xFF381E72),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
private fun seedColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val base = rememberDynamicColorScheme(seedColor = seedColor, isDark = isDark)
    val onPrimary = if (seedColor.luminance() > 0.5f) Color.Black else Color.White
    return base.copy(primary = seedColor, onPrimary = onPrimary)
}

@Composable
fun KigHelperTheme(
    darkMode: Int = 0,
    colorMode: Int = 0,
    presetColorIndex: Int = 0,
    customColor: Long = 0xFF6650A4,
    fontType: Int = 0,
    fontWeight: Int = 400,
    selectedCustomFont: String? = null,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    val colorScheme = when (colorMode) {
        0 -> {
            // 跟随系统/动态配色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }

        1 -> {
            // 预设颜色 - 所选颜色直接作为 primary，其余由 MaterialKolor 生成
            val seedColor = PresetColors[presetColorIndex.coerceIn(0, PresetColors.lastIndex)]
            seedColorScheme(seedColor = seedColor, isDark = darkTheme)
        }

        2 -> {
            // 自定义颜色 - 所选颜色直接作为 primary，其余由 MaterialKolor 生成
            val seedColor = Color(customColor.toInt())
            seedColorScheme(seedColor = seedColor, isDark = darkTheme)
        }

        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val context = LocalContext.current
    val builtinCount = FontType.entries.size
    val fontFamily = remember(fontType, selectedCustomFont) {
        if (fontType < builtinCount) {
            FontType.entries[fontType].fontFamily
        } else {
            selectedCustomFont?.let { loadCustomFontFamily(context, it) }
                ?: FontType.entries[0].fontFamily
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = createTypography(fontFamily, if (fontType == 0) FontWeight.Bold else FontWeight(fontWeight)),
        content = content
    )
}
