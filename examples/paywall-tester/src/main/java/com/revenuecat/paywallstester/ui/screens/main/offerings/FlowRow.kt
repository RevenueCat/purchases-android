package com.revenuecat.paywallstester.ui.screens.main.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowListing
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
        suspend fun loadAll(): List<FlowRow> = coroutineScope {
            Purchases.sharedInstance.awaitWorkflowListings()
                .map { listing -> async { load(listing) } }
                .awaitAll()
                .sortedBy { (it.name ?: it.id).lowercase() }
        }

        @OptIn(InternalRevenueCatAPI::class)
        private suspend fun load(listing: WorkflowListing): FlowRow =
            runCatching { Purchases.sharedInstance.awaitGetWorkflow(listing.workflowId) }.fold(
                onSuccess = { workflow ->
                    val initialStep = workflow.steps[workflow.initialStepId]
                    FlowRow(
                        id = listing.workflowId,
                        name = workflow.displayName,
                        error = null,
                        claimedOfferingId = listing.offeringIdentifier,
                        offeringIds = workflow.stepOfferingIds(),
                        uiLessOfferingId = initialStep
                            ?.takeIf { it.isOfferingStep }
                            ?.let { it.offeringIdentifier ?: listing.offeringIdentifier },
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

        // A step's own offering wins over the offering configured on its screen.
        @OptIn(InternalRevenueCatAPI::class)
        private fun PublishedWorkflow.stepOfferingIds(): Set<String> =
            steps.values.mapNotNull { step ->
                step.offeringIdentifier ?: step.screenId?.let { screens[it]?.offeringIdentifier }
            }.toSet()
    }
}
