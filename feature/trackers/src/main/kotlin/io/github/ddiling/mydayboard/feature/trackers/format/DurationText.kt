package io.github.ddiling.mydayboard.feature.trackers.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.trackers.R
import java.time.Duration

/** The localized text for this duration: "just now", "5 min", "3 h 20 min" or "2 days". */
@Composable
internal fun Duration.asText(): String =
    when (val parts = toParts()) {
        DurationParts.JustNow -> {
            stringResource(R.string.trackers_duration_just_now)
        }

        is DurationParts.Minutes -> {
            stringResource(R.string.trackers_duration_minutes, parts.minutes)
        }

        is DurationParts.Hours -> {
            if (parts.minutes == 0) {
                stringResource(R.string.trackers_duration_hours, parts.hours)
            } else {
                stringResource(R.string.trackers_duration_hours_minutes, parts.hours, parts.minutes)
            }
        }

        is DurationParts.Days -> {
            pluralStringResource(R.plurals.trackers_days, parts.days, parts.days)
        }
    }
