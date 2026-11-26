@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.events.WorkflowEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.FlowEvent
import com.revenuecat.purchases.ui.revenuecatui.FlowEvent.EventTypes
import com.revenuecat.purchases.ui.revenuecatui.FlowEvent.Keys

internal fun WorkflowEvent.toFlowEvent(workflow: PublishedWorkflow, localeIdentifier: String): FlowEvent {
    val rawProperties = buildMap {
        put(Keys.TIMESTAMP.name, creationData.date.time)
        put(Keys.TRACE_ID.name, traceId)
        put(Keys.LOCALE.name, localeIdentifier)
        putFlowContext(workflow, stepId)
        when (val event = this@toFlowEvent) {
            is WorkflowEvent.StepStarted -> {
                put(Keys.EVENT_TYPE.name, EventTypes.FLOW_STEP_STARTED)
                putIfNotNull(Keys.FROM_STEP_ID.name, event.fromStepId)
                putIfNotNull(Keys.ENTRY_REASON.name, event.entryReason)
                putStepDetails(event.isFirstStep, event.isLastStep, event.experiment)
            }
            is WorkflowEvent.StepCompleted -> {
                put(Keys.EVENT_TYPE.name, EventTypes.FLOW_STEP_COMPLETED)
                putIfNotNull(Keys.TO_STEP_ID.name, event.toStepId)
                putStepDetails(event.isFirstStep, event.isLastStep, event.experiment)
            }
            is WorkflowEvent.Close -> {
                put(Keys.EVENT_TYPE.name, EventTypes.FLOW_CLOSE)
                putStepDetails(event.isFirstStep, event.isLastStep, event.experiment)
            }
        }
    }
    return FlowEvent(rawProperties)
}

internal fun PaywallEvent.toFlowEvent(): FlowEvent? {
    val eventType = when (type) {
        PaywallEventType.IMPRESSION -> EventTypes.PAYWALL_IMPRESSION
        PaywallEventType.CLOSE -> EventTypes.PAYWALL_CLOSE
        else -> return null
    }
    val rawProperties = buildMap {
        putPaywallData(this@toFlowEvent)
        put(Keys.EVENT_TYPE.name, eventType)
        putIfNotNull(Keys.TRACE_ID.name, data.traceId)
        putIfNotNull(Keys.FLOW_ID.name, data.workflowId)
        putIfNotNull(Keys.STEP_ID.name, data.stepId)
    }
    return FlowEvent(rawProperties)
}

private fun MutableMap<String, Any>.putFlowContext(workflow: PublishedWorkflow, stepId: String) {
    put(Keys.FLOW_ID.name, workflow.id)
    put(Keys.FLOW_NAME.name, workflow.displayName)
    put(Keys.STEP_ID.name, stepId)
    val step = workflow.steps[stepId] ?: return
    put(Keys.STEP_TYPE.name, step.type)
    putIfNotNull(Keys.SCREEN_TYPE.name, step.stepScreenType?.takeIf { it.isNotEmpty() })
    putIfNotNull(Keys.SCREEN_NAME.name, step.screenId?.let(workflow.screens::get)?.name)
}

private fun MutableMap<String, Any>.putStepDetails(
    isFirstStep: Boolean?,
    isLastStep: Boolean?,
    experiment: WorkflowEvent.ExperimentData?,
) {
    putIfNotNull(Keys.IS_FIRST_STEP.name, isFirstStep)
    putIfNotNull(Keys.IS_LAST_STEP.name, isLastStep)
    putIfNotNull(Keys.EXPERIMENT_ID.name, experiment?.experimentId)
    putIfNotNull(Keys.EXPERIMENT_VARIANT.name, experiment?.experimentVariant)
}
