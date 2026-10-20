package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForbiddenPublicConstructorTest {

    private fun rule(ignoreAnnotated: List<String> = emptyList()) = ForbiddenPublicConstructor(
        TestConfig(
            "active" to true,
            "ignoreAnnotated" to ignoreAnnotated,
        ),
    )

    // region should flag

    @Test
    fun `flags primary constructor without visibility modifier`() {
        val findings = rule().lint("public class MyClass(val value: Int)")
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("MyClass"))
    }

    @Test
    fun `flags explicit public primary constructor`() {
        val findings = rule().lint("public class MyClass public constructor(val value: Int)")
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags public secondary constructor`() {
        val code = """
            public class MyClass internal constructor(val value: Int) {
                constructor() : this(0)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags each public constructor separately`() {
        val code = """
            public class MyClass(val value: Int) {
                constructor() : this(0)
                public constructor(value: String) : this(value.length)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `flags class without any declared constructor`() {
        val findings = rule().lint("public class MyClass")
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("MyClass"))
    }

    @Test
    fun `flags abstract class with implicit constructor`() {
        val findings = rule().lint("public abstract class MyClass(val value: Int)")
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags public class nested inside public class`() {
        val code = """
            public class Outer internal constructor() {
                public class Inner(val value: Int)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("Inner"))
    }

    @Test
    fun `flags constructor annotated with an annotation not in the ignore list`() {
        val code = """
            public class MyClass @SomeOtherAnnotation constructor(val value: Int)
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(1, findings.size)
    }

    // endregion

    // region should not flag

    @Test
    fun `does not flag internal primary constructor`() {
        val findings = rule().lint("public class MyClass internal constructor(val value: Int)")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag private primary constructor`() {
        val findings = rule().lint("public class MyClass private constructor(val value: Int)")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag protected primary constructor`() {
        val findings = rule().lint("public abstract class MyClass protected constructor(val value: Int)")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag non-public secondary constructors`() {
        val code = """
            public class MyClass private constructor(val value: Int) {
                internal constructor() : this(0)
                private constructor(value: String) : this(value.length)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag internal class`() {
        val findings = rule().lint("internal class MyClass(val value: Int)")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag private class`() {
        val findings = rule().lint("private class MyClass")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag class nested inside internal class`() {
        val code = """
            internal class Outer {
                class Inner(val value: Int)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag class nested inside private class`() {
        val code = """
            private class Outer {
                class Inner(val value: Int)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag interface`() {
        val findings = rule().lint("public interface MyInterface")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag object`() {
        val code = """
            public object MyObject
            public class MyClass private constructor() {
                public companion object
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag enum class`() {
        val findings = rule().lint("public enum class MyEnum(val value: Int) { A(1) }")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag annotation class`() {
        val findings = rule().lint("public annotation class MyAnnotation(val value: Int)")
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag sealed class`() {
        val code = """
            public sealed class MySealed(val value: Int) {
                constructor() : this(0)
            }
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag class annotated with an ignored annotation`() {
        val code = """
            @InternalRevenueCatAPI
            public class MyClass(val value: Int) {
                constructor() : this(0)
            }
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag constructor annotated with an ignored annotation`() {
        val code = """
            public class MyClass @InternalRevenueCatAPI constructor(val value: Int) {
                @InternalRevenueCatAPI
                constructor() : this(0)
            }
        """.trimIndent()
        val findings = rule(ignoreAnnotated = listOf("InternalRevenueCatAPI")).lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag when suppressed with Suppress annotation`() {
        val code = """
            @Suppress("ForbiddenPublicConstructor")
            public class MyClass(val value: Int)
        """.trimIndent()
        val findings = rule().lint(code)
        assertEquals(0, findings.size)
    }

    // endregion
}
