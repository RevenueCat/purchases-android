package com.revenuecat.paywallstester.ui.screens.main.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** A workflow synced through remote config, listed by id so workflows that claim no offering can be opened too. */
data class FlowRow(
    val id: String,
    val name: String?,
    val error: String?,
    /** Offerings that route to this flow, resolved the same way the SDK resolves an offering entry point. */
    val claimedOfferingIds: Set<String>,
    /** Offerings this flow's steps use, claimed or not. */
    val stepOfferingIds: Set<String>,
    /** Set for an offering flow: its only step is an offering step, so it shows no UI and returns this offering. */
    val returnedOfferingId: String?,
) {
    val isOfferingFlow: Boolean
        get() = returnedOfferingId != null

    val subtitle: String
        get() = error
            ?: claimedOfferingIds.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(prefix = "Offering: ")
            ?: stepOfferingIds.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(prefix = "Uses: ")
            ?: "No offering"

    fun referencesOffering(offeringId: String): Boolean =
        offeringId in claimedOfferingIds || offeringId in stepOfferingIds

    fun matches(query: String): Boolean =
        (listOfNotNull(id, name) + claimedOfferingIds + stepOfferingIds).any { it.lowercase().contains(query) }

    companion object {
        @OptIn(InternalRevenueCatAPI::class)
        // Off the main thread: reading a workflow decodes its JSON body.
        suspend fun loadAll(offerings: Offerings): List<FlowRow> = withContext(Dispatchers.Default) {
            val claims = offerings.all.keys
                .mapNotNull { offeringId ->
                    (Purchases.sharedInstance.resolveWorkflow(offeringId) as? WorkflowResolution.Found)
                        ?.let { it.workflowId to offeringId }
                }
                .groupBy({ it.first }, { it.second })
            coroutineScope {
                Purchases.sharedInstance.awaitGetWorkflowIds()
                    .map { workflowId -> async { load(workflowId, claims[workflowId].orEmpty().toSet()) } }
                    .awaitAll()
                    .sortedBy { (it.name ?: it.id).lowercase() }
            }
        }

        @OptIn(InternalRevenueCatAPI::class)
        private suspend fun load(workflowId: String, claimedOfferingIds: Set<String>): FlowRow =
            runCatching { Purchases.sharedInstance.awaitGetWorkflow(workflowId) }.fold(
                onSuccess = { workflow ->
                    val initialStep = workflow.steps[workflow.initialStepId]
                    // Same rule as checkpoints: an offering step must be the flow's only step.
                    val startsWithOfferingStep = initialStep?.isOfferingStep == true
                    val returnedOfferingId = initialStep?.offeringIdentifier?.takeIf { workflow.steps.size == 1 }
                    FlowRow(
                        id = workflowId,
                        name = workflow.displayName,
                        error = NOT_SERVABLE.takeIf { startsWithOfferingStep && returnedOfferingId == null },
                        claimedOfferingIds = claimedOfferingIds,
                        stepOfferingIds = workflow.stepOfferingIds(),
                        returnedOfferingId = returnedOfferingId.takeIf { startsWithOfferingStep },
                    )
                },
                onFailure = { error ->
                    FlowRow(
                        id = workflowId,
                        name = null,
                        error = error.message ?: error.toString(),
                        claimedOfferingIds = claimedOfferingIds,
                        stepOfferingIds = emptySet(),
                        returnedOfferingId = null,
                    )
                },
            )

        private const val NOT_SERVABLE = "Not servable: an offering step must be the only step and name an offering."

        // A step's own offering wins over the offering configured on its screen.
        @OptIn(InternalRevenueCatAPI::class)
        private fun PublishedWorkflow.stepOfferingIds(): Set<String> =
            steps.values.mapNotNull { step ->
                step.offeringIdentifier ?: step.screenId?.let { screens[it]?.offeringIdentifier }
            }.toSet()
    }
}
