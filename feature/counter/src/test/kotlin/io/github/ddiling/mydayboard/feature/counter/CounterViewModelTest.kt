package io.github.ddiling.mydayboard.feature.counter

import io.github.ddiling.mydayboard.core.domain.SummarizeTapsUseCase
import io.github.ddiling.mydayboard.core.domain.TapSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterViewModelTest {
    private val viewModel = CounterViewModel(SummarizeTapsUseCase())

    @Test
    fun `starts with no taps`() {
        assertEquals(CounterUiState(0, TapSummary.None), viewModel.uiState.value)
    }

    @Test
    fun `each tap increments the count`() {
        repeat(2) { viewModel.onTap() }

        assertEquals(CounterUiState(2, TapSummary.Some(2)), viewModel.uiState.value)
    }
}
