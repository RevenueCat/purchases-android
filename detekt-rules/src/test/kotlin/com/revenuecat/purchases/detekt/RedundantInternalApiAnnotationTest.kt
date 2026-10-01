package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class RedundantInternalApiAnnotationTest {

    private fun rule() = RedundantInternalApiAnnotation(TestConfig("active" to true))

    // region should flag

    @Test
    fun `flags member annotated inside annotated class`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                @InternalRevenueCatAPI
                fun doWork() {}
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags property annotated inside annotated class`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                @InternalRevenueCatAPI
                val value: Int = 0
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags nested class annotated inside annotated outer`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                @InternalRevenueCatAPI
                class Inner
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags deeply nested member`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                class Middle {
                    @InternalRevenueCatAPI
                    fun deep() {}
                }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags multiple redundant annotations`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                @InternalRevenueCatAPI
                fun a() {}
                @InternalRevenueCatAPI
                fun b() {}
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(2, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag member without annotation`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                fun doWork() {}
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag annotation on class without enclosing annotated type`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                fun doWork() {}
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag top-level annotated class`() {
        val code = """
            @InternalRevenueCatAPI
            class MyClass
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag annotation on unrelated class`() {
        val code = """
            class Outer {
                @InternalRevenueCatAPI
                fun doWork() {}
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when suppressed`() {
        val code = """
            @InternalRevenueCatAPI
            class Outer {
                @Suppress("RedundantInternalApiAnnotation")
                @InternalRevenueCatAPI
                fun doWork() {}
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
