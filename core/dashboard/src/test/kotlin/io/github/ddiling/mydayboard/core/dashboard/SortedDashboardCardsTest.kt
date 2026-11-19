package io.github.ddiling.mydayboard.core.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SortedDashboardCardsTest {
    @Test
    fun `cards are sorted by ascending order`() {
        val cards = setOf(card("late", order = 20), card("early", order = 10))

        assertEquals(listOf("early", "late"), sortedDashboardCards(cards).map { it.key })
    }

    @Test
    fun `cards with the same order are sorted by key`() {
        val cards = setOf(card("b", order = 1), card("a", order = 1))

        assertEquals(listOf("a", "b"), sortedDashboardCards(cards).map { it.key })
    }

    @Test
    fun `an empty set gives an empty list`() {
        assertTrue(sortedDashboardCards(emptySet()).isEmpty())
    }

    @Test
    fun `duplicate keys are rejected naming the key`() {
        val cards = setOf(card("trackers", order = 1), card("trackers", order = 2))

        try {
            sortedDashboardCards(cards)
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("trackers"))
        }
    }

    private fun card(
        key: String,
        order: Int,
    ) = object : DashboardCard {
        override val key = key
        override val order = order

        @Composable
        override fun Content(
            host: DashboardHost,
            modifier: Modifier,
        ) = Unit
    }
}
