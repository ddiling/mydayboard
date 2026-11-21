package io.github.ddiling.mydayboard.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Black = Color(0xFF000000)

private val AppColors =
    darkColorScheme(
        primary = Color(0xFF8C9EFF),
        secondary = Color(0xFF64FFDA),
        background = Black,
        surface = Black,
        onBackground = Color(0xFFECECF1),
        onSurface = Color(0xFFECECF1),
        onSurfaceVariant = Color(0xFF9A9AA6),
    )

/**
 * The app's Material 3 theme: dark only, on pure black. The design exists in one variant, so there is one
 * look to build and test, and black pixels cost no power on OLED screens. It ignores the system light/dark
 * setting and Material You colours on purpose, because both would replace the app's own palette.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}
