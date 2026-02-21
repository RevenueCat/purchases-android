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
                                "conditions": [ { "type": "intro_offer" } ],
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
                                "conditions": [ { "type": "expanded" } ],
                                "properties": {
                                  "font_name": "expanded font"
                                }
                              },
                              {
                                "conditions": [ { "type": "medium" } ],
                                "properties": {
                                  "font_name": "medium font"
                                }
                              },
                              {
                                "conditions": [ { "type": "compact" } ],
                                "properties": {
                                  "font_name": "compact font"
                                }
                              },
                              {
                                "conditions": [ { "type": "selected" }, { "type": "intro_offer" } ],
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
                              }
                            ]
                        """.trimIndent(),
                        expected = listOf(
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.IntroOffer()),
                                properties = PartialTextComponent(fontName = FontAlias("intro font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.MultiplePhaseOffers),
                                properties = PartialTextComponent(fontName = FontAlias("multiple intros font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Selected),
                                properties = PartialTextComponent(fontName = FontAlias("selected font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Expanded),
                                properties = PartialTextComponent(fontName = FontAlias("expanded font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Medium),
                                properties = PartialTextComponent(fontName = FontAlias("medium font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Compact),
                                properties = PartialTextComponent(fontName = FontAlias("compact font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.Selected,
                                    ComponentOverride.Condition.IntroOffer(),
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
                            )
                        )
                    )
                ),
                arrayOf(
                    "new V0 condition types",
                    Args(
                        json = """
                            [
                              {
                                "conditions": [
                                  { "type": "selected_package", "operator": "in", "packages": ["pkg_a"] }
                                ],
                                "properties": { "font_name": "selected package font" }
                              },
                              {
                                "conditions": [
                                  { "type": "variable", "operator": "=", "variable": "theme", "value": "dark" }
                                ],
                                "properties": { "font_name": "variable font" }
                              },
                              {
                                "conditions": [
                                  { "type": "intro_offer", "operator": "=", "value": true }
                                ],
                                "properties": { "font_name": "intro offer with operator font" }
                              }
                            ]
                        """.trimIndent(),
                        expected = listOf(
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.SelectedPackage(
                                        operator = ComponentOverride.ArrayOperator.IN,
                                        packages = listOf("pkg_a"),
                                    ),
                                ),
                                properties = PartialTextComponent(
                                    fontName = FontAlias("selected package font"),
                                ),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.Variable(
                                        operator = ComponentOverride.EqualityOperator.EQUALS,
                                        variable = "theme",
                                        value = ComponentOverride.ConditionValue.StringValue("dark"),
                                    ),
                                ),
                                properties = PartialTextComponent(fontName = FontAlias("variable font")),
                            ),
                            ComponentOverride(
                                conditions = listOf(
                                    ComponentOverride.Condition.IntroOffer(
                                        operator = ComponentOverride.EqualityOperator.EQUALS,
                                        value = true,
                                    ),
                                ),
                                properties = PartialTextComponent(
                                    fontName = FontAlias("intro offer with operator font"),
                                ),
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
                            "conditions": [ { "type": "intro_offer" } ],
                            "properties": { "override_source_lid": "intro" }
                          },
                          {
                            "conditions": [ { "type": "multiple_intro_offers" } ],
                            "properties": { "override_source_lid": "multiple_intros" }
                          },
                          {
                            "conditions": [ { "type": "selected" } ],
                            "properties": { "override_source_lid": "selected" }
                          },
                          {
                            "conditions": [ { "type": "compact" } ],
                            "properties": { "override_source_lid": "compact" }
                          },
                          {
                            "conditions": [ { "type": "medium" } ],
                            "properties": { "override_source_lid": "medium" }
                          },
                          {
                            "conditions": [ { "type": "expanded" } ],
                            "properties": { "override_source_lid": "expanded" }
                          }
                        ]
                        """.trimIndent(),
                        expected = listOf(
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.IntroOffer()),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("intro")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.MultiplePhaseOffers),
                                properties = PartialImageComponent(
                                    overrideSourceLid = LocalizationKey("multiple_intros")
                                ),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Selected),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("selected")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Compact),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("compact")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Medium),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("medium")),
                            ),
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Expanded),
                                properties = PartialImageComponent(overrideSourceLid = LocalizationKey("expanded")),
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
                arrayOf("{ \"type\": \"compact\" }", ComponentOverride.Condition.Compact),
                arrayOf("{ \"type\": \"medium\" }", ComponentOverride.Condition.Medium),
                arrayOf("{ \"type\": \"expanded\" }", ComponentOverride.Condition.Expanded),
                arrayOf("{ \"type\": \"intro_offer\" }", ComponentOverride.Condition.IntroOffer()),
                arrayOf("{ \"type\": \"multiple_intro_offers\" }", ComponentOverride.Condition.MultiplePhaseOffers),
                arrayOf("{ \"type\": \"selected\" }", ComponentOverride.Condition.Selected),
                arrayOf("{ \"type\": \"promo_offer\" }", ComponentOverride.Condition.PromoOffer()),
                arrayOf("{ \"type\": \"unsupported\" }", ComponentOverride.Condition.Unsupported),
                arrayOf("{ \"type\": \"some_future_unknown_value\" }", ComponentOverride.Condition.Unsupported),

                // IntroOffer with operator and value
                arrayOf(
                    """{ "type": "intro_offer", "operator": "=", "value": true }""",
                    ComponentOverride.Condition.IntroOffer(
                        operator = ComponentOverride.EqualityOperator.EQUALS,
                        value = true,
                    ),
                ),
                arrayOf(
                    """{ "type": "intro_offer", "operator": "!=", "value": false }""",
                    ComponentOverride.Condition.IntroOffer(
                        operator = ComponentOverride.EqualityOperator.NOT_EQUALS,
                        value = false,
                    ),
                ),

                // PromoOffer with operator and value
                arrayOf(
                    """{ "type": "promo_offer", "operator": "=", "value": true }""",
                    ComponentOverride.Condition.PromoOffer(
                        operator = ComponentOverride.EqualityOperator.EQUALS,
                        value = true,
                    ),
                ),

                // SelectedPackage
                arrayOf(
                    """{ "type": "selected_package", "operator": "in", "packages": ["pkg_a", "pkg_b"] }""",
                    ComponentOverride.Condition.SelectedPackage(
                        operator = ComponentOverride.ArrayOperator.IN,
                        packages = listOf("pkg_a", "pkg_b"),
                    ),
                ),
                arrayOf(
                    """{ "type": "selected_package", "operator": "not in", "packages": ["pkg_c"] }""",
                    ComponentOverride.Condition.SelectedPackage(
                        operator = ComponentOverride.ArrayOperator.NOT_IN,
                        packages = listOf("pkg_c"),
                    ),
                ),

                // Variable with string value
                arrayOf(
                    """{ "type": "variable", "operator": "=", "variable": "plan_type", "value": "premium" }""",
                    ComponentOverride.Condition.Variable(
                        operator = ComponentOverride.EqualityOperator.EQUALS,
                        variable = "plan_type",
                        value = ComponentOverride.ConditionValue.StringValue("premium"),
                    ),
                ),
                // Variable with int value
                arrayOf(
                    """{ "type": "variable", "operator": "!=", "variable": "level", "value": 5 }""",
                    ComponentOverride.Condition.Variable(
                        operator = ComponentOverride.EqualityOperator.NOT_EQUALS,
                        variable = "level",
                        value = ComponentOverride.ConditionValue.IntValue(5),
                    ),
                ),
                // Variable with double value
                arrayOf(
                    """{ "type": "variable", "operator": "=", "variable": "score", "value": 9.5 }""",
                    ComponentOverride.Condition.Variable(
                        operator = ComponentOverride.EqualityOperator.EQUALS,
                        variable = "score",
                        value = ComponentOverride.ConditionValue.DoubleValue(9.5),
                    ),
                ),
                // Variable with boolean value
                arrayOf(
                    """{ "type": "variable", "operator": "=", "variable": "is_vip", "value": true }""",
                    ComponentOverride.Condition.Variable(
                        operator = ComponentOverride.EqualityOperator.EQUALS,
                        variable = "is_vip",
                        value = ComponentOverride.ConditionValue.BoolValue(true),
                    ),
                ),

                // Known type with unknown operator falls back to Unsupported
                arrayOf(
                    """{ "type": "selected_package", "operator": "contains", "packages": ["a"] }""",
                    ComponentOverride.Condition.Unsupported,
                ),
                arrayOf(
                    """{ "type": "variable", "operator": ">", "variable": "x", "value": 1 }""",
                    ComponentOverride.Condition.Unsupported,
                ),
                // Known type with missing required fields falls back to Unsupported
                arrayOf(
                    """{ "type": "selected_package" }""",
                    ComponentOverride.Condition.Unsupported,
                ),
                arrayOf(
                    """{ "type": "variable", "operator": "=" }""",
                    ComponentOverride.Condition.Unsupported,
                ),

                // Known type with changed field types falls back to Unsupported
                arrayOf(
                    """{ "type": "selected_package", "operator": "in", "packages": "not_an_array" }""",
                    ComponentOverride.Condition.Unsupported,
                ),
                arrayOf(
                    """{ "type": "variable", "operator": "=", "variable": "x", "value": [1, 2] }""",
                    ComponentOverride.Condition.Unsupported,
                ),
                arrayOf(
                    """{ "type": "variable", "operator": "=", "variable": "x", "value": {"nested": true} }""",
                    ComponentOverride.Condition.Unsupported,
                ),

                // Completely unexpected JSON shape falls back to Unsupported
                arrayOf(
                    """{ "no_type_field": true }""",
                    ComponentOverride.Condition.Unsupported,
                ),
                arrayOf(
                    """{}""",
                    ComponentOverride.Condition.Unsupported,
                ),
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
