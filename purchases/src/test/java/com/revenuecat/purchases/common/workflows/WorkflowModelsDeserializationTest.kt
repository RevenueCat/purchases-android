@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.StoreReplacementMode
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
}
