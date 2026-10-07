package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedundantJvmNameWithSyntheticTest {

    private val rule = RedundantJvmNameWithSynthetic(TestConfig("active" to true))

    // region should flag

    @Test
    fun `flags function with both JvmName and JvmSynthetic`() {
        val code = """
            @JvmSynthetic
            @JvmName("bar")
            fun foo() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("redundant"))
    }

    @Test
    fun `flags property with both annotations`() {
        val code = """
            @JvmSynthetic
            @JvmName("_bar")
            val foo: String = ""
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags regardless of annotation order`() {
        val code = """
            @JvmName("bar")
            @JvmSynthetic
            fun foo() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags multiple declarations independently`() {
        val code = """
            @JvmSynthetic
            @JvmName("a")
            fun first() {}

            @JvmSynthetic
            @JvmName("b")
            fun second() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag JvmName without JvmSynthetic`() {
        val code = """
            @JvmName("bar")
            fun foo() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag JvmSynthetic without JvmName`() {
        val code = """
            @JvmSynthetic
            fun foo() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag function with neither annotation`() {
        val code = """
            fun foo() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when annotations are on different declarations`() {
        val code = """
            @JvmSynthetic
            fun foo() {}

            @JvmName("baz")
            fun bar() {}
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
