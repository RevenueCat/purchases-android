@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import java.util.UUID

class WorkflowEventTest {

    @Test
    fun `StepStarted carries workflow, step, trace, and optional metadata`() {
        val id = UUID.randomUUID()
        val date = Date()
        val event = WorkflowEvent.StepStarted(
            creationData = WorkflowEvent.CreationData(id = id, date = date),
            workflowId = "wfl_abc",
            stepId = "step-1",
            traceId = "trace-1",
            fromStepId = null,
            entryReason = "start",
            isFirstStep = true,
            isLastStep = false,
        )

        assertThat(event.workflowId).isEqualTo("wfl_abc")
        assertThat(event.stepId).isEqualTo("step-1")
        assertThat(event.traceId).isEqualTo("trace-1")
        assertThat(event.entryReason).isEqualTo("start")
        assertThat(event.isFirstStep).isTrue
        assertThat(event.isPriorityEvent).isFalse
    }

    @Test
    fun `StepCompleted carries toStepId for forward nav and null for dismiss`() {
        val forward = WorkflowEvent.StepCompleted(
            creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
            workflowId = "wfl_abc",
            stepId = "step-1",
            traceId = "trace-1",
            toStepId = "step-2",
            isFirstStep = true,
            isLastStep = false,
        )
        val dismiss = forward.copy(toStepId = null)

        assertThat(forward.toStepId).isEqualTo("step-2")
        assertThat(dismiss.toStepId).isNull()
    }

    @Test
    fun `StepStarted converts to BackendStoredEvent_Workflows with matching properties`() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val date = Date(1717000000000L)
        val event = WorkflowEvent.StepStarted(
            creationData = WorkflowEvent.CreationData(id, date),
            workflowId = "wfl_abc",
            stepId = "step-1",
            traceId = "trace-1",
            fromStepId = null,
            entryReason = "start",
            isFirstStep = true,
            isLastStep = false,
        )

        val stored = event.toBackendStoredEvent("user_42")
        assertThat(stored).isInstanceOf(BackendStoredEvent.Workflows::class.java)
        val backend = (stored as BackendStoredEvent.Workflows).event
        assertThat(backend.id).isEqualTo(id.toString())
        assertThat(backend.eventName).isEqualTo("workflows_step_started")
        assertThat(backend.timestampMs).isEqualTo(date.time)
        assertThat(backend.appUserID).isEqualTo("user_42")
        assertThat(backend.properties.workflowId).isEqualTo("wfl_abc")
        assertThat(backend.properties.stepId).isEqualTo("step-1")
        assertThat(backend.properties.traceId).isEqualTo("trace-1")
        assertThat(backend.properties.entryReason).isEqualTo("start")
        assertThat(backend.properties.isFirstStep).isTrue
    }

    @Test
    fun `StepCompleted converts with toStepId in properties`() {
        val event = WorkflowEvent.StepCompleted(
            creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
            workflowId = "wfl_abc",
            stepId = "step-1",
            traceId = "trace-1",
            toStepId = "step-2",
            isFirstStep = true,
            isLastStep = false,
        )

        val stored = event.toBackendStoredEvent("user_42") as BackendStoredEvent.Workflows
        assertThat(stored.event.eventName).isEqualTo("workflows_step_completed")
        assertThat(stored.event.properties.toStepId).isEqualTo("step-2")
        assertThat(stored.event.properties.fromStepId).isNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `BackendStoredEvent_Workflows round-trips through EventsManager file storage JSON`() {
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendStoredEvent::class) {
                    subclass(BackendStoredEvent.Workflows::class, BackendStoredEvent.Workflows.serializer())
                }
            }
            explicitNulls = false
        }
        val original = BackendStoredEvent.Workflows(
            BackendEvent.Workflows(
                id = "evt_id",
                eventName = "workflows_step_started",
                timestampMs = 1L,
                appUserID = "u",
                properties = BackendEvent.Workflows.Properties(
                    workflowId = "wfl",
                    stepId = "s1",
                    traceId = "t",
                ),
            ),
        )
        val encoded = json.encodeToString(BackendStoredEvent.serializer(), original as BackendStoredEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), encoded)

        assertThat(decoded).isEqualTo(original)
    }
}
