package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ComponentOverridesTests {

    // This tests deserialization of ComponentOverrides containing PartialTextComponent and PartialImageComponent, just
    // to make sure deserialization of generics works as expected.

    @RunWith(Parameterized::class)
    class ComponentOverridesPartialTextComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ComponentOverrides<PartialTextComponent>,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all values present",
                    Args(
                        json = """
                        {
                          "intro_offer": {
                            "font_name": "intro font"
                          },
                          "multiple_intro_offers": {
                            "font_name": "multiple intros font"
                          },
                          "states": {
                            "selected": {
                              "font_name": "selected font"
                            }
                          },
                          "conditions": {
                            "compact": {
                              "font_name": "compact font"
                            },
                            "medium": {
                              "font_name": "medium font"
                            },
                            "expanded": {
                              "font_name": "expanded font"
                            }
                          }
                        }
                        """.trimIndent(),
                        expected = ComponentOverrides(
                            introOffer = PartialTextComponent(fontName = FontAlias("intro font")),
                            multipleIntroOffers = PartialTextComponent(fontName = FontAlias("multiple intros font")),
                            states = ComponentStates(
                                selected = PartialTextComponent(fontName = FontAlias("selected font"))
                            ),
                            conditions = ComponentConditions(
                                compact = PartialTextComponent(fontName = FontAlias("compact font")),
                                medium = PartialTextComponent(fontName = FontAlias("medium font")),
                                expanded = PartialTextComponent(fontName = FontAlias("expanded font")),
                            )
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = ComponentOverrides()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ComponentOverrides containing PartialTextComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ComponentOverrides<PartialTextComponent>>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class ComponentOverridesPartialImageComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ComponentOverrides<PartialImageComponent>,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all values present",
                    Args(
                        json = """
                        {
                          "intro_offer": {
                            "override_source_lid": "intro"
                          },
                          "multiple_intro_offers": {
                            "override_source_lid": "multiple_intros"
                          },
                          "states": {
                            "selected": {
                              "override_source_lid": "selected"
                            }
                          },
                          "conditions": {
                            "compact": {
                              "override_source_lid": "compact"
                            },
                            "medium": {
                              "override_source_lid": "medium"
                            },
                            "expanded": {
                              "override_source_lid": "expanded"
                            }
                          }
                        }
                        """.trimIndent(),
                        expected = ComponentOverrides(
                            introOffer = PartialImageComponent(overrideSourceLid = LocalizationKey("intro")),
                            multipleIntroOffers = PartialImageComponent(
                                overrideSourceLid = LocalizationKey("multiple_intros")
                            ),
                            states = ComponentStates(
                                selected = PartialImageComponent(overrideSourceLid = LocalizationKey("selected"))
                            ),
                            conditions = ComponentConditions(
                                compact = PartialImageComponent(overrideSourceLid = LocalizationKey("compact")),
                                medium = PartialImageComponent(overrideSourceLid = LocalizationKey("medium")),
                                expanded = PartialImageComponent(overrideSourceLid = LocalizationKey("expanded")),
                            )
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = ComponentOverrides()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ComponentOverrides containing PartialImageComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ComponentOverrides<PartialImageComponent>>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
