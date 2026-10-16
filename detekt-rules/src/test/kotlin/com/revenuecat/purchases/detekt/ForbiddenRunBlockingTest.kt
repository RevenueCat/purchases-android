package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ForbiddenRunBlockingTest {

    private fun rule() = ForbiddenRunBlocking(TestConfig("active" to true))

    // region should flag

    @Test
    fun `flags runBlocking call`() {
        val code = """
            import kotlinx.coroutines.runBlocking
            fun doWork() {
                runBlocking { println("hello") }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags runBlocking in class method`() {
        val code = """
            import kotlinx.coroutines.runBlocking
            class MyClass {
                fun doWork() {
                    val result = runBlocking { compute() }
                }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags multiple runBlocking calls`() {
        val code = """
            import kotlinx.coroutines.runBlocking
            fun a() { runBlocking { } }
            fun b() { runBlocking { } }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(2, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag other function calls`() {
        val code = """
            fun doWork() {
                println("hello")
                launch { }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when suppressed`() {
        val code = """
            import kotlinx.coroutines.runBlocking
            @Suppress("ForbiddenRunBlocking")
            fun doWork() {
                runBlocking { }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
