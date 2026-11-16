package io.github.ddiling.mydayboard.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SummarizeTapsUseCaseTest {
    private val summarizeTaps = SummarizeTapsUseCase()

    @Test
    fun `zero taps is None`() {
        assertEquals(TapSummary.None, summarizeTaps(0))
    }

    @Test
    fun `positive taps keep the count`() {
        assertEquals(TapSummary.Some(3), summarizeTaps(3))
    }

    @Test
    fun `negative taps are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { summarizeTaps(-1) }
    }
}
