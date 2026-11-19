package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.data.di.TimeModule
import io.github.ddiling.mydayboard.core.data.di.TimeModule_ClockFactory
import io.github.ddiling.mydayboard.core.data.di.TimeModule_IdsFactory
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimeModuleTest {
    @Test
    fun `the provided clock reads the real time`() {
        val clock = TimeModule.clock()

        assertNotEquals(0L, clock.millis())
    }

    @Test
    fun `the provided id generator returns a different id each time`() {
        val ids = TimeModule.ids()

        assertNotEquals(ids.newId(), ids.newId())
    }

    @Test
    fun `the generated clock factory builds a clock that reads the real time`() {
        val clock = TimeModule_ClockFactory.create().get()

        assertNotEquals(0L, clock.millis())
    }

    @Test
    fun `the generated id factory builds a working generator`() {
        val ids = TimeModule_IdsFactory.create().get()

        assertNotEquals(ids.newId(), ids.newId())
    }
}
