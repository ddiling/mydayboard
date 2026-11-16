package io.github.ddiling.mydayboard.feature.counter

import androidx.lifecycle.ViewModel
import io.github.ddiling.mydayboard.core.domain.SummarizeTapsUseCase
import io.github.ddiling.mydayboard.core.domain.TapSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CounterUiState(
    val tapCount: Int,
    val summary: TapSummary,
)

class CounterViewModel(
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
