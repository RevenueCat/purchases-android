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
        val findings = rule().lint("public enum class MyEnum { A, B }")
        assertEquals(1, findings.size)
        assertEquals(true, findings[0].message.contains("MyEnum"))
    }

    @Test
    fun `flags implicit public enum class`() {
        val findings = rule().lint("enum class MyEnum { A, B }")
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags multiple enum declarations in one file`() {
        val code = """
            public enum class First { A }
            public enum class Second { B }
            internal enum class ShouldBeIgnored { C }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `flags enum class with an annotation not in the ignore list`() {
        val code = """
            @SomeOtherAnnotation
            public enum class MyEnum { A }
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(1, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag internal enum class`() {
        val findings = rule().lint("internal enum class MyEnum { A }")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag private enum class`() {
        val findings = rule().lint("private enum class MyEnum { A }")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag protected enum class`() {
        val code = """
            open class Outer {
                protected enum class MyEnum { A }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag non-enum class`() {
        val findings = rule().lint("public class MyClass")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum class nested inside internal class`() {
        val code = """
            internal class Outer {
                enum class Inner { A }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum class annotated with an ignored annotation`() {
        val code = """
            @InternalRevenueCatAPI
            public enum class MyEnum { A }
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when suppressed with Suppress annotation`() {
        val code = """
            @Suppress("ForbiddenPublicEnum")
            public enum class MyEnum { A }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum class nested inside private class`() {
        val code = """
            private class Outer {
                enum class Inner { A }
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
