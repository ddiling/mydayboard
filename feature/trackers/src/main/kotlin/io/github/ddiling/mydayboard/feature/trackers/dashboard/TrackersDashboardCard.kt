package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerReasonsDestination
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPromptActions
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPromptDialog
import javax.inject.Inject

/** The trackers card on the dashboard: every tracker with a quick action, right where the user starts. */
internal class TrackersDashboardCard
    @Inject
    constructor() : DashboardCard {
        override val key: String = "trackers"
        override val order: Int = CARD_ORDER

        @Composable
        override fun Content(
            host: DashboardHost,
            modifier: Modifier,
        ) {
            val viewModel: TrackersDashboardCardViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val actions =
                TrackersDashboardCardActions(
                    onPlusOne = viewModel::onPlusOne,
                    onReset = viewModel::onReset,
                    onUndo = viewModel::onUndo,
                    onUndoDismissed = viewModel::onUndoDismissed,
                )
            val prompt by viewModel.prompt.collectAsStateWithLifecycle()
            val promptActions =
                ReasonPromptActions(
                    onWait = viewModel::onPromptWait,
                    onAnother = viewModel::onPromptAnother,
                    onLogAnyway = viewModel::onPromptLogAnyway,
                    onAddFirstReason = { trackerId ->
                        viewModel.onPromptAddFirstReason()
                        host.navigate(TrackerReasonsDestination(trackerId))
                    },
                )
            TrackersDashboardCardContent(uiState, actions, host, modifier)
            ReasonPromptDialog(prompt, promptActions)
        }

        private companion object {
            const val CARD_ORDER = 100
        }
    }

/** Contributes the trackers card to the dashboard's set of cards. */
@Module
@InstallIn(SingletonComponent::class)
internal interface TrackersDashboardModule {
    @Binds
    @IntoSet
    fun bindTrackersDashboardCard(card: TrackersDashboardCard): DashboardCard
}
