package io.github.ddiling.mydayboard.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoAnyTypeTest {
    private fun findings(code: String) = NoAnyType(Config.empty).lint(code)

    @Test
    fun `reports Any as a parameter type`() {
        assertEquals(1, findings("fun f(value: Any) = value").size)
    }

    @Test
    fun `reports nullable Any as a return type`() {
        assertEquals(1, findings("fun f(): Any? = null").size)
    }

    @Test
    fun `reports Any as a property type`() {
        assertEquals(1, findings("val value: Any = 1").size)
    }

    @Test
    fun `reports Any as a type argument in a declaration`() {
        assertEquals(1, findings("val values: List<Any> = emptyList()").size)
    }

    @Test
    fun `reports Any as a type argument in a call`() {
        assertEquals(1, findings("val values = emptyList<Any>()").size)
    }

    @Test
    fun `reports Any in a cast`() {
        assertEquals(1, findings("fun f(x: String) = x as Any").size)
    }

    @Test
    fun `message tells what to use instead`() {
        assertTrue(findings("val value: Any = 1").single().message.contains("exact type"))
    }

    @Test
    fun `allows Any in the parameter of an equals override`() {
        val code = "class A { override fun equals(other: Any?): Boolean = true }"

        assertTrue(findings(code).isEmpty())
    }

    @Test
    fun `reports Any in a non-override function named equals`() {
        assertEquals(1, findings("class A { fun equals(other: Any?): Boolean = true }").size)
    }

    @Test
    fun `reports Any in the return type of an equals override`() {
        val code = "class A { override fun equals(other: Any?): Any = true }"

        assertEquals(1, findings(code).size)
    }

    @Test
    fun `ignores the word Any in comments and strings`() {
        val code =
            """
            /** Any value works here. */
            // Any other
            val text = "Any"
            """.trimIndent()

        assertTrue(findings(code).isEmpty())
    }

    @Test
    fun `allows exact types`() {
        assertTrue(findings("val values: List<String> = emptyList<String>()").isEmpty())
    }

    @Test
    fun `ignores Any in a supertype list`() {
        assertTrue(findings("open class A : Any()").isEmpty())
    }
}
