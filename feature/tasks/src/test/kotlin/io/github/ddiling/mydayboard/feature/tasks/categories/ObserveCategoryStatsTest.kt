package io.github.ddiling.mydayboard.feature.tasks.categories

import io.github.ddiling.mydayboard.core.domain.task.computeTaskStats
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCategoryStatsTest {
    private val fixture = CategoryStatsFixture()

    private suspend fun contentOf(categoryId: String) =
        fixture.observe(categoryId).first() as CategoryStatsUiState.Content

    @Test
    fun `the hero value is the credit of all tasks per day`() =
        runTest {
            val meals = fixture.addCategory()
            fixture.addMeal(meals, "Breakfast", doneDays = NINE_DAYS)
            fixture.addMeal(meals, "Lunch", doneDays = NINE_DAYS)
            fixture.addMeal(meals, "Dinner", doneDays = THREE_DAYS)

            val content = contentOf(meals.id)

            assertEquals((NINE_DAYS + NINE_DAYS + THREE_DAYS) / NINE_DAYS.toDouble(), content.perDay, 1e-9)
            assertEquals(21.0, content.doneOfPlanned, 1e-9)
            assertEquals(THREE_TASKS * NINE_DAYS, content.plannedDays)
            assertEquals(NINE_DAYS - THREE_DAYS, content.skipped)
        }

    @Test
    fun `archived tasks are listed apart and still counted`() =
        runTest {
            val meals = fixture.addCategory()
            fixture.addMeal(meals, "Breakfast", doneDays = NINE_DAYS)
            val lunch = fixture.addMeal(meals, "Lunch", doneDays = NINE_DAYS)
            fixture.tasks.archiveTask(lunch.id)

            val content = contentOf(meals.id)

            assertEquals(listOf("Breakfast"), content.tasks.map { it.name })
            assertEquals(listOf("Lunch"), content.archivedLines.map { it.name })
            assertEquals(2.0, content.perDay, 1e-9)
        }

    @Test
    fun `a category without tasks is empty`() =
        runTest {
            val meals = fixture.addCategory()

            assertEquals(CategoryStatsUiState.Empty("Meals"), fixture.observe(meals.id).first())
        }

    @Test
    fun `an unknown category is not found`() =
        runTest {
            assertEquals(CategoryStatsUiState.NotFound, fixture.observe("missing").first())
        }

    @Test
    fun `a deleted category is not found`() =
        runTest {
            val meals = fixture.addCategory()
            fixture.addMeal(meals, "Breakfast", doneDays = NINE_DAYS)
            fixture.categories.deleteCategory(meals.id)

            assertEquals(CategoryStatsUiState.NotFound, fixture.observe(meals.id).first())
        }

    @Test
    fun `each task's skipped count is the one of its own stats`() =
        runTest {
            val meals = fixture.addCategory()
            val dinner = fixture.addMeal(meals, "Dinner", doneDays = THREE_DAYS)
            val ownStats = computeTaskStats(dinner, fixture.ticks.observeTicks(dinner.id).first(), fixture.today)

            val line = contentOf(meals.id).tasks.single()

            assertEquals(ownStats.skipped, line.skipped)
            assertEquals(NINE_DAYS - THREE_DAYS, line.skipped)
        }

    @Test
    fun `at midnight the numbers include the day that just ended`() =
        runTest {
            val late = CategoryStatsFixture(warsaw(2026, 11, 18, 23, 59))
            val meals = late.addCategory()
            late.addMeal(meals, "Breakfast", doneDays = NINE_DAYS)
            val states = late.run { collectStates(meals.id) }
            assertEquals(NINE_DAYS, (states.last() as CategoryStatsUiState.Content).plannedDays)

            late.clock.advanceBy(Duration.ofMinutes(2))
            late.ticker.tryEmit(late.clock.instant())
            runCurrent()

            assertEquals(NINE_DAYS + 1, (states.last() as CategoryStatsUiState.Content).plannedDays)
        }

    @Test
    fun `a zone change at the same instant moves the window`() =
        runTest {
            val evening = CategoryStatsFixture(warsaw(2026, 11, 18, 20, 30))
            val meals = evening.addCategory()
            evening.addMeal(meals, "Breakfast", doneDays = NINE_DAYS)
            val states = evening.run { collectStates(meals.id) }
            assertEquals(NINE_DAYS, (states.last() as CategoryStatsUiState.Content).plannedDays)

            evening.clock.zone = TestZones.TOKYO
            evening.ticker.tryEmit(evening.clock.instant())
            runCurrent()

            assertEquals(NINE_DAYS + 1, (states.last() as CategoryStatsUiState.Content).plannedDays)
        }

    private companion object {
        const val NINE_DAYS = 9
        const val THREE_DAYS = 3
        const val THREE_TASKS = 3
    }
}
