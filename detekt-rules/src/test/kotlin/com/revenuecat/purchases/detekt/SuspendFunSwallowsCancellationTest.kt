package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class SuspendFunSwallowsCancellationTest {

    private val rule = SuspendFunSwallowsCancellation(TestConfig("active" to true))

    // region should flag

    @Test
    fun `flags catch Exception in suspend fun`() {
        val code = """
            suspend fun doWork() {
                try { fetch() } catch (e: Exception) { log(e) }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags catch Throwable in suspend fun`() {
        val code = """
            suspend fun doWork() {
                try { fetch() } catch (e: Throwable) { log(e) }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags nested try-catch in suspend fun`() {
        val code = """
            suspend fun outer() {
                withContext(Dispatchers.IO) {
                    try { fetch() } catch (e: Exception) { log(e) }
                }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag when sibling catch clause handles CancellationException`() {
        val code = """
            suspend fun doWork() {
                try {
                    fetch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log(e)
                }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when CancellationException is rethrown`() {
        val code = """
            suspend fun doWork() {
                try {
                    fetch()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    log(e)
                }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when ensureActive is called`() {
        val code = """
            suspend fun doWork() {
                try {
                    fetch()
                } catch (e: Exception) {
                    ensureActive()
                    log(e)
                }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag non-suspend fun`() {
        val code = """
            fun doWork() {
                try { fetch() } catch (e: Exception) { log(e) }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag specific exception type`() {
        val code = """
            suspend fun doWork() {
                try { fetch() } catch (e: IOException) { log(e) }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when fully qualified CancellationException is used`() {
        val code = """
            suspend fun doWork() {
                try {
                    fetch()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    log(e)
                }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
