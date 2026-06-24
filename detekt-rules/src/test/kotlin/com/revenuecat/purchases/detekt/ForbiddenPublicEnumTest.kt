package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ForbiddenPublicEnumTest {

    private fun rule(ignoreAnnotated: List<String> = emptyList()) = ForbiddenPublicEnum(
        TestConfig(
            "active" to true,
            "ignoreAnnotated" to ignoreAnnotated,
        ),
    )

    // region should flag

    @Test
    fun `flags public enum class`() {
        val code = """
            public enum class MyEnum { A, B }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
        assertEquals(true, findings[0].message.contains("MyEnum"))
    }

    @Test
    fun `flags implicitly public enum class`() {
        val code = """
            enum class MyEnum { A, B }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags enum with annotation not in ignore list`() {
        val code = """
            @SomeOtherAnnotation
            public enum class MyEnum { A, B }
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(1, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag internal enum class`() {
        val findings = rule().lint("internal enum class MyEnum { A, B }")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag private enum class`() {
        val findings = rule().lint("private enum class MyEnum { A, B }")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum nested inside internal class`() {
        val code = """
            internal class Outer {
                enum class Inner { A, B }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum nested inside internal object`() {
        val code = """
            internal object Outer {
                enum class Inner { A, B }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum annotated with ignored annotation`() {
        val code = """
            @InternalRevenueCatAPI
            public enum class MyEnum { A, B }
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when suppressed`() {
        val code = """
            @Suppress("ForbiddenPublicEnum")
            public enum class MyEnum { A, B }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag non-enum class`() {
        val findings = rule().lint("public class MyClass")
        assertEquals(0, findings.size)
    }

    // endregion
}
