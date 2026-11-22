package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaskDetailHistoryTest {
    private val fixture = TaskDetailFixture()
    private val lastFriday = LocalDate.of(2026, 11, 13)

    @Test
    fun `an overdue one-time task that can still be done shows as late and not as missed`() =
        runTest {
            val task = fixture.addTask("Electricity bill", Repeat.OneTime(lastFriday), onMiss = OnMiss.STAYS)

            assertEquals(listOf(HistoryRow(lastFriday, HistoryResult.Late)), fixture.content(task).history)
        }
}
