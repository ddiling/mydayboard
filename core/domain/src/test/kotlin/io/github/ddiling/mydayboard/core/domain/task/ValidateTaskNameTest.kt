package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateTaskNameTest {
    @Test
    fun `a name is trimmed and its inner spaces collapsed`() {
        val result = validateTaskName("  Water \n  filter ")

        assertEquals(TaskNameValidation.Valid("Water filter"), result)
    }

    @Test
    fun `an empty name is blank`() {
        assertEquals(TaskNameValidation.Blank, validateTaskName(""))
    }

    @Test
    fun `a name of only whitespace is blank`() {
        assertEquals(TaskNameValidation.Blank, validateTaskName(" \t\n "))
    }

    @Test
    fun `a name of 60 code points is valid`() {
        val name = "😀".repeat(60)

        assertEquals(TaskNameValidation.Valid(name), validateTaskName(name))
    }

    @Test
    fun `a name of 61 code points is too long`() {
        val result = validateTaskName("a".repeat(61))

        assertEquals(TaskNameValidation.TooLong(60), result)
    }
}
