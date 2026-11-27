@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.events.WorkflowEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import java.util.Date
import java.util.UUID

internal class FlowEventMapperTest {

    private val sessionId = UUID.randomUUID()

    private fun paywallEvent(type: PaywallEventType, paywallIdentifier: String? = "pw_123") = PaywallEvent(
        creationData = PaywallEvent.CreationData(id = UUID.randomUUID(), date = Date(1_700_000_000_000L)),
        data = PaywallEvent.Data(
            paywallIdentifier = paywallIdentifier,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "default"),
            paywallRevision = 7,
            sessionIdentifier = sessionId,
            displayMode = "full_screen",
            localeIdentifier = "en_US",
            darkMode = true,
        ),
        type = type,
    )

    @Test
    fun `impression outside a flow maps paywall keys only`() {
        val event = paywallEvent(PaywallEventType.IMPRESSION).toFlowEvent(workflow = null)

        assertThat(event?.rawProperties).containsOnly(
            entry("event_type", "paywall_impression"),
            entry("timestamp", 1_700_000_000_000L),
            entry("locale", "en_US"),
            entry("paywall_id", "pw_123"),
            entry("offering_id", "default"),
            entry("paywall_revision", 7),
            entry("session_id", sessionId.toString()),
            entry("display_mode", "full_screen"),
            entry("dark_mode", true),
        )
    }

    @Test
    fun `close maps to paywall_close and omits a missing paywall id`() {
        val event = paywallEvent(PaywallEventType.CLOSE, paywallIdentifier = null).toFlowEvent(workflow = null)

        assertThat(event?.rawProperties)
            .containsEntry("event_type", "paywall_close")
            .doesNotContainKey("paywall_id")
    }

    @Test
    fun `paywall events without a flow equivalent map to null`() {
        val unmapped = PaywallEventType.values().filterNot {
            it == PaywallEventType.IMPRESSION || it == PaywallEventType.CLOSE
        }

        assertThat(unmapped.map { paywallEvent(it).toFlowEvent(workflow = null) }).containsOnlyNulls()
    }

    private val offerScreen = mockk<WorkflowScreen> { every { name } returns "Offer" }

    private fun workflow(screenTypes: List<String>) = PublishedWorkflow(
        id = "wf_1",
        displayName = "Onboarding",
        initialStepId = "step_1",
        steps = mapOf(
            "step_2" to WorkflowStep(
                id = "step_2",
                type = "screen",
                screenId = "screen_offer",
                metadata = JsonObject(mapOf("screen_type" to JsonArray(screenTypes.map(::JsonPrimitive)))),
            ),
        ),
        screens = mapOf("screen_offer" to offerScreen),
    )

    @Test
    fun `step started maps flow context, navigation and experiment keys`() {
        val event = WorkflowEvent.StepStarted(
            creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date(1_700_000_000_000L)),
            workflowId = "wf_1",
            stepId = "step_2",
            traceId = "trace_1",
            fromStepId = "step_1",
            entryReason = "forward",
            isFirstStep = false,
            isLastStep = true,
            workflowBlobRef = "blob",
            fallbackOriginalStepId = "step_0",
            experiment = WorkflowEvent.ExperimentData(experimentId = "exp_1", experimentVariant = "b"),
        ).toFlowEvent(workflow(listOf("paywall")), localeIdentifier = "en-US")

        assertThat(event.rawProperties).containsOnly(
            entry("event_type", "flow_step_started"),
            entry("timestamp", 1_700_000_000_000L),
            entry("trace_id", "trace_1"),
            entry("locale", "en-US"),
            entry("flow_id", "wf_1"),
            entry("flow_name", "Onboarding"),
            entry("step_id", "step_2"),
            entry("step_type", "screen"),
            entry("screen_type", listOf("paywall")),
            entry("screen_name", "Offer"),
            entry("from_step_id", "step_1"),
            entry("entry_reason", "forward"),
            entry("is_first_step", false),
            entry("is_last_step", true),
            entry("experiment_id", "exp_1"),
            entry("experiment_variant", "b"),
        )
    }

    @Test
    fun `close omits an empty screen_type`() {
        val event = WorkflowEvent.Close(
            creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
            workflowId = "wf_1",
            stepId = "step_2",
            traceId = "trace_1",
        ).toFlowEvent(workflow(emptyList()), localeIdentifier = "en-US")

        assertThat(event.rawProperties)
            .containsEntry("event_type", "flow_close")
            .doesNotContainKeys("screen_type", "from_step_id", "to_step_id", "entry_reason")
    }
}
