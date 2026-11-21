package io.github.ddiling.mydayboard.previews

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.DashboardHeader
import io.github.ddiling.mydayboard.DashboardScreen
import io.github.ddiling.mydayboard.HeaderTime
import io.github.ddiling.mydayboard.SettingsScreen
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.designsystem.theme.AppTheme
import io.github.ddiling.mydayboard.core.navigation.Destination

private object PreviewHost : DashboardHost {
    override val snackbarHostState = SnackbarHostState()

    override fun navigate(destination: Destination) = Unit
}

private class PreviewCard(
    override val key: String,
    override val order: Int,
) : DashboardCard {
    @Composable
    override fun Content(
        host: DashboardHost,
        modifier: Modifier,
    ) = Card(modifier) { Text(text = "Card $key") }
}

private val previewTime = HeaderTime(time = "07:05", date = "03.01", weekday = "Sat")

@Preview
@Composable
private fun DashboardHeaderPreview() {
    PreviewSurface { DashboardHeader(time = previewTime, onOpenSettings = {}, modifier = Modifier.padding(20.dp)) }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme { SettingsScreen(onBack = {}) }
}

@Preview
@Composable
private fun EmptyDashboardPreview() {
    AppTheme { DashboardScreen(headerTime = previewTime, cards = emptyList(), host = PreviewHost) }
}

@Preview
@Composable
private fun DashboardWithTwoCardsPreview() {
    AppTheme {
        DashboardScreen(
            headerTime = previewTime,
            cards = listOf(PreviewCard("one", 1), PreviewCard("two", 2)),
            host = PreviewHost,
        )
    }
}
