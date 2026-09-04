@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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
    fun `WorkflowStep type is null when absent`() {
        // Untyped workflows ship steps without a type; the whole workflow must still parse.
        val json = """
            {"id": "step_1"}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.type).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier reads param_values`() {
        val json = """
            {"id": "step_1", "param_values": {"offering_identifier": "default"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isEqualTo("default")
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null when param_values lacks it`() {
        val json = """
            {"id": "step_1", "param_values": {"other": "value"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a null value`() {
        val json = """
            {"id": "step_1", "param_values": {"offering_identifier": null}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a non-string value`() {
        val json = """
            {"id": "step_1", "param_values": {"offering_identifier": 42}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
    }

    @Test
    fun `WorkflowStep offeringIdentifier is null for a blank value`() {
        val json = """
            {"id": "step_1", "param_values": {"offering_identifier": "  "}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.offeringIdentifier).isNull()
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

    private fun workflowScreenJson(
        productChangeConfig: String? = null,
        zeroDecimalPlaceCountries: String? = null,
    ): String {
        val optionalFields = listOfNotNull(
            productChangeConfig?.let { "\"play_store_product_change_mode\": $it" },
            zeroDecimalPlaceCountries?.let { "\"zero_decimal_place_countries\": $it" },
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
