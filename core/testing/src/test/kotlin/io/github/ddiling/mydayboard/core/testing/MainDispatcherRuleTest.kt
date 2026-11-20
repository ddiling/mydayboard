package io.github.ddiling.mydayboard.core.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    @Test
    fun `the default dispatcher runs launched work eagerly`() {
        var ran = false

        runWithRule(MainDispatcherRule()) { CoroutineScope(Dispatchers.Main).launch { ran = true } }

        assertTrue(ran)
    }

    @Test
    fun `a passed dispatcher is the one installed as Main`() {
        val standard = StandardTestDispatcher()
        var ran = false
        var ranBeforeAdvancing = true

        runWithRule(MainDispatcherRule(standard)) {
            CoroutineScope(Dispatchers.Main).launch { ran = true }
            ranBeforeAdvancing = ran
            standard.scheduler.advanceUntilIdle()
        }

        assertFalse(ranBeforeAdvancing)
        assertTrue(ran)
    }

    @Test
    fun `Main is no longer the test dispatcher once the test has finished`() {
        runWithRule(MainDispatcherRule()) { }

        assertThrows(IllegalStateException::class.java) { Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext) }
    }

    private fun runWithRule(
        rule: MainDispatcherRule,
        body: () -> Unit,
    ) {
        val statement =
            object : Statement() {
                override fun evaluate() = body()
            }
        rule.apply(statement, Description.EMPTY).evaluate()
    }
}
