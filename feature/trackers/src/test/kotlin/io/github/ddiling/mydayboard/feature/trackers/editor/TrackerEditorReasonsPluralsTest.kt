package io.github.ddiling.mydayboard.feature.trackers.editor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.feature.trackers.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Polish has four plural forms, so one, few and many must each pick their own word. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pl")
class TrackerEditorReasonsPluralsTest {
    private fun count(quantity: Int): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.resources.getQuantityString(R.plurals.trackers_editor_reasons_count, quantity, quantity)
    }

    @Test
    fun `one reason uses the singular form`() {
        assertEquals("1 powód", count(1))
    }

    @Test
    fun `two reasons use the few form`() {
        assertEquals("2 powody", count(2))
    }

    @Test
    fun `five reasons use the many form`() {
        assertEquals("5 powodów", count(5))
    }
}
