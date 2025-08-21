package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PaywallTransitionTest(@Suppress("UNUSED_PARAMETER") name: String, private val args: PaywallTransitionTest.Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: PaywallTransition,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "fade_and_scale ease-in-out",
                Args(
                    json = """
                        {
                          "animation": {
                            "ms_delay": 1500,
                            "type": {
                              "type": "ease_in_out"
                            }
                          },
                          "displacement_strategy": "greedy",
                          "type": {
                            "type": "fade_and_scale"
                          }
                        }
                        """.trimIndent(),
                    expected = PaywallTransition(
                        type = PaywallTransition.TransitionType.FadeAndScale,
                        displacementStrategy = PaywallTransition.DisplacementStrategy.GREEDY,
                        animation = PaywallAnimation(
                            type = PaywallAnimation.AnimationType.EaseInOut,
                            msDelay = 1500,
                            msDuration = null
                        )
                    )
                ),
            ),
            arrayOf(
                "fade null",
                Args(
                    json = """
                        {
                          "displacement_strategy": "lazy",
                          "type": {
                            "type": "fade"
                          }
                        }
                        """.trimIndent(),
                    expected = PaywallTransition(
                        type = PaywallTransition.TransitionType.Fade,
                        displacementStrategy = PaywallTransition.DisplacementStrategy.LAZY,
                        animation = null
                    )
                ),
            ),
            arrayOf(
                "custom custom",
                Args(
                    json = """
                        {
                          "animation": {
                            "ms_delay": 1500,
                            "type": {
                              "type": "custom",
                              "value": "some random animation"
                            }
                          },
                          "displacement_strategy": "greedy",
                          "type": {
                            "type": "custom",
                            "value": "some random transition"
                          }
                        }
                        """.trimIndent(),
                    expected = PaywallTransition(
                        type = PaywallTransition.TransitionType.Custom("some random transition"),
                        displacementStrategy = PaywallTransition.DisplacementStrategy.GREEDY,
                        animation = PaywallAnimation(
                            type = PaywallAnimation.AnimationType.Custom("some random animation"),
                            msDelay = 1500,
                            msDuration = null
                        )
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PaywallTransition`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PaywallTransition>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}