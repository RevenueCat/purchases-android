@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import java.security.MessageDigest

/**
 * Resolves a [WorkflowDetailResponse] envelope into a [WorkflowFetchResult]
 * by handling inline data or CDN fetching + hash verification.
 */
internal class WorkflowDetailResolver(
    private val workflowCdnFetcher: WorkflowCdnFetcher,
    private val signatureVerificationMode: SignatureVerificationMode,
) {

    @Throws(IllegalStateException::class, SignatureVerificationException::class)
    fun resolve(response: WorkflowDetailResponse): WorkflowFetchResult {
        val workflow = when (response.action) {
            WorkflowResponseAction.INLINE -> {
                response.data
                    ?: error("Inline workflow response missing data")
            }
            WorkflowResponseAction.USE_CDN -> {
                val url = response.url
                    ?: error("CDN workflow response missing url")
                val json = workflowCdnFetcher.fetchCompiledWorkflowJson(url)
                val workflow = WorkflowJsonParser.parsePublishedWorkflow(json)
                if (signatureVerificationMode.shouldVerify) {
                    val expectedHash = response.hash
                    if (expectedHash != null) {
                        verifyContentHash(json, url, expectedHash)
                    }
                }
                workflow
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
        if (actualHash == expectedHash) return

        if (signatureVerificationMode is SignatureVerificationMode.Enforced) {
            throw SignatureVerificationException(url)
        }
        val error = PurchasesError(
            PurchasesErrorCode.SignatureVerificationError,
            "CDN workflow content hash mismatch. Expected: $expectedHash, got: $actualHash",
        )
        errorLog(error)
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
