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
    public class ComponentOverridesPartialTextComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        public class Args(
            @Language("json")
            val json: String,
            val expected: List<ComponentOverride<PartialTextComponent>>,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = listOf(
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
                                conditions = listOf(ComponentOverride.Condition.IntroOffer),
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
                                    ComponentOverride.Condition.IntroOffer,
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
    public class ComponentOverridesPartialImageComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        public class Args(
            @Language("json")
            val json: String,
            val expected: List<ComponentOverride<PartialImageComponent>>,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = listOf(
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
                                conditions = listOf(ComponentOverride.Condition.IntroOffer),
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
    public class DeserializeComponentOverrideConditionTests(
        private val serialized: String,
        private val expected: ComponentOverride.Condition,
    ) {

        companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = listOf(
                arrayOf("{ \"type\": \"compact\" }", ComponentOverride.Condition.Compact),
                arrayOf("{ \"type\": \"medium\" }", ComponentOverride.Condition.Medium),
                arrayOf("{ \"type\": \"expanded\" }", ComponentOverride.Condition.Expanded),
                arrayOf("{ \"type\": \"intro_offer\" }", ComponentOverride.Condition.IntroOffer),
                arrayOf("{ \"type\": \"multiple_intro_offers\" }", ComponentOverride.Condition.MultiplePhaseOffers),
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
