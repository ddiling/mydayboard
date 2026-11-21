package io.github.ddiling.mydayboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** What the dashboard header shows: "07:05", "03.01" and a short weekday such as "Sat". */
data class HeaderTime(
    val time: String,
    val date: String,
    val weekday: String,
)

/**
 * Turns an injected [Clock] into header text. The zone is read from the clock on every call, not once, so a
 * clock that follows the device zone shows the new local time after the user travels. Whole-minute zone
 * offsets are assumed, so a UTC minute boundary is a local one too.
 */
class HeaderClockState(
    private val clock: Clock,
) {
    fun now(locale: Locale = Locale.getDefault()): HeaderTime {
        val local = clock.instant().atZone(clock.zone)
        return HeaderTime(
            time = local.format(TIME_FORMAT),
            date = local.format(DATE_FORMAT),
            weekday = local.dayOfWeek.shortName(locale),
        )
    }

    /** Wait until the next full minute, so the header changes together with the status bar clock. */
    fun millisToNextMinute(): Long = MILLIS_PER_MINUTE - Math.floorMod(clock.millis(), MILLIS_PER_MINUTE)

    private fun DayOfWeek.shortName(locale: Locale) = getDisplayName(TextStyle.SHORT, locale)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
    }
}

/** The header text, refreshed at every minute boundary while the screen is shown. */
@Composable
fun rememberHeaderTime(clock: Clock): HeaderTime {
    val state = remember(clock) { HeaderClockState(clock) }
    val time by produceState(state.now(), state) {
        while (true) {
            delay(state.millisToNextMinute())
            value = state.now()
        }
    }
    return time
}
