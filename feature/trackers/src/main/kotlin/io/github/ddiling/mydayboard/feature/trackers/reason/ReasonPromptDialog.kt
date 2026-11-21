package io.github.ddiling.mydayboard.feature.trackers.reason

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.ddiling.mydayboard.core.designsystem.theme.DialogOutlineColor
import io.github.ddiling.mydayboard.core.designsystem.theme.DialogSurfaceColor
import io.github.ddiling.mydayboard.feature.trackers.R

private val DialogCorner = 28.dp

/**
 * Shows [prompt] centered over the screen, or nothing when it is null. A tap outside and Back count as the
 * "wait" choice, so leaving the dialog never logs anything by accident.
 */
@Composable
internal fun ReasonPromptDialog(
    prompt: ReasonPrompt?,
    actions: ReasonPromptActions,
) {
    if (prompt == null) return
    Dialog(onDismissRequest = actions.onWait) {
        ReasonPromptSurface(prompt, actions)
    }
}

/**
 * The dialog's card: the dark surface with a thin outline, as in the design. It is apart from the window so
 * previews show the same card the user sees.
 */
@Composable
internal fun ReasonPromptSurface(
    prompt: ReasonPrompt,
    actions: ReasonPromptActions,
) {
    Surface(
        shape = RoundedCornerShape(DialogCorner),
        color = DialogSurfaceColor,
        border = BorderStroke(1.dp, DialogOutlineColor),
    ) {
        ReasonPromptContent(prompt, actions)
    }
}

/**
 * The dialog's content, apart from the window so previews can render it. The reason comes before the buttons in
 * reading order, so a screen reader says why before it offers a choice. The content scrolls, so a large font
 * cannot push a button off the screen. The "wait" button is the big one on purpose: staying put is the easy
 * choice, and logging is the quiet grey text.
 */
@Composable
internal fun ReasonPromptContent(
    prompt: ReasonPrompt,
    actions: ReasonPromptActions,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("trackers_reason_dialog"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(prompt.trackerName.uppercase(), color = muted, fontSize = 12.sp, letterSpacing = 0.8.sp)
        when (prompt) {
            is ReasonPrompt.WithReason -> {
                ReasonText(prompt, actions.onAnother)
            }

            is ReasonPrompt.NoReasons -> {
                Text(stringResource(R.string.trackers_reason_dialog_no_reasons), color = muted, fontSize = 15.sp)
            }
        }
        val isCounter = prompt.kind == ReasonPromptKind.COUNTER
        val bigLabel =
            when {
                prompt is ReasonPrompt.NoReasons -> R.string.trackers_reason_dialog_add_first
                isCounter -> R.string.trackers_reason_dialog_wait
                else -> R.string.trackers_reason_dialog_hold_on
            }
        val onBig =
            if (prompt is ReasonPrompt.NoReasons) {
                (
                    {
                        actions.onAddFirstReason(
                            prompt.trackerId,
                        )
                    }
                )
            } else {
                actions.onWait
            }
        FilledTonalButton(
            onBig,
            Modifier
                .padding(
                    top = 16.dp,
                ).fillMaxWidth()
                .testTag("trackers_reason_dialog_big"),
        ) { Text(stringResource(bigLabel), textAlign = TextAlign.Center) }
        val logLabel =
            if (isCounter) R.string.trackers_reason_dialog_log_anyway else R.string.trackers_reason_dialog_reset_anyway
        TextButton(
            actions.onLogAnyway,
            Modifier.fillMaxWidth().testTag("trackers_reason_dialog_log"),
        ) { Text(stringResource(logLabel), color = muted) }
    }
}

@Composable
private fun ReasonText(
    prompt: ReasonPrompt.WithReason,
    onAnother: () -> Unit,
) {
    val quote = stringResource(R.string.trackers_reason_dialog_quote, prompt.reason.text)
    Text(quote, Modifier.padding(top = 6.dp).testTag("trackers_reason_dialog_reason"), fontSize = 22.sp)
    if (prompt.canSwap) {
        TextButton(
            onAnother,
            Modifier.testTag("trackers_reason_dialog_another"),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Text(stringResource(R.string.trackers_reason_dialog_another))
        }
    }
}
