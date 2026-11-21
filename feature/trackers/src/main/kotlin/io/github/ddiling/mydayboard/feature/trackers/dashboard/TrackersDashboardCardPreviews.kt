package io.github.ddiling.mydayboard.feature.trackers.dashboard

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.navigation.Destination
import java.time.Duration

private object PreviewHost : DashboardHost {
    override val snackbarHostState = SnackbarHostState()

    override fun navigate(destination: Destination) = Unit
}

private val previewRows =
    listOf(
        TrackersDashboardRow.Counter("1", "Water", todayCount = 3, sinceLast = Duration.ofMinutes(200)),
        TrackersDashboardRow.DaysSince("2", "No sugar", days = 12),
    )

@Composable
private fun PreviewCard(uiState: TrackersDashboardCardUiState) {
    PreviewSurface { TrackersDashboardCardContent(uiState, TrackersDashboardCardActions(), PreviewHost) }
}

@PreviewLightDark
@Composable
private fun TrackersDashboardCardEmptyPreview() {
    PreviewCard(TrackersDashboardCardUiState.Content(emptyList(), totalCount = 0, pendingUndo = null))
}

@PreviewLightDark
@Composable
private fun TrackersDashboardCardRowsPreview() {
    PreviewCard(TrackersDashboardCardUiState.Content(previewRows, totalCount = previewRows.size, pendingUndo = null))
}

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackersDashboardCardLargeFontPreview() {
    PreviewCard(TrackersDashboardCardUiState.Content(previewRows, totalCount = previewRows.size, pendingUndo = null))
}
