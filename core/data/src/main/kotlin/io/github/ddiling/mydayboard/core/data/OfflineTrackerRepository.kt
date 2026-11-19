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
 * The event DAO queries do not check the parent tracker, so this class does: events of a deleted tracker are
 * hidden and event writes under it are ignored. Doing it here keeps the DAO queries simple.
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
            val row = trackerDao.getActiveById(id) ?: return
            trackerDao.update(row.copy(name = name, updatedAt = clock.millis()))
        }

        override suspend fun deleteTracker(id: String) {
            val row = trackerDao.getActiveById(id) ?: return
            val now = clock.millis()
            trackerDao.update(row.copy(updatedAt = now, deletedAt = now))
        }

        override suspend fun addEvent(
            trackerId: String,
            occurredAt: Instant,
        ): TrackerEvent? {
            if (trackerDao.getActiveById(trackerId) == null) return null
            val now = clock.instant()
            val event = TrackerEvent(ids.newId(), trackerId, occurredAt, createdAt = now, updatedAt = now)
            eventDao.insert(event.toEntity())
            return event
        }

        override suspend fun updateEventTime(
            eventId: String,
            occurredAt: Instant,
        ) {
            val row = activeEvent(eventId) ?: return
            eventDao.update(row.copy(occurredAt = occurredAt.toEpochMilli(), updatedAt = clock.millis()))
        }

        override suspend fun deleteEvent(eventId: String) {
            val row = activeEvent(eventId) ?: return
            val now = clock.millis()
            eventDao.update(row.copy(updatedAt = now, deletedAt = now))
        }

        /** The event, only while neither it nor its tracker is deleted. */
        private suspend fun activeEvent(eventId: String) =
            eventDao.getActiveById(eventId)?.takeIf { trackerDao.getActiveById(it.trackerId) != null }
    }
