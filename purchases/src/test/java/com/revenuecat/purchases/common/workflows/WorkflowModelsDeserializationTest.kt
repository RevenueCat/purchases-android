@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

internal class WorkflowModelsDeserializationTest {

    @Test
    fun `WorkflowStep stepScreenType reads paywall from metadata`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": ["paywall"]}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).containsExactly("paywall")
    }

    @Test
    fun `WorkflowStep isOfferingStep is true only for offering steps`() {
        val offeringStep = JsonTools.json.decodeFromString(
            WorkflowStep.serializer(),
            """{"id": "step_1", "type": "offering"}""",
        )
        val screenStep = JsonTools.json.decodeFromString(
            WorkflowStep.serializer(),
            """{"id": "step_2", "type": "screen", "screen_id": "screen_1"}""",
        )
        assertThat(offeringStep.isOfferingStep).isTrue
        assertThat(screenStep.isOfferingStep).isFalse
    }

    @Test
    fun `WorkflowStep stepScreenType is empty when tagged with empty array`() {
        // A step the backend tagged with no known type. Empty (not null) means "explicitly not a
        // paywall", which suppresses paywall events.
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": []}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isEmpty()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when screen_type key absent`() {
        // Older workflows omit screen_type. Null (not empty) preserves the always-report behavior.
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"other_key": "value"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when metadata is null`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": null}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep experiment params read experiment_id and experiment_variant from param_values`() {
        val json = """
            {"id": "step_1", "type": "screen",
             "param_values": {"experiment_id": "exp_abc", "experiment_variant": "b", "other": 1}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.experimentId).isEqualTo("exp_abc")
        assertThat(step.experimentVariant).isEqualTo("b")
    }

    @Test
    fun `WorkflowStep experiment params are null when absent`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering_identifier": "premium"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.experimentId).isNull()
        assertThat(step.experimentVariant).isNull()
    }

    @Test
    fun `WorkflowStep experiment params are read independently`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"experiment_id": "exp_abc"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.experimentId).isEqualTo("exp_abc")
        assertThat(step.experimentVariant).isNull()
    }

    @Test
    fun `WorkflowStep experiment params are null when not strings`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"experiment_id": 12, "experiment_variant": null}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.experimentId).isNull()
        assertThat(step.experimentVariant).isNull()
    }

    @Test
    fun `WorkflowStep experiment params are read from metadata`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"experiment_id": "exp_abc", "experiment_variant": "holdout"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.experimentId).isEqualTo("exp_abc")
        assertThat(step.experimentVariant).isEqualTo("holdout")
    }

    @Test
    fun `WorkflowStep experiment params prefer metadata over param_values`() {
        val json = """
            {
              "id": "step_1",
              "type": "screen",
              "param_values": {"experiment_id": "exp_old", "experiment_variant": "a"},
              "metadata": {"experiment_id": "exp_new", "experiment_variant": "b"}
            }
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.experimentId).isEqualTo("exp_new")
        assertThat(step.experimentVariant).isEqualTo("b")
    }

    @Test
    fun `PublishedWorkflow decodes a fallback copy step`() {
        val json = """
            {
              "id": "wf_test",
              "display_name": "Test",
              "initial_step_id": "entry",
              "steps": {
                "entry": {
                  "id": "entry",
                  "type": "screen",
                  "trigger_actions": {"btn": {"type": "step", "step_id": "paywall_a~f"}}
                },
                "paywall_a": {
                  "id": "paywall_a",
                  "type": "screen",
                  "screen_id": "pw_123",
                  "param_values": {"experiment_id": "exp_abc", "experiment_variant": "b"},
                  "metadata": {"screen_type": ["paywall"]}
                },
                "paywall_a~f": {
                  "id": "paywall_a~f",
                  "type": "screen",
                  "screen_id": "pw_123",
                  "param_values": {},
                  "metadata": {"screen_type": ["paywall"], "fallback_original_step_id": "paywall_a"}
                }
              },
              "screens": {}
            }
        """.trimIndent()
        val workflow = JsonTools.json.decodeFromString(PublishedWorkflow.serializer(), json)

        val original = workflow.steps.getValue("paywall_a")
        val copy = workflow.steps.getValue("paywall_a~f")
        assertThat(workflow.steps.getValue("entry").triggerActions["btn"])
            .isEqualTo(WorkflowTriggerAction.Step(stepId = "paywall_a~f"))
        assertThat(copy.id).isEqualTo("paywall_a~f")
        assertThat(copy.screenId).isEqualTo(original.screenId)
        assertThat(copy.fallbackOriginalStepId).isEqualTo("paywall_a")
        assertThat(copy.experimentId).isNull()
        assertThat(copy.stepScreenType).containsExactly("paywall")
        assertThat(original.fallbackOriginalStepId).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when metadata is absent`() {
        val json = """
            {"id": "step_1", "type": "screen"}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType ignores non-string entries`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": ["paywall", 1, null]}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).containsExactly("paywall")
    }

    // A present-but-non-array `screen_type` is treated as untagged (null), matching iOS. The backend
    // only ships `screen_type` as a JSON array; these pin the conservative fallback for malformed shapes.

    @Test
    fun `WorkflowStep stepScreenType is null when screen_type is a scalar`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": "paywall"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when screen_type is an object`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": {"value": "paywall"}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier reads param_values offering identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {"identifier": "default"}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isEqualTo("default")
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null when param_values lacks offering`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"other": "value"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null when offering is null`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": null}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null when offering is not an object`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": "default"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null when offering has no identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a null identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {"identifier": null}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a non-string identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {"identifier": 42}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a blank identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {"identifier": "  "}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier falls back to the flat offering_identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering_identifier": "default"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isEqualTo("default")
    }

    @Test
    fun `WorkflowStep offeringIdentifier prefers the nested offering over the flat offering_identifier`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {"identifier": "nested"}, "offering_identifier": "flat"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isEqualTo("nested")
    }

    @Test
    fun `WorkflowStep offeringIdentifier falls back to the flat offering_identifier when the nested one is invalid`() {
        val json = """
            {"id": "step_1", "type": "screen", "param_values": {"offering": {"identifier": 42}, "offering_identifier": "flat"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a non-string or blank flat offering_identifier`() {
        listOf(
            """{"id": "step_1", "type": "screen", "param_values": {"offering_identifier": 42}}""",
            """{"id": "step_1", "type": "screen", "param_values": {"offering_identifier": null}}""",
            """{"id": "step_1", "type": "screen", "param_values": {"offering_identifier": "  "}}""",
        ).forEach { json ->
            val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
            assertThat(step.offeringIdentifier).describedAs(json).isNull()
        }
    }

    @Test
    fun `WorkflowScreen reads state_declarations`() {
        val json = """
            {
              "template_name": "components",
              "asset_base_url": "https://assets.pawwalls.com",
              "components_config": {
                "base": {
                  "stack": {"type": "stack", "components": []},
                  "background": {"type": "color", "value": {"light": {"type": "hex", "value": "#ffffff"}}}
                }
              },
              "components_localizations": {"en_US": {}},
              "default_locale": "en_US",
              "state_declarations": {"selected_tab": {"type": "string", "default": "monthly"}}
            }
        """.trimIndent()
        val screen = JsonTools.json.decodeFromString(WorkflowScreen.serializer(), json)

        val declaration = screen.stateDeclarations?.get("selected_tab")
        assertThat(declaration?.type).isEqualTo(StateDeclaration.ValueType.STRING)
        assertThat(declaration?.defaultValue?.content).isEqualTo("monthly")
    }

    @Test
    fun `WorkflowScreen reads automatically_scale_font_size`() {
        val json = """
            {
              "template_name": "components",
              "asset_base_url": "https://assets.pawwalls.com",
              "components_config": {
                "base": {
                  "stack": {"type": "stack", "components": []},
                  "background": {"type": "color", "value": {"light": {"type": "hex", "value": "#ffffff"}}}
                }
              },
              "components_localizations": {"en_US": {}},
              "default_locale": "en_US",
              "automatically_scale_font_size": false
            }
        """.trimIndent()

        val screen = JsonTools.json.decodeFromString(WorkflowScreen.serializer(), json)

        assertThat(screen.automaticallyScaleFontSize).isFalse()
    }

    @Test
    fun `WorkflowScreen reads play_store_product_change_mode`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(
                productChangeConfig = """
                    {
                      "upgrade_replacement_mode": "charge_full_price",
                      "downgrade_replacement_mode": "deferred"
                    }
                """.trimIndent(),
            ),
        )

        assertThat(screen.productChangeConfig?.upgradeReplacementMode)
            .isEqualTo(StoreReplacementMode.CHARGE_FULL_PRICE)
        assertThat(screen.productChangeConfig?.downgradeReplacementMode)
            .isEqualTo(StoreReplacementMode.DEFERRED)
    }

    @Test
    fun `WorkflowScreen treats empty play_store_product_change_mode as absent`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(productChangeConfig = "{}"),
        )

        assertThat(screen.productChangeConfig).isNull()
    }

    @Test
    fun `WorkflowScreen reads zero_decimal_place_countries`() {
        // The backend posts the field keyed by store; only the Google list applies here.
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(
                zeroDecimalPlaceCountries = "{\"apple\": [\"TWN\", \"MEX\"], \"google\": [\"TW\", \"MX\"]}",
            ),
        )

        assertThat(screen.zeroDecimalPlaceCountries).containsExactly("TW", "MX")
    }

    @Test
    fun `WorkflowScreen defaults zero_decimal_place_countries to empty when absent`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(),
        )

        assertThat(screen.zeroDecimalPlaceCountries).isEmpty()
    }

    @Test
    fun `WorkflowScreen reads components_video_localizations`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(
                componentsVideoLocalizations = """
                    {
                      "es_ES": {
                        "video_lid": {
                          "light": {"url": "https://video.pawwalls.com/es.mp4", "width": 1080, "height": 1920}
                        }
                      }
                    }
                """.trimIndent(),
            ),
        )

        assertThat(screen.componentsVideoLocalizations).isEqualTo(
            mapOf(
                LocaleId("es_ES") to mapOf(
                    LocalizationKey("video_lid") to ThemeVideoUrls(
                        light = VideoUrls(
                            width = 1080u,
                            height = 1920u,
                            url = URL("https://video.pawwalls.com/es.mp4"),
                        ),
                        dark = null,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `WorkflowScreen defaults components_video_localizations to empty when absent`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(),
        )

        assertThat(screen.componentsVideoLocalizations).isEmpty()
    }

    @Test
    fun `WorkflowScreen defaults components_video_localizations to empty when null`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            workflowScreenJson(componentsVideoLocalizations = "null"),
        )

        assertThat(screen.componentsVideoLocalizations).isEmpty()
    }

    private fun workflowScreenJson(
        productChangeConfig: String? = null,
        zeroDecimalPlaceCountries: String? = null,
        componentsVideoLocalizations: String? = null,
    ): String {
        val optionalFields = listOfNotNull(
            productChangeConfig?.let { "\"play_store_product_change_mode\": $it" },
            zeroDecimalPlaceCountries?.let { "\"zero_decimal_place_countries\": $it" },
            componentsVideoLocalizations?.let { "\"components_video_localizations\": $it" },
        ).joinToString(",\n")

        return """
            {
              "template_name": "components",
              "asset_base_url": "https://assets.pawwalls.com",
              "components_config": {
                "base": {
                  "stack": {"type": "stack", "components": []},
                  "background": {"type": "color", "value": {"light": {"type": "hex", "value": "#ffffff"}}}
                }
              },
              "components_localizations": {"en_US": {}},
              "default_locale": "en_US"${if (optionalFields.isEmpty()) "" else ",\n$optionalFields"}
            }
        """.trimIndent()
    }

    // region default_locale

    private fun screenJson(defaultLocaleFragment: String) = """
        {
          $defaultLocaleFragment
          "template_name": "tmpl",
          "asset_base_url": "https://assets.revenuecat.com",
          "components_localizations": {},
          "components_config": {
            "base": {
              "stack": {
                "type": "stack", "components": [],
                "dimension": { "type": "vertical", "alignment": "center", "distribution": "center" },
                "size": { "width": { "type": "fill" }, "height": { "type": "fill" } },
                "padding": { "top": 0, "bottom": 0, "leading": 0, "trailing": 0 },
                "margin": { "top": 0, "bottom": 0, "leading": 0, "trailing": 0 }
              },
              "background": { "type": "color", "value": { "light": { "type": "hex", "value": "#FFFFFF" } } }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `WorkflowScreen default_locale is preserved when present`() {
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            screenJson(""""default_locale": "es_ES","""),
        )
        assertThat(screen.defaultLocaleIdentifier).isEqualTo(LocaleId("es_ES"))
    }

    @Test
    fun `WorkflowScreen default_locale falls back to en when null`() {
        // The backend sends null for template-derived screens. Throwing here discards the whole
        // workflow, not just this field, taking every screen in it down with the paywall.
        val screen = JsonTools.json.decodeFromString(
            WorkflowScreen.serializer(),
            screenJson(""""default_locale": null,"""),
        )
        assertThat(screen.defaultLocaleIdentifier).isEqualTo(LocaleId("en"))
    }

    @Test
    fun `WorkflowScreen default_locale falls back to en for non-string values`() {
        // iOS only accepts a JSON string here, so coercing a number or bool into LocaleId("42")
        // would hand the renderer a locale the other platforms never produce.
        listOf("42", "1.5", "true", "{}", """{ "value": "es_ES" }""", """["es_ES"]""").forEach { value ->
            val screen = JsonTools.json.decodeFromString(
                WorkflowScreen.serializer(),
                screenJson(""""default_locale": $value,"""),
            )
            assertThat(screen.defaultLocaleIdentifier)
                .`as`("default_locale was coerced from: %s", value)
                .isEqualTo(LocaleId("en"))
        }
    }

    @Test
    fun `WorkflowScreen default_locale falls back to en when missing`() {
        val screen = JsonTools.json.decodeFromString(WorkflowScreen.serializer(), screenJson(""))
        assertThat(screen.defaultLocaleIdentifier).isEqualTo(LocaleId("en"))
    }

    @Test
    fun `workflow and offerings paths agree on default_locale`() {
        // Both models describe the same screen; the workflow path only re-wraps what /offerings
        // serves directly. They must not diverge on a shared field.
        listOf(
            """"default_locale": "es_ES",""",
            """"default_locale": null,""",
            """"default_locale": 42,""",
            """"default_locale": true,""",
            """"default_locale": {},""",
            "",
        ).forEach { fragment ->
            val json = screenJson(fragment)
            val fromWorkflow = JsonTools.json
                .decodeFromString(WorkflowScreen.serializer(), json).defaultLocaleIdentifier
            val fromOfferings = JsonTools.json
                .decodeFromString(PaywallComponentsData.serializer(), json).defaultLocaleIdentifier

            assertThat(fromWorkflow)
                .`as`("default_locale diverged for fragment: %s", fragment.ifEmpty { "<omitted>" })
                .isEqualTo(fromOfferings)
        }
    }

    // endregion
}
