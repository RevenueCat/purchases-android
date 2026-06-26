@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.elapsedMillisecondsSince
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.models.Checksum
import kotlinx.coroutines.CancellationException

/**
 * Resolves a [WorkflowDetailResponse] envelope into a [WorkflowDataResult]
 * by handling inline data or CDN fetching + checksum validation.
 */
internal class WorkflowDetailResolver(
    private val workflowCdnFetcher: WorkflowCdnFetcher,
) {

    @Throws(IllegalStateException::class, SignatureVerificationException::class)
    suspend fun resolve(response: WorkflowDetailResponse): WorkflowDataResult {
        val workflow = when (response.action) {
            WorkflowResponseAction.INLINE -> {
                response.data
                    ?: error("Inline workflow response missing data")
            }
            WorkflowResponseAction.USE_CDN -> {
                val url = response.url
                    ?: error("CDN workflow response missing url")
                val checksum = response.hash?.let { Checksum(Checksum.Algorithm.SHA256, it) }
                val cdnFetchStartedAtNanos = System.nanoTime()
                val json = try {
                    workflowCdnFetcher.fetchCompiledWorkflowJson(url, checksum)
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    debugLog {
                        "Workflow CDN fetch failed in " +
                            "${elapsedMillisecondsSince(cdnFetchStartedAtNanos)} ms."
                    }
                    throw e
                }
                debugLog {
                    "Workflow CDN fetch completed in " +
                        "${elapsedMillisecondsSince(cdnFetchStartedAtNanos)} ms."
                }
                WorkflowJsonParser.parsePublishedWorkflow(json)
            }
        }
        return WorkflowDataResult(
            workflow = workflow,
            enrolledVariants = response.enrolledVariants,
        )
    }
}
