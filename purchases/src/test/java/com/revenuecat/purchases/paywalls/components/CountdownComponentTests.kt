package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(Enclosed::class)
internal class CountdownComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeCountdownComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            public val json: String,
            public val hasEndStack: Boolean,
            public val hasFallback: Boolean,
        )

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "simple_countdown",
                    Args(
                        json = """
                            {
                              "type": "countdown",
                              "style": {
                                "type": "date",
                                "date": "2025-11-20T02:06:12.634Z"
                              },
                              "countdown_stack": {
                                "type": "stack",
                                "components": [
                                  {
                                    "color": {
                                      "light": {
                                        "type": "alias",
                                        "value": "primary"
                                      }
                                    },
                                    "components": [],
                                    "id": "countdownText",
                                    "name": "Countdown Text",
                                    "text_lid": "countdown_text",
                                    "type": "text"
                                  }
                                ]
                              }
                            }
                        """.trimIndent(),
                        hasEndStack = false,
                        hasFallback = false,
                    ),
                ),
                arrayOf(
                    "countdown_with_end_stack",
                    Args(
                        json = """
                            {
                              "type": "countdown",
                              "style": {
                                "type": "date",
                                "date": "2025-11-20T02:06:12.634Z"
                              },
                              "countdown_stack": {
                                "type": "stack",
                                "components": []
                              },
                              "end_stack": {
                                "type": "stack",
                                "components": []
                              }
                            }
                        """.trimIndent(),
                        hasEndStack = true,
                        hasFallback = false,
                    ),
                ),
                arrayOf(
                    "countdown_with_fallback",
                    Args(
                        json = """
                            {
                              "type": "countdown",
                              "style": {
                                "type": "date",
                                "date": "2025-11-20T02:06:12.634Z"
                              },
                              "countdown_stack": {
                                "type": "stack",
                                "components": []
                              },
                              "fallback": {
                                "type": "stack",
                                "components": []
                              }
                            }
                        """.trimIndent(),
                        hasEndStack = false,
                        hasFallback = true,
                    ),
                ),
                arrayOf(
                    "countdown_with_end_and_fallback",
                    Args(
                        json = """
                            {
                              "type": "countdown",
                              "style": {
                                "type": "date",
                                "date": "2025-11-20T02:06:12.634Z"
                              },
                              "countdown_stack": {
                                "type": "stack",
                                "components": []
                              },
                              "end_stack": {
                                "type": "stack",
                                "components": []
                              },
                              "fallback": {
                                "type": "stack",
                                "components": []
                              }
                            }
                        """.trimIndent(),
                        hasEndStack = true,
                        hasFallback = true,
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize CountdownComponent as CountdownComponent`() {
            val actual = JsonTools.json.decodeFromString<CountdownComponent>(args.json)
            
            assertIs<CountdownComponent>(actual)
            assertNotNull(actual.style)
            assertEquals("date", actual.style.type)
            assertNotNull(actual.style.date)
            assertNotNull(actual.countdownStack)
            
            if (args.hasEndStack) {
                assertNotNull(actual.endStack)
            } else {
                assertNull(actual.endStack)
            }
            
            if (args.hasFallback) {
                assertNotNull(actual.fallback)
            } else {
                assertNull(actual.fallback)
            }
        }

        @Test
        fun `Should properly deserialize CountdownComponent as PaywallComponent`() {
            val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)
            
            assertIs<CountdownComponent>(actual)
            val countdown = actual as CountdownComponent
            assertNotNull(countdown.style)
            assertEquals("date", countdown.style.type)
            assertNotNull(countdown.style.date)
            assertNotNull(countdown.countdownStack)
            
            if (args.hasEndStack) {
                assertNotNull(countdown.endStack)
            } else {
                assertNull(countdown.endStack)
            }
            
            if (args.hasFallback) {
                assertNotNull(countdown.fallback)
            } else {
                assertNull(countdown.fallback)
            }
        }
    }
}
