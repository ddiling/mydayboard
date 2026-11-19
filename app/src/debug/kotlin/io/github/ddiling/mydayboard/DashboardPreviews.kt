package io.github.ddiling.mydayboard

import androidx.compose.material3.Card
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
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

@Preview(showBackground = true)
@Composable
private fun EmptyDashboardPreview() {
    AppTheme { DashboardScreen(cards = emptyList(), host = PreviewHost) }
}

@Preview(showBackground = true)
@Composable
private fun DashboardWithTwoCardsPreview() {
    AppTheme { DashboardScreen(cards = listOf(PreviewCard("one", 1), PreviewCard("two", 2)), host = PreviewHost) }
}
