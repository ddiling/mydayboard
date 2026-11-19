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
 * Maps by name and fails loudly on an unknown value: a type written by a newer app version must not
 * silently turn into a different kind of tracker.
 */
internal fun TrackerEntityType.toDomain(): TrackerType = trackerTypeOf(name)

internal fun TrackerType.toEntity(): TrackerEntityType = TrackerEntityType.valueOf(name)

internal fun trackerTypeOf(name: String): TrackerType =
    TrackerType.entries.firstOrNull { it.name == name } ?: error("Unknown tracker type '$name'")
