package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The picker speaks in UTC midnight milliseconds, so a date converts both ways without a time zone shifting it. */
private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickedDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * A date field: tap to pick. The whole row is a button of at least 48 dp, because the date text alone is too small
 * to hit. A null [date] shows an empty field, with [error] under it when the date is required; the calendar then
 * opens on [opensOn].
 */
@Composable
internal fun DateField(
    label: String,
    date: LocalDate?,
    opensOn: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var picking by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    DashedField(label, modifier, error) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { picking = true }
                .testTag(tag),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)).orEmpty())
        }
    }
    if (picking) {
        DatePickerPopup(
            initial = date ?: opensOn,
            onConfirm = {
                picking = false
                onDateChange(it)
            },
            onDismiss = { picking = false },
        )
    }
}

/** The Material 3 calendar wrapped once; [initial] is the day it opens on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopup(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.selectedDateMillis?.toPickedDate() ?: initial) },
            ) { Text(stringResource(R.string.tasks_form_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tasks_form_cancel)) } },
    ) { DatePicker(state) }
}

/**
 * A whole-number input. The typed text is kept here, so the user can clear the box and type a new number. Every
 * edit is reported, an empty box as [EMPTY_NUMBER] (0), which the form's number fields reject, so the model never
 * holds a number the box does not show. The box shows the typed text while it still reads as [value], and the
 * value itself once they differ, for example after the model changed from outside.
 */
@Composable
internal fun DashedNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var typed by remember { mutableStateOf(value.toString()) }
    val shown = if ((typed.toIntOrNull() ?: EMPTY_NUMBER) == value) typed else value.toString()
    DashedTextField(
        label = label,
        value = shown,
        onValueChange = { edited ->
            val digits = edited.filter(Char::isDigit).take(MAX_NUMBER_DIGITS)
            typed = digits
            onValueChange(digits.toIntOrNull() ?: EMPTY_NUMBER)
        },
        modifier = modifier,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private const val EMPTY_NUMBER = 0
private const val MAX_NUMBER_DIGITS = 3
