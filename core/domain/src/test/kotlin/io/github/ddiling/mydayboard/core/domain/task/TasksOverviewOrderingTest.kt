package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TasksOverviewOrderingTest {
    private val fixture = TasksOverviewFixture()
    private val today = fixture.today

    private suspend fun dashboardNames(limit: Int = 10): List<String> =
        fixture.overview().dashboardRows(limit).map { it.task.name }

    @Test
    fun `the dashboard orders by time, then untimed tasks by name`() =
        runTest {
            fixture.addTask("SQL")
            fixture.addTask("Lunch", time = LocalTime.of(13, 0))
            fixture.addTask("English")
            fixture.addTask("Pills", time = LocalTime.of(9, 0))
            fixture.addTask("Breakfast", time = LocalTime.of(8, 30))

            assertEquals(listOf("Breakfast", "Pills", "Lunch", "English", "SQL"), dashboardNames())
        }

    @Test
    fun `the dashboard shows at most 3 undone rows by default`() =
        runTest {
            val done = fixture.addTask("Breakfast", time = LocalTime.of(7, 0))
            fixture.ticks.addTick(done.id, today, null)
            fixture.addTask("Pills", time = LocalTime.of(9, 0))
            fixture.addTask("Lunch", time = LocalTime.of(13, 0))
            fixture.addTask("Vacuum", time = LocalTime.of(15, 0))
            fixture.addTask("English", time = LocalTime.of(18, 0))

            val rows = fixture.overview().dashboardRows()

            assertEquals(listOf("Pills", "Lunch", "Vacuum"), rows.map { it.task.name })
        }

    @Test
    fun `a several-times task is ordered by its next undone time and shows 1 of 2`() =
        runTest {
            val times = Repeat.TimesPerDay(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
            val pills = fixture.addTask("Pills", repeat = times)
            fixture.addTask("Lunch", time = LocalTime.of(12, 0))
            fixture.ticks.addTick(pills.id, today, LocalTime.of(8, 0))

            val row = fixture.rowOf(pills)

            assertEquals(LocalTime.of(20, 0), row.time)
            assertEquals(1, row.done)
            assertEquals(2, row.target)
            assertEquals(listOf("Lunch", "Pills"), dashboardNames())
        }

    @Test
    fun `a task with a time sorts before one without even when its name is later`() =
        runTest {
            fixture.addTask("Apple")
            fixture.addTask("Zumba", time = LocalTime.of(10, 0))

            assertEquals(listOf("Zumba", "Apple"), dashboardNames())
        }

    @Test
    fun `names are ordered ignoring case`() =
        runTest {
            fixture.addTask("SQL")
            fixture.addTask("english")

            assertEquals(listOf("english", "SQL"), dashboardNames())
        }

    @Test
    fun `groups are alphabetical, No category is last and a deleted category lands there`() =
        runTest {
            val chores = fixture.categories.createCategory("chores")
            val study = fixture.categories.createCategory("Study")
            val gone = fixture.categories.createCategory("Gone")
            fixture.addTask("SQL") { copy(categoryId = study.id) }
            fixture.addTask("Vacuum") { copy(categoryId = chores.id) }
            fixture.addTask("Water filter")
            fixture.addTask("Orphan") { copy(categoryId = gone.id) }
            fixture.categories.deleteCategory(gone.id)

            val groups = fixture.overview().groupedByCategory()

            assertEquals(listOf("chores", "Study", null), groups.map { it.category?.name })
            assertEquals(listOf("Orphan", "Water filter"), groups.last().rows.map { it.task.name })
        }

    @Test
    fun `inside a group tasks due today come first by time, then the others by name`() =
        runTest {
            val monday = Repeat.Weekdays(setOf(java.time.DayOfWeek.MONDAY))
            fixture.addTask("Aardvark", repeat = monday)
            fixture.addTask("Vacuum")
            fixture.addTask("Breakfast", time = LocalTime.of(8, 0))

            val rows =
                fixture
                    .overview()
                    .groupedByCategory()
                    .single()
                    .rows

            assertEquals(listOf("Breakfast", "Vacuum", "Aardvark"), rows.map { it.task.name })
        }

    @Test
    fun `a late stays task is not on the dashboard`() =
        runTest {
            val monthly = Repeat.Monthly(15)
            fixture.addTask("Pay bills", repeat = monthly, startDate = java.time.LocalDate.of(2026, 11, 1)) {
                copy(onMiss = OnMiss.STAYS)
            }

            assertTrue(dashboardNames().isEmpty())
        }

    @Test
    fun `when everything is done the dashboard is empty`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            fixture.ticks.addTick(task.id, today, null)

            assertTrue(dashboardNames().isEmpty())
        }
}
