package io.github.ddiling.mydayboard.core.domain.time

import java.time.Clock
import java.time.LocalDate

/**
 * The calendar date on this clock right now. The zone is read at every call, so with [LiveZoneClock] "today"
 * follows the device after the user travels or changes the zone, instead of staying in the zone of the first call.
 */
fun Clock.today(): LocalDate = LocalDate.now(this)
