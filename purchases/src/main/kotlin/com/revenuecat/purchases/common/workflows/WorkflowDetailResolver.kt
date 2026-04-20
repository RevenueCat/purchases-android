@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import java.security.MessageDigest

/**
 * Resolves a [WorkflowDetailResponse] envelope into a [WorkflowFetchResult]
 * by handling inline data or CDN fetching + hash verification.
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
                val json = workflowCdnFetcher.fetchCompiledWorkflowJson(url)
                val expectedHash = response.hash
                if (expectedHash != null) {
                    verifyContentHash(json, url, expectedHash)
                }
                WorkflowJsonParser.parsePublishedWorkflow(json)
            }
        }
        return WorkflowFetchResult(
            workflow = workflow,
            enrolledVariants = response.enrolledVariants,
        )
    }

    @Throws(SignatureVerificationException::class)
    private fun verifyContentHash(json: String, url: String, expectedHash: String) {
        val actualHash = computeCanonicalHash(json)
        if (actualHash != expectedHash) {
            throw SignatureVerificationException(url)
        }
    }

    companion object {
        /**
         * Computes SHA-256 hex digest of the CDN workflow JSON.
         * The backend uploads canonical JSON (sorted keys, compact separators)
         * and computes the hash from those same bytes, so we just hash the
         * raw CDN content directly.
         */
        internal fun computeCanonicalHash(json: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
