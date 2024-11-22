package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class SizeTests {

    @RunWith(Parameterized::class)
    class DeserializeSizeConstraintTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: SizeConstraint,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "fit - value absent",
                    Args(
                        json = """
                            {
                              "type": "fit"
                            }
                        """.trimIndent(),
                        expected = SizeConstraint.Fit
                    )
                ),
                arrayOf(
                    "fit - value null",
                    Args(
                        json = """
                            {
                              "type": "fit",
                              "value": null
                            }
                        """.trimIndent(),
                        expected = SizeConstraint.Fit
                    )
                ),
                arrayOf(
                    "fill - value absent",
                    Args(
                        json = """
                            {
                              "type": "fill"
                            }
                        """.trimIndent(),
                        expected = SizeConstraint.Fill
                    )
                ),
                arrayOf(
                    "fill - value null",
                    Args(
                        json = """
                            {
                              "type": "fill",
                              "value": null
                            }
                        """.trimIndent(),
                        expected = SizeConstraint.Fill
                    )
                ),
                arrayOf(
                    "fixed - value zero",
                    Args(
                        json = """
                            {
                              "type": "fixed",
                              "value": 0
                            }
                        """.trimIndent(),
                        expected = SizeConstraint.Fixed(
                            value = 0.toUInt()
                        )
                    )
                ),
                arrayOf(
                    "fixed - value positive",
                    Args(
                        json = """
                            {
                              "type": "fixed",
                              "value": 100
                            }
                        """.trimIndent(),
                        expected = SizeConstraint.Fixed(
                            value = 100.toUInt()
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize SizeConstraint`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<SizeConstraint>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializeSizeTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: Size,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "fit x fill",
                    Args(
                        json = """
                            {
                              "width": {
                                "type": "fit",
                                "value": null
                              },
                              "height": {
                                "type": "fill",
                                "value": null
                              }
                            }
                        """.trimIndent(),
                        expected = Size(
                            width = SizeConstraint.Fit,
                            height = SizeConstraint.Fill,
                        )
                    )
                ),
                arrayOf(
                    "fill x fixed(123)",
                    Args(
                        json = """
                            {
                              "width": {
                                "type": "fill",
                                "value": null
                              },
                              "height": {
                                "type": "fixed",
                                "value": 123
                              }
                            }
                        """.trimIndent(),
                        expected = Size(
                            width = SizeConstraint.Fill,
                            height = SizeConstraint.Fixed(
                                value = 123.toUInt()
                            ),
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize Size`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<Size>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
