package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEntityType
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerReasonEntity
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TrackerMappersTest {
    private val startedAt = Instant.ofEpochMilli(1_000)
    private val createdAt = Instant.ofEpochMilli(2_000)
    private val updatedAt = Instant.ofEpochMilli(3_000)

    @Test
    fun `tracker survives a round trip through the entity for every type`() {
        for (type in TrackerType.entries) {
            val tracker = Tracker("t1", "Name", type, startedAt, createdAt, updatedAt)

            assertEquals(tracker, tracker.toEntity().toDomain())
        }
    }

    @Test
    fun `tracker entity stores every field as given in epoch millis`() {
        val entity = Tracker("t1", "Name", TrackerType.DAYS_SINCE, startedAt, createdAt, updatedAt).toEntity()

        assertEquals(TrackerEntity("t1", "Name", TrackerEntityType.DAYS_SINCE, 1_000, 2_000, 3_000, null), entity)
    }

    @Test
    fun `event survives a round trip through the entity`() {
        val event = TrackerEvent("e1", "t1", startedAt, createdAt, updatedAt)

        assertEquals(event, event.toEntity().toDomain())
    }

    @Test
    fun `event entity stores every field as given in epoch millis`() {
        val entity = TrackerEvent("e1", "t1", startedAt, createdAt, updatedAt).toEntity()

        assertEquals(TrackerEventEntity("e1", "t1", 1_000, 2_000, 3_000, null), entity)
    }

    @Test
    fun `tracker entity keeps askForReason in both directions`() {
        val tracker =
            Tracker("t1", "Name", TrackerType.EVENT_COUNTER, startedAt, createdAt, updatedAt, askForReason = true)

        assertEquals(tracker, tracker.toEntity().toDomain())
        assertEquals(true, tracker.toEntity().askForReason)
    }

    @Test
    fun `reason entity converts epoch millis to instants`() {
        val entity = TrackerReasonEntity("r1", "t1", "Water", createdAt = 2_000, updatedAt = 3_000, deletedAt = null)

        assertEquals(TrackerReason("r1", "t1", "Water", createdAt, updatedAt), entity.toDomain())
    }
}
