package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository

class FakeTrackerRepositoryTest : TrackerRepositoryContractTest() {
    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = FakeTrackerRepository(clock, ids)
}
