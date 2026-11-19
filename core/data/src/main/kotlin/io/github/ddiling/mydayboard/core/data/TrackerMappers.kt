package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEntityType
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import java.time.Instant

/** The domain never sees `deletedAt`: deleted rows are filtered out before they are mapped. */
internal fun TrackerEntity.toDomain() =
    Tracker(
        id = id,
        name = name,
        type = type.toDomain(),
        startedAt = Instant.ofEpochMilli(startedAt),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

/** A new row is never deleted, so `deletedAt` stays null. */
internal fun Tracker.toEntity() =
    TrackerEntity(
        id = id,
        name = name,
        type = type.toEntity(),
        startedAt = startedAt.toEpochMilli(),
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

internal fun TrackerEventEntity.toDomain() =
    TrackerEvent(
        id = id,
        trackerId = trackerId,
        occurredAt = Instant.ofEpochMilli(occurredAt),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

internal fun TrackerEvent.toEntity() =
    TrackerEventEntity(
        id = id,
        trackerId = trackerId,
        occurredAt = occurredAt.toEpochMilli(),
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

/**
 * Exhaustive `when`s, so adding a type breaks the build here. An unknown stored string never reaches this
 * code: Room's enum converter rejects it when reading the row.
 */
internal fun TrackerEntityType.toDomain(): TrackerType =
    when (this) {
        TrackerEntityType.EVENT_COUNTER -> TrackerType.EVENT_COUNTER
        TrackerEntityType.DAYS_SINCE -> TrackerType.DAYS_SINCE
    }

internal fun TrackerType.toEntity(): TrackerEntityType =
    when (this) {
        TrackerType.EVENT_COUNTER -> TrackerEntityType.EVENT_COUNTER
        TrackerType.DAYS_SINCE -> TrackerEntityType.DAYS_SINCE
    }
