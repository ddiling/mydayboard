package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.trackers.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 86_400_000L

/** Future starts would make the new tracker look empty from day one, so only today and the past can be picked. */
internal fun isSelectableStartDate(
    utcTimeMillis: Long,
    today: LocalDate,
): Boolean = utcTimeMillis / MILLIS_PER_DAY <= today.toEpochDay()

/** The picker works in UTC midnights, so dates are converted by epoch day and no zone is involved. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartDateField(
    date: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0] // not cached globally: the app language can change
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.trackers_editor_start_date) + ": " + date.format(dateFormat))
    }
    if (showPicker) {
        val pickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = date.toEpochDay() * MILLIS_PER_DAY,
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) = isSelectableStartDate(utcTimeMillis, today)
                    },
            )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateChange(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.trackers_editor_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.trackers_editor_cancel)) }
            },
        ) { DatePicker(pickerState) }
    }
}
