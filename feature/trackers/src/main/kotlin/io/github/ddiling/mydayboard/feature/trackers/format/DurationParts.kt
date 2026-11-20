package io.github.ddiling.mydayboard.feature.trackers.format

import java.time.Duration

/**
 * A duration reduced to the one or two units worth showing, kept free of Android so the rounding rules can be
 * tested without resources. Longer spans drop the smaller unit ("2 days", not "2 days 3 h"), because on a list
 * row the extra precision is noise.
 */
internal sealed interface DurationParts {
    /** Under a minute: counting seconds would only make the row flicker. */
    data object JustNow : DurationParts

    data class Minutes(
        val minutes: Int,
    ) : DurationParts

    data class Hours(
        val hours: Int,
        val minutes: Int,
    ) : DurationParts

    data class Days(
        val days: Int,
    ) : DurationParts
}

/** Splits the duration into [DurationParts]; a negative duration (clock moved back) is [DurationParts.JustNow]. */
internal fun Duration.toParts(): DurationParts {
    val totalMinutes = toMinutes()
    return when {
        totalMinutes < 1 -> {
            DurationParts.JustNow
        }

        totalMinutes < MINUTES_PER_HOUR -> {
            DurationParts.Minutes(totalMinutes.toInt())
        }

        totalMinutes < MINUTES_PER_DAY -> {
            DurationParts.Hours((totalMinutes / MINUTES_PER_HOUR).toInt(), (totalMinutes % MINUTES_PER_HOUR).toInt())
        }

        else -> {
            DurationParts.Days(toDays().toIntSaturated())
        }
    }
}

/** Plural resources take an `Int`; a day count beyond it is not a real tracker, so it saturates instead of wrapping. */
internal fun Long.toIntSaturated(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
