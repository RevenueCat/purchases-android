package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(Enclosed::class)
internal class ComponentOverridesTests {

    // This tests deserialization of ComponentOverrides containing PartialTextComponent and PartialImageComponent, just
    // to make sure deserialization of generics works as expected.

    @RunWith(ParameterizedRobolectricTestRunner::class)
    class ComponentOverridesPartialTextComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: List<ComponentOverride<PartialTextComponent>>,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all conditions present",
                    Args(
                        json = """
                            [
                              {
                                "conditions": [ { "type": "introductory_offer", "operator": "=", "value": true } ],
                                "properties": {
                                  "font_name": "intro font"
                                }
                              },
                              {
                                "conditions": [ { "type": "multiple_intro_offers" } ],
                                "properties": {
                                  "font_name": "multiple intros font"
                                }
                              },
                              {
                                "conditions": [ { "type": "selected" } ],
                                "properties": {
                                  "font_name": "selected font"
                                }
                              },
                              {
                                "conditions": [ { "type": "selected" }, { "type": "introductory_offer", "operator": "=", "value": true } ],
                                "properties": {
                                  "font_name": "compact font"
                                }
                              },
                              {
                                "conditions": [ { "type": "unknown" } ],
                                "properties": {
                                  "font_name": "unknown condition font"
                                }
                              },
                              {
                                "conditions": [ { "type": "selected", "other_property": "value" } ],
                                "properties": {
                                  "font_name": "condition with other unknown property"
                                }
                              },
                              {
                                "conditions": [
                                  {
                                    "type": "orientation",
                                    "operator": "in",
                                    "orientations": ["portrait"]
                                  }
                                ],
                                "properties": {
                                  "font_name": "orientation font"
                                }
                              },
                              {
                                "conditions": [
                                  {
                                    "type": "screen_size",
                                    "operator": "not_in",
                                    "sizes": ["tablet", "desktop"]
                                  }
                                ],
                                "properties": {
                                  "font_name": "screen size font"
                                }
                              },
                              {
                                "conditions": [
                                  {
                                    "type": "selected_package",
                                    "operator": "in",
                                    "packages": ["rc_annual"]
                                  }
                                ],
                                "properties": {
                                  "font_name": "selected package font"
                                }
                              }
                            ]
                        """.trimIndent(),
                        expected = listOf(
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                )),
                                properties = PartialTextComponent(fontName = FontAlias("intro font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.MultipleIntroOffers),
                                properties = PartialTextComponent(fontName = FontAlias("multiple intros font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Selected),
                                properties = PartialTextComponent(fontName = FontAlias("selected font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.Selected,
                                    ComponentOverride.Condition.IntroOffer(
                                        operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                        value = true,
                                    ),
                                ),
                                properties = PartialTextComponent(fontName = FontAlias("compact font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Unsupported),
                                properties = PartialTextComponent(fontName = FontAlias("unknown condition font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Selected),
                                properties = PartialTextComponent(
                                    fontName = FontAlias("condition with other unknown property"),
                                ),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.Orientation(
                                        operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                        orientations = listOf(ComponentOverride.Condition.OrientationType.PORTRAIT),
                                    )
                                ),
                                properties = PartialTextComponent(fontName = FontAlias("orientation font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.ScreenSize(
                                        operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                        sizes = listOf("tablet", "desktop"),
                                    )
                                ),
                                properties = PartialTextComponent(fontName = FontAlias("screen size font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.SelectedPackage(
                                        operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                        packages = listOf("rc_annual"),
                                    )
                                ),
                                properties = PartialTextComponent(fontName = FontAlias("selected package font")),
                            ),
                        )
                    )
                ),
                arrayOf(
                    "no overrides",
                    Args(
                        json = """
                        []
                        """.trimIndent(),
                        expected = emptyList()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ComponentOverrides containing PartialTextComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<List<ComponentOverride<PartialTextComponent>>>(args.json)

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
            val expected: List<ComponentOverride<PartialImageComponent>>,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all conditions present",
                    Args(
                        json = """
                        [
                          {
                            "conditions": [ { "type": "introductory_offer", "operator": "=", "value": true } ],
                            "properties": { "override_source_lid": "intro" }
                          },
                          {
                            "conditions": [ { "type": "multiple_intro_offers" } ],
                            "properties": { "override_source_lid": "multiple_intros" }
                          },
                          {
                            "conditions": [ { "type": "selected" } ],
                            "properties": { "override_source_lid": "selected" }
                          }
                        ]
                        """.trimIndent(),
                        expected = listOf(
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                )),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("intro")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.MultipleIntroOffers),
                                properties = PartialImageComponent(
                                    overrideSourceLid = LocalizationKey("multiple_intros")
                                ),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Selected),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("selected")),
                            ),
                        )
                    )
                ),
                arrayOf(
                    "no overrides",
                    Args(
                        json = """
                        []
                        """.trimIndent(),
                        expected = emptyList()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ComponentOverrides containing PartialImageComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<List<ComponentOverride<PartialImageComponent>>>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializeComponentOverrideConditionTests(
        private val serialized: String,
        private val expected: ComponentOverride.Condition,
    ) {

        companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf("{ \"type\": \"introductory_offer\", \"operator\": \"=\", \"value\": true }", ComponentOverride.Condition.IntroOffer(
                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                    value = true,
                )),
                arrayOf("{ \"type\": \"introductory_offer\", \"operator\": \"=\", \"value\": false }", ComponentOverride.Condition.IntroOffer(
                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                    value = false,
                )),
                arrayOf("{ \"type\": \"introductory_offer\", \"operator\": \"!=\", \"value\": true }", ComponentOverride.Condition.IntroOffer(
                    operator = ComponentOverride.Condition.EqualityOperatorType.NOT_EQUALS,
                    value = true,
                )),
                arrayOf("{ \"type\": \"introductory_offer\", \"operator\": \"!=\", \"value\": false }", ComponentOverride.Condition.IntroOffer(
                    operator = ComponentOverride.Condition.EqualityOperatorType.NOT_EQUALS,
                    value = false,
                )),
                arrayOf("{ \"type\": \"multiple_intro_offers\" }", ComponentOverride.Condition.MultipleIntroOffers),
                arrayOf("{ \"type\": \"selected\" }", ComponentOverride.Condition.Selected),
                arrayOf("{ \"type\": \"unsupported\" }", ComponentOverride.Condition.Unsupported),
                arrayOf("{ \"type\": \"some_future_unknown_value\" }", ComponentOverride.Condition.Unsupported),
            )
        }

        @Test
        fun `Should properly deserialize Condition`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ComponentOverride.Condition>(serialized)

            // Assert
            assert(actual == expected)
        }
    }
}
