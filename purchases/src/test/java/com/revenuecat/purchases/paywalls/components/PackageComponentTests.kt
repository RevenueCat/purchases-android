package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PromoOfferConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PackageComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    private lateinit var previousLogHandler: LogHandler

    @Before
    public fun setUp() {
        previousLogHandler = currentLogHandler
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) = Unit
            override fun d(tag: String, msg: String) = Unit
            override fun i(tag: String, msg: String) = Unit
            override fun w(tag: String, msg: String) = Unit
            override fun e(tag: String, msg: String, throwable: Throwable?) = Unit
        }
    }

    @After
    public fun tearDown() {
        currentLogHandler = previousLogHandler
    }

    public class Args(
        @Language("json")
        public val json: String,
        val expected: PackageComponent,
    )

    public companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = listOf(
            arrayOf(
                "non-empty stack",
                Args(
                    json = """
                        {
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
                          "stack": {
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
                                "id": "xmpgCrN9Rb",
                                "name": "Text",
                                "text_lid": "7bkohQjzIE",
                                "type": "text"
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("7bkohQjzIE"),
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                )
                            ),
                        )
                    )
                ),
            ),
            arrayOf(
                "empty stack",
                Args(
                    json = """
                        {
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
                        stack = StackComponent(
                            components = emptyList(),
                        )
                    )
                ),
            ),
            arrayOf(
                "valid play_store_offer",
                Args(
                    json = """
                        {
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "play_store_offer": {
                            "offer_id": "my-offer"
                          }
                        }
                        """.trimIndent(),
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
                        stack = StackComponent(components = emptyList()),
                        playStoreOffer = PromoOfferConfig(offerId = "my-offer"),
                    )
                ),
            ),
            arrayOf(
                "malformed play_store_offer defaults to null",
                Args(
                    json = """
                        {
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "play_store_offer": {
                            "unexpected_field": 123
                          }
                        }
                        """.trimIndent(),
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
                        stack = StackComponent(components = emptyList()),
                        playStoreOffer = null,
                    )
                ),
            ),
            arrayOf(
                "play_store_offer with wrong type defaults to null",
                Args(
                    json = """
                        {
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "play_store_offer": "not-an-object"
                        }
                        """.trimIndent(),
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
                        stack = StackComponent(components = emptyList()),
                        playStoreOffer = null,
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PackageComponent as PackageComponent`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PackageComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }

    @Test
    fun `Should properly deserialize PackageComponent as PaywallComponent`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
