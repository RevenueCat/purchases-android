package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.JsonTools
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ShapeTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        public val json: String,
        public val expected: Shape,
    )

    public companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = listOf(
            arrayOf(
                "rectangle - corners present",
                Args(
                    json = """
                        {
                          "corners": {
                            "top_leading": 1,
                            "top_trailing": 2,
                            "bottom_leading": 3,
                            "bottom_trailing": 4
                          },
                          "type": "rectangle"
                        }
                        """.trimIndent(),
                    expected = Shape.Rectangle(
                        corners = CornerRadiuses.Dp(
                            topLeading = 1.0,
                            topTrailing = 2.0,
                            bottomLeading = 3.0,
                            bottomTrailing = 4.0
                        )
                    )
                )
            ),
            arrayOf(
                "rectangle - corners absent",
                Args(
                    json = """
                        {
                          "type": "rectangle"
                        }
                        """.trimIndent(),
                    expected = Shape.Rectangle()
                )
            ),
            arrayOf(
                "rectangle - corners null",
                Args(
                    json = """
                        {
                          "corners": null,
                          "type": "rectangle"
                        }
                        """.trimIndent(),
                    expected = Shape.Rectangle()
                )
            ),
            arrayOf(
                "pill",
                Args(
                    json = """
                        {
                          "type": "pill"
                        }
                        """.trimIndent(),
                    expected = Shape.Pill
                )
            ),
            arrayOf(
                "unknown",
                Args(
                    json = """
                        {
                          "unknown_property": "some_value",
                          "type": "some_unknown_type"
                        }
                        """.trimIndent(),
                    expected = Shape.Rectangle(corners = null)
                )
            ),
        )
    }

    @Test
    fun `Should properly deserialize Shape`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<Shape>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
