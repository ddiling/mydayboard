package io.github.ddiling.mydayboard.feature.trackers.reason

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import java.time.Instant

private val previewReason = TrackerReason("r1", "t1", "I sleep better", Instant.EPOCH, Instant.EPOCH)

private val previewCounter =
    ReasonPrompt.WithReason("t1", "Water", ReasonPromptKind.COUNTER, previewReason, canSwap = true)

@Composable
private fun PromptPreview(prompt: ReasonPrompt) = PreviewSurface { ReasonPromptSurface(prompt, ReasonPromptActions()) }

@PreviewLightDark
@Composable
private fun ReasonPromptCounterPreview() = PromptPreview(previewCounter)

@PreviewLightDark
@Composable
private fun ReasonPromptDaysSincePreview() =
    PromptPreview(previewCounter.copy(trackerName = "No sweets", kind = ReasonPromptKind.DAYS_SINCE))

@PreviewLightDark
@Composable
private fun ReasonPromptSingleReasonPreview() = PromptPreview(previewCounter.copy(canSwap = false))

@PreviewLightDark
@Composable
private fun ReasonPromptNoReasonsPreview() =
    PromptPreview(ReasonPrompt.NoReasons("t1", "Water", ReasonPromptKind.COUNTER))

@Preview(name = "Reason prompt large font light", fontScale = 2f)
@Preview(name = "Reason prompt large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ReasonPromptLargeFontPreview() = PromptPreview(previewCounter)
