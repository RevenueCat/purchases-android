package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PaddingTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: Padding,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "ints",
                Args(
                    json = """
                        {
                          "top": 1,
                          "bottom": 2,
                          "leading": 3,
                          "trailing": 4
                        }
                        """.trimIndent(),
                    expected = Padding(
                        top = 1.0,
                        bottom = 2.0,
                        leading = 3.0,
                        trailing = 4.0,
                    )
                )
            ),
            arrayOf(
                "floats",
                Args(
                    json = """
                        {
                          "top": 1.2,
                          "bottom": 2.3,
                          "leading": 3.4,
                          "trailing": 4.5
                        }
                        """.trimIndent(),
                    expected = Padding(
                        top = 1.2,
                        bottom = 2.3,
                        leading = 3.4,
                        trailing = 4.5,
                    )
                )
            ),
        )
    }

    @Test
    fun `Should properly deserialize Padding`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<Padding>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
