package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.dao.TrackerDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerReasonDao
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * [TrackerReasonRepository] on Room. Deletes are soft (`deletedAt`), and unknown or deleted ids are no-ops.
 * Every write is one conditional statement in the DAO, so a concurrent tracker delete cannot be undone by a
 * stale read-modify-write; the reason query already hides the reasons of a deleted tracker.
 */
internal class OfflineTrackerReasonRepository
    @Inject
    constructor(
        private val trackerDao: TrackerDao,
        private val reasonDao: TrackerReasonDao,
        private val clock: Clock,
        private val ids: IdGenerator,
    ) : TrackerReasonRepository {
        override fun observeReasons(trackerId: String): Flow<List<TrackerReason>> =
            reasonDao
                .observeActiveForTracker(trackerId)
                .map { rows ->
                    rows.map { it.toDomain() }
                }.distinctUntilChanged()

        override suspend fun setAskForReason(
            id: String,
            enabled: Boolean,
        ) {
            trackerDao.setAskForReason(id, enabled, now = clock.millis())
        }

        override suspend fun addReason(
            trackerId: String,
            text: String,
        ): TrackerReason? {
            val now = clock.instant()
            val reason = TrackerReason(ids.newId(), trackerId, text, createdAt = now, updatedAt = now)
            val inserted = reasonDao.insertForActiveTracker(reason.id, trackerId, text, now.toEpochMilli())
            return reason.takeIf { inserted != NOT_INSERTED }
        }

        override suspend fun updateReason(
            reasonId: String,
            text: String,
        ) {
            reasonDao.updateTextIfActive(reasonId, text, now = clock.millis())
        }

        override suspend fun deleteReason(reasonId: String) {
            reasonDao.softDeleteIfActive(reasonId, now = clock.millis())
        }

        override suspend fun restoreReason(reasonId: String) {
            reasonDao.restoreIfDeleted(reasonId, now = clock.millis())
        }

        private companion object {
            const val NOT_INSERTED = -1L
        }
    }
