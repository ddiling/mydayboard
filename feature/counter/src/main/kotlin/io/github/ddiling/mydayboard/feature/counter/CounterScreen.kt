package io.github.ddiling.mydayboard.feature.counter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ddiling.mydayboard.core.designsystem.component.PrimaryButton
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.SummarizeTapsUseCase
import io.github.ddiling.mydayboard.core.domain.TapSummary

/**
 * Public entry point of the feature: the only API apps use.
 * Swap the ViewModel factory for your DI of choice.
 */
@Composable
fun CounterRoute(
    modifier: Modifier = Modifier,
    viewModel: CounterViewModel = viewModel { CounterViewModel(SummarizeTapsUseCase()) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CounterScreen(uiState = uiState, onTap = viewModel::onTap, modifier = modifier)
}

@Composable
internal fun CounterScreen(
    uiState: CounterUiState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = uiState.summary.asText(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("tap_summary"),
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = stringResource(R.string.counter_tap_me),
            onClick = onTap,
            modifier = Modifier.testTag("tap_button"),
        )
    }
}

@Composable
private fun TapSummary.asText(): String =
    when (this) {
        TapSummary.None -> stringResource(R.string.counter_no_taps_yet)
        is TapSummary.Some -> pluralStringResource(R.plurals.counter_taps, count, count)
    }

@PreviewLightDark
@Composable
private fun CounterScreenEmptyPreview() {
    PreviewSurface {
        CounterScreen(uiState = CounterUiState(0, TapSummary.None), onTap = {})
    }
}

@PreviewLightDark
@Composable
private fun CounterScreenTappedPreview() {
    PreviewSurface {
        CounterScreen(uiState = CounterUiState(3, TapSummary.Some(3)), onTap = {})
    }
}
