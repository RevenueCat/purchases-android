@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import kotlinx.serialization.descriptors.SerialDescriptor
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

    /**
     * A workflow screen and an offering's paywall are the same backend document, but they decode
     * through two independent field lists, so a field added to [PaywallComponentsData] alone is
     * silently dropped on the workflow path (this is how `state_declarations` went missing, which
     * left every state_condition override inert on funnel paywalls).
     *
     * Adding a field to [PaywallComponentsData] fails this test until it is either decoded by
     * [WorkflowScreen] (and passed through by `WorkflowScreenMapper`) or listed below with a reason.
     */
    @Test
    fun `WorkflowScreen decodes every PaywallComponentsData field`() {
        val notSentPerScreen = setOf(
            // Supplied by WorkflowScreenMapper from the screens map key, not by the screen body.
            "id",
            // Absent from the backend's per-screen payload (serialize_paywalls_as_screens).
            "zero_decimal_place_countries",
            "play_store_product_change_mode",
            // Sent per screen but not wired through yet: funnel paywalls always use the default.
            "automatically_scale_font_size",
        )

        val missing = PaywallComponentsData.serializer().descriptor.serialNames() -
            WorkflowScreen.serializer().descriptor.serialNames() -
            notSentPerScreen

        assertThat(missing).isEmpty()
    }

    private fun SerialDescriptor.serialNames(): Set<String> =
        (0 until elementsCount).map { getElementName(it) }.toSet()
}
