package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.dao.TrackerDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerEventDao
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * [TrackerRepository] on Room. Deletes are soft (`deletedAt`), and unknown or deleted ids are no-ops.
 *
 * The event read queries do not check the parent tracker, so `observeEvents` does. Writes are single
 * conditional statements in the DAOs, so a concurrent delete cannot be undone by a stale read-modify-write.
 */
internal class OfflineTrackerRepository
    @Inject
    constructor(
        private val trackerDao: TrackerDao,
        private val eventDao: TrackerEventDao,
        private val clock: Clock,
        private val ids: IdGenerator,
    ) : TrackerRepository {
        override fun observeTrackers(): Flow<List<Tracker>> =
            trackerDao.observeActive().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

        override fun observeTracker(id: String): Flow<Tracker?> =
            trackerDao.observeActiveById(id).map { it?.toDomain() }.distinctUntilChanged()

        override fun observeEvents(trackerId: String): Flow<List<TrackerEvent>> =
            combine(
                trackerDao.observeActiveById(trackerId),
                eventDao.observeActiveForTracker(trackerId),
            ) { tracker, rows ->
                if (tracker == null) emptyList() else rows.map { it.toDomain() }
            }.distinctUntilChanged()

        override fun observeAllEvents(): Flow<List<TrackerEvent>> =
            eventDao.observeActiveForActiveTrackers().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

        override suspend fun createTracker(
            name: String,
            type: TrackerType,
            startedAt: Instant,
        ): Tracker {
            val now = clock.instant()
            val tracker = Tracker(ids.newId(), name, type, startedAt, createdAt = now, updatedAt = now)
            trackerDao.insert(tracker.toEntity())
            return tracker
        }

        override suspend fun renameTracker(
            id: String,
            name: String,
        ) {
            trackerDao.rename(id, name, now = clock.millis())
        }

        override suspend fun deleteTracker(id: String) {
            trackerDao.softDelete(id, now = clock.millis())
        }

        override suspend fun addEvent(
            trackerId: String,
            occurredAt: Instant,
        ): TrackerEvent? {
            val now = clock.instant()
            val event = TrackerEvent(ids.newId(), trackerId, occurredAt, createdAt = now, updatedAt = now)
            val inserted =
                eventDao.insertForActiveTracker(
                    event.id,
                    trackerId,
                    occurredAt.toEpochMilli(),
                    now.toEpochMilli(),
                )
            return event.takeIf { inserted != NOT_INSERTED }
        }

        override suspend fun updateEventTime(
            eventId: String,
            occurredAt: Instant,
        ) {
            eventDao.updateOccurredAtIfActive(eventId, occurredAt.toEpochMilli(), now = clock.millis())
        }

        override suspend fun deleteEvent(eventId: String) {
            eventDao.softDeleteIfActive(eventId, now = clock.millis())
        }

        override suspend fun restoreEvent(eventId: String) {
            eventDao.restoreIfDeleted(eventId, now = clock.millis())
        }

        private companion object {
            const val NOT_INSERTED = -1L
        }
    }
