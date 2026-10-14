package com.revenuecat.paywallstester.ui.screens.main.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowListing
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
    /** The offering the flow is attached to in the dashboard, if any. */
    val claimedOfferingId: String?,
    /** Offerings this flow's steps use, claimed or not. */
    val offeringIds: Set<String>,
    /** Set when the first step is an offering step: the flow shows no UI and only returns this offering. */
    val uiLessOfferingId: String?,
) {
    val subtitle: String
        get() = error
            ?: claimedOfferingId?.let { "Offering: $it" }
            ?: offeringIds.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(prefix = "Uses: ")
            ?: "No offering"

    fun uses(offeringId: String): Boolean = claimedOfferingId == offeringId || offeringId in offeringIds

    fun matches(query: String): Boolean =
        listOfNotNull(id, name, claimedOfferingId).any { it.lowercase().contains(query) }

    companion object {
        @OptIn(InternalRevenueCatAPI::class)
        // Off the main thread: reading a workflow decodes its JSON body.
        suspend fun loadAll(): List<FlowRow> = withContext(Dispatchers.Default) {
            coroutineScope {
                Purchases.sharedInstance.awaitWorkflowListings()
                    .map { listing -> async { load(listing) } }
                    .awaitAll()
                    .sortedBy { (it.name ?: it.id).lowercase() }
            }
        }

        @OptIn(InternalRevenueCatAPI::class)
        private suspend fun load(listing: WorkflowListing): FlowRow =
            runCatching { Purchases.sharedInstance.awaitGetWorkflow(listing.workflowId) }.fold(
                onSuccess = { workflow ->
                    val initialStep = workflow.steps[workflow.initialStepId]
                    // Same rule as checkpoints: an offering step must be the flow's only step.
                    val isUiLess = initialStep?.isOfferingStep == true
                    val uiLessOfferingId = initialStep?.offeringIdentifier?.takeIf { workflow.steps.size == 1 }
                    FlowRow(
                        id = listing.workflowId,
                        name = workflow.displayName,
                        error = NOT_SERVABLE.takeIf { isUiLess && uiLessOfferingId == null },
                        claimedOfferingId = listing.offeringIdentifier,
                        offeringIds = workflow.stepOfferingIds(),
                        uiLessOfferingId = uiLessOfferingId.takeIf { isUiLess },
                    )
                },
                onFailure = { error ->
                    FlowRow(
                        id = listing.workflowId,
                        name = null,
                        error = error.message ?: error.toString(),
                        claimedOfferingId = listing.offeringIdentifier,
                        offeringIds = emptySet(),
                        uiLessOfferingId = null,
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
