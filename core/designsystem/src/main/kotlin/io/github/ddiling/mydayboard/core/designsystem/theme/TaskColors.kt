package io.github.ddiling.mydayboard.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val DARK_BACKGROUND_LUMINANCE = 0.5f

/**
 * The two meanings tasks colour: done (green) and late (red). They live here, not in the tasks feature, because
 * the dashboard shows them too and they must match. Each has a pale variant for dark surfaces and a deep one for
 * light surfaces, both readable (contrast of at least 4.5) on their own background. Colour is never the only
 * sign: screens add a tick or a word next to it.
 */
object TaskColors {
    val DoneOnDark = Color(0xFFA8D5B5)
    val DoneOnLight = Color(0xFF1B6B3A)
    val LateOnDark = Color(0xFFF2B8B5)
    val LateOnLight = Color(0xFFB3261E)

    fun done(darkSurface: Boolean): Color = if (darkSurface) DoneOnDark else DoneOnLight

    fun late(darkSurface: Boolean): Color = if (darkSurface) LateOnDark else LateOnLight

    /** The done colour for the current theme, chosen by how dark its background is. */
    val done: Color
        @Composable get() = done(isDarkSurface())

    /** The late colour for the current theme, chosen by how dark its background is. */
    val late: Color
        @Composable get() = late(isDarkSurface())

    @Composable
    private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
}
