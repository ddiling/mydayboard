package io.github.ddiling.mydayboard.feature.trackers.dashboard

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
        TrackersDashboardRow.Counter("3", "Stretching", todayCount = 0, sinceLast = null),
        TrackersDashboardRow.DaysSince("4", "Reading before bed", days = 1),
    )

@Composable
private fun PreviewCard(
    rows: List<TrackersDashboardRow>,
    totalCount: Int = rows.size,
) {
    PreviewSurface {
        TrackersDashboardCardContent(
            TrackersDashboardCardUiState.Content(rows, totalCount, pendingUndo = null),
            TrackersDashboardCardActions(),
            PreviewHost,
            Modifier.padding(horizontal = 20.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackersDashboardCardFourPreview() {
    PreviewCard(previewRows)
}

@PreviewLightDark
@Composable
private fun TrackersDashboardCardTwoPreview() {
    PreviewCard(previewRows.take(2))
}

@PreviewLightDark
@Composable
private fun TrackersDashboardCardEmptyPreview() {
    PreviewCard(emptyList())
}

@PreviewLightDark
@Composable
private fun TrackersDashboardCardSevenPreview() {
    PreviewCard(previewRows, totalCount = 7)
}

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackersDashboardCardLargeFontPreview() {
    PreviewCard(previewRows)
}
