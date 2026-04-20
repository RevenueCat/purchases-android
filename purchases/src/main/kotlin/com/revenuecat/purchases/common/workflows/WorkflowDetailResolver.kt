@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.models.Checksum

/**
 * Resolves a [WorkflowDetailResponse] envelope into a [WorkflowFetchResult]
 * by handling inline data or CDN fetching + checksum validation.
 */
internal class WorkflowDetailResolver(
    private val workflowCdnFetcher: WorkflowCdnFetcher,
) {

    @Throws(IllegalStateException::class, SignatureVerificationException::class)
    suspend fun resolve(response: WorkflowDetailResponse): WorkflowFetchResult {
        val workflow = when (response.action) {
            WorkflowResponseAction.INLINE -> {
                response.data
                    ?: error("Inline workflow response missing data")
            }
            WorkflowResponseAction.USE_CDN -> {
                val url = response.url
                    ?: error("CDN workflow response missing url")
                val checksum = response.hash?.let { Checksum(Checksum.Algorithm.SHA256, it) }
                val json = workflowCdnFetcher.fetchCompiledWorkflowJson(url, checksum)
                WorkflowJsonParser.parsePublishedWorkflow(json)
            }
        }
        return WorkflowFetchResult(
            workflow = workflow,
            enrolledVariants = response.enrolledVariants,
        )
    }
}
