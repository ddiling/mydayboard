package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateCategoryNameTest {
    @Test
    fun `a name is trimmed`() {
        val result = validateCategoryName("  Home ", existingNames = emptyList())

        assertEquals(CategoryNameValidation.Valid("Home"), result)
    }

    @Test
    fun `a name of 30 code points is valid`() {
        val name = "a".repeat(30)

        assertEquals(CategoryNameValidation.Valid(name), validateCategoryName(name, emptyList()))
    }

    @Test
    fun `a name of 31 code points is too long`() {
        val result = validateCategoryName("a".repeat(31), emptyList())

        assertEquals(CategoryNameValidation.TooLong(30), result)
    }

    @Test
    fun `a name equal to an existing one ignoring case is a duplicate`() {
        val result = validateCategoryName("home", existingNames = listOf("Work", "HOME"))

        assertEquals(CategoryNameValidation.Duplicate, result)
    }

    @Test
    fun `the name being renamed is not a duplicate of itself`() {
        val result = validateCategoryName("home", existingNames = listOf("Home"), ignoreName = "Home")

        assertEquals(CategoryNameValidation.Valid("home"), result)
    }
}
