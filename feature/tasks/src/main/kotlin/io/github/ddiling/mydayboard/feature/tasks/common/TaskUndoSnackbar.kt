package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.format.DateTimeFormatter

private val slotFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * What the snackbar says after a swipe: "Breakfast done", "Send CVs 2/3" (a count goal, not finished),
 * "Send CVs 3/3 · done", or "Litter boxes 20:00 done" (one of several times). The time form comes first
 * because a several-times task also has a target above one, and the time is what tells the user which one.
 */
@Composable
internal fun undoMessage(offer: UndoOffer): String {
    val outcome = offer.outcome
    val slot = outcome.slot
    return when {
        slot != null -> stringResource(R.string.tasks_undo_slot_done, offer.name, slot.format(slotFormat))
        outcome.target <= 1 -> stringResource(R.string.tasks_undo_done, offer.name)
        offer.completes -> stringResource(R.string.tasks_undo_progress_done, offer.name, outcome.done, outcome.target)
        else -> stringResource(R.string.tasks_undo_progress, offer.name, outcome.done, outcome.target)
    }
}

/**
 * Shows [offer] with an Undo action on [hostState] and reports how it ended: [onUndo] when the user taps Undo,
 * [onDismissed] when it times out or is swiped away. A newer offer replaces the shown one without calling either,
 * because the older tick stays.
 */
@Composable
internal fun TaskUndoSnackbar(
    offer: UndoOffer?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismissed: () -> Unit,
) {
    val message = offer?.let { undoMessage(it) }
    val actionLabel = stringResource(R.string.tasks_undo_action)
    val currentOnUndo = rememberUpdatedState(onUndo)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    LaunchedEffect(offer) {
        if (message != null) {
            when (hostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short)) {
                SnackbarResult.ActionPerformed -> currentOnUndo.value()
                SnackbarResult.Dismissed -> currentOnDismissed.value()
            }
        }
    }
}
