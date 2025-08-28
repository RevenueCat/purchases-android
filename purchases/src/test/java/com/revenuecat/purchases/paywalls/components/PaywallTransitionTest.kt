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
                            "ms_duration": 300,
                            "type": "ease_in_out"
                          },
                          "displacement_strategy": "greedy",
                          "type": "fade_and_scale"
                        }
                    """.trimIndent(),
                    expected = PaywallTransition(
                        type = PaywallTransition.TransitionType.FADE_AND_SCALE,
                        displacementStrategy = PaywallTransition.DisplacementStrategy.GREEDY,
                        animation = PaywallAnimation(
                            type = PaywallAnimation.AnimationType.EASE_IN_OUT,
                            msDelay = 1500,
                            msDuration = 300,
                        ),
                    ),
                ),
            ),
            arrayOf(
                "fade null",
                Args(
                    json = """
                        {
                          "displacement_strategy": "lazy",
                          "type": "fade"
                        }
                    """.trimIndent(),
                    expected = PaywallTransition(
                        type = PaywallTransition.TransitionType.FADE,
                        displacementStrategy = PaywallTransition.DisplacementStrategy.LAZY,
                        animation = null,
                    ),
                ),
            ),
            arrayOf(
                "custom custom -> default serialization take effect",
                Args(
                    json = """
                        {
                          "animation": {
                            "ms_delay": 0,
                            "ms_duration": 100,
                            "type": "custom"
                          },
                          "displacement_strategy": "greedy",
                          "type": "custom"
                        }
                    """.trimIndent(),
                    expected = PaywallTransition(
                        type = PaywallTransition.TransitionType.FADE,
                        displacementStrategy = PaywallTransition.DisplacementStrategy.GREEDY,
                        animation = PaywallAnimation(
                            type = PaywallAnimation.AnimationType.EASE_IN_OUT,
                            msDelay = 0,
                            msDuration = 100,
                        ),
                    ),
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
