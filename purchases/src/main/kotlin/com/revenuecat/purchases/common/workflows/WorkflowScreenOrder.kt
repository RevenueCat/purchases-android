@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * The screens a customer reaches, breadth first from this workflow's entry steps. Warming gets cut short by a
 * paywall opening or a memory trim, so this order decides which pages are warm by then.
 *
 * Unreachable screens come last rather than being dropped: [WorkflowTriggerAction] has an `Unknown` variant
 * and [WorkflowStep.type] is an open string, so a navigation this SDK version cannot read must not silently
 * stop a page that really is shown from warming.
 *
 * [PublishedWorkflow.singleStepFallbackId] counts as an entry step: [PublishedWorkflow.dismissExitOffer]
 * reads it, and nothing guarantees triggers reach it.
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

/**
 * Triggers first, in the order the components firing them appear. An action no trigger references still
 * counts, so an entry point this SDK version does not model cannot hide a step.
 */
private fun WorkflowStep.nextStepIds(): List<String> =
    (triggers.map { trigger -> trigger.actionId } + triggerActions.keys)
        .mapNotNull { actionId -> (triggerActions[actionId] as? WorkflowTriggerAction.Step)?.stepId }
