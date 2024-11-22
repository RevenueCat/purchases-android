package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class BorderTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: Border,
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
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "width": 123
                        }
                        """.trimIndent(),
                    expected = Border(
                        color = ColorScheme(light = ColorInfo.Alias("primary")),
                        width = 123.0
                    )
                )
            ),
            arrayOf(
                "floats",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "width": 456.34
                        }
                        """.trimIndent(),
                    expected = Border(
                        color = ColorScheme(light = ColorInfo.Alias("primary")),
                        width = 456.34
                    )
                )
            ),
        )
    }

    @Test
    fun `Should properly deserialize Border`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<Border>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
