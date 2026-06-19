package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ForbiddenPublicSealedClassTest {

    private fun rule(ignoreAnnotated: List<String> = emptyList()) = ForbiddenPublicSealedClass(
        TestConfig(
            "active" to true,
            "ignoreAnnotated" to ignoreAnnotated,
        ),
    )

    // region should flag

    @Test
    fun `flags public sealed class`() {
        val findings = rule().lint("public sealed class MySealedClass")
        assertEquals(1, findings.size)
        assertEquals(true, findings[0].message.contains("MySealedClass"))
    }

    @Test
    fun `flags public sealed interface`() {
        val findings = rule().lint("public sealed interface MySealedInterface")
        assertEquals(1, findings.size)
        assertEquals(true, findings[0].message.contains("MySealedInterface"))
    }

    @Test
    fun `flags multiple sealed declarations in one file`() {
        val code = """
            public sealed class First
            public sealed interface Second
            internal sealed class ShouldBeIgnored
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `flags sealed class with an annotation that is not in the ignore list`() {
        val code = """
            @SomeOtherAnnotation
            public sealed class MySealedClass
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(1, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag internal sealed class`() {
        val findings = rule().lint("internal sealed class MySealedClass")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag private sealed class`() {
        val findings = rule().lint("private sealed class MySealedClass")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag protected sealed class`() {
        val code = """
            open class Outer {
                protected sealed class MySealedClass
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag non-sealed class`() {
        val findings = rule().lint("public class MyClass")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag sealed class nested inside internal class`() {
        val code = """
            internal class Outer {
                sealed class Inner
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag sealed class nested inside private class`() {
        val code = """
            private class Outer {
                sealed class Inner
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag sealed class annotated with an ignored annotation`() {
        val code = """
            @InternalRevenueCatAPI
            public sealed class MySealedClass
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when suppressed with Suppress annotation`() {
        val code = """
            @Suppress("ForbiddenPublicSealedClass")
            public sealed class MySealedClass
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag sealed class nested inside internal object`() {
        val code = """
            internal object Outer {
                sealed class Inner
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
