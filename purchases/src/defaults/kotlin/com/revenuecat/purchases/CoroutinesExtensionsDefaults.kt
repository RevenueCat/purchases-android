package com.revenuecat.purchases

import com.revenuecat.purchases.common.workflows.WorkflowResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Fetches a published workflow by identifier.
 *
 * @param workflowId The identifier of the workflow to fetch.
 * @throws [PurchasesException] with a [PurchasesError] if there's an error fetching the workflow.
 * @return The [WorkflowResult] for the given identifier.
 */
@InternalRevenueCatAPI
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitGetWorkflow(
    workflowId: String,
): WorkflowResult {
    return suspendCoroutine { continuation ->
        getWorkflow(
            workflowId = workflowId,
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}
