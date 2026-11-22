package io.github.ddiling.mydayboard.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskColorsTest {
    private val readableContrast = 4.5f

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val (light, dark) = maxOf(a.luminance(), b.luminance()) to minOf(a.luminance(), b.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test
    fun `done and late are readable on a dark surface`() {
        assertTrue(contrast(TaskColors.done(darkSurface = true), Color.Black) >= readableContrast)
        assertTrue(contrast(TaskColors.late(darkSurface = true), Color.Black) >= readableContrast)
    }

    @Test
    fun `done and late are readable on a light surface`() {
        assertTrue(contrast(TaskColors.done(darkSurface = false), Color.White) >= readableContrast)
        assertTrue(contrast(TaskColors.late(darkSurface = false), Color.White) >= readableContrast)
    }
}
