package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class TrackerEarliestEventTimeTest {
    private val startedAt = Instant.parse("2025-03-01T10:00:00Z")

    private fun tracker(type: TrackerType) = Tracker("t", "Name", type, startedAt, startedAt, startedAt)

    @Test
    fun `days since tracker allows events from its start`() {
        assertEquals(startedAt, tracker(TrackerType.DAYS_SINCE).earliestAllowedEventTime)
    }

    @Test
    fun `event counter allows events before it was started`() {
        assertNull(tracker(TrackerType.EVENT_COUNTER).earliestAllowedEventTime)
    }
}
