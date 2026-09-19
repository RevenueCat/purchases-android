@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Screens in the order a user reaches them.
 * Unreachable screens come last, not dropped: [WorkflowTriggerAction] has an `Unknown` variant and
 * [WorkflowStep.type] is an open, optional string, so a navigation this version cannot read must not stop a real
 * page from warming.
 */
internal fun PublishedWorkflow.screensInVisitOrder(): List<WorkflowScreen> {
    val roots = setOfNotNull(initialStepId, singleStepFallbackId)
    val seenSteps = roots.toMutableSet()
    val frontier = ArrayDeque(roots)
    val screenIds = linkedSetOf<String>()
    while (frontier.isNotEmpty()) {
        val step = steps[frontier.removeFirst()] ?: continue
        step.screenId?.let(screenIds::add)
        step.nextStepIds().forEach { stepId -> if (seenSteps.add(stepId)) frontier.addLast(stepId) }
    }
    return (screenIds + screens.keys).mapNotNull { screenId -> screens[screenId] }
}

private fun WorkflowStep.nextStepIds(): List<String> =
    (triggers.map { trigger -> trigger.actionId } + triggerActions.keys)
        .mapNotNull { actionId -> (triggerActions[actionId] as? WorkflowTriggerAction.Step)?.stepId }
