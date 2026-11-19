package io.github.ddiling.mydayboard.feature.counter

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.SummarizeTapsUseCase
import io.github.ddiling.mydayboard.core.domain.TapSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CounterUiState(
    val tapCount: Int,
    val summary: TapSummary,
)

/**
 * Holds the tap count for [CounterRoute].
 *
 * `@HiltViewModel` + `@Inject constructor` let `hiltViewModel()` create it with its dependencies from the
 * Hilt graph, so neither the screen nor the app needs a hand-written factory. Tests still call the
 * constructor directly with real objects.
 */
@HiltViewModel
class CounterViewModel
    @Inject
    constructor(
        private val summarizeTaps: SummarizeTapsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CounterUiState(tapCount = 0, summary = summarizeTaps(0)))
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

        fun onTap() {
            _uiState.update { current ->
                val count = current.tapCount + 1
                current.copy(tapCount = count, summary = summarizeTaps(count))
            }
        }
    }
