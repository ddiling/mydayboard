package io.github.ddiling.mydayboard.feature.trackers.reasons

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import java.time.Instant

private val previewReasons =
    listOf("I sleep better", "I save money", "I feel calmer in the evening").mapIndexed { i, text ->
        TrackerReason("r$i", "water", text, Instant.EPOCH.plusSeconds(i.toLong()), Instant.EPOCH)
    }

private val previewContent = TrackerReasonsUiState.Content("No sweets", previewReasons)

@Composable
private fun Preview(state: TrackerReasonsUiState) =
    PreviewSurface { TrackerReasonsScreen(state, TrackerReasonsActions()) }

@PreviewLightDark
@Composable
private fun TrackerReasonsPreview() = Preview(previewContent)

@PreviewLightDark
@Composable
private fun TrackerReasonsEmptyPreview() = Preview(previewContent.copy(reasons = emptyList()))

@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackerReasonsLargeFontPreview() = Preview(previewContent)
