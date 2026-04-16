@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Resolves a [WorkflowDetailResponse] envelope into a [WorkflowFetchResult]
 * by handling inline data or CDN fetching + hash verification.
 */
internal class WorkflowDetailResolver(
    private val workflowCdnFetcher: WorkflowCdnFetcher,
    private val signatureVerificationMode: SignatureVerificationMode,
) {

    @Throws(IllegalStateException::class)
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
                        verifyContentHash(json, expectedHash)
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

    private fun verifyContentHash(json: String, expectedHash: String) {
        val actualHash = computeCanonicalHash(json)
        if (actualHash != expectedHash) {
            val error = PurchasesError(
                PurchasesErrorCode.SignatureVerificationError,
                "CDN workflow content hash mismatch. Expected: $expectedHash, got: $actualHash",
            )
            errorLog(error)
        }
    }

    companion object {
        private const val CONTROL_CHAR_UPPER_BOUND = 0x20
        private const val NON_ASCII_LOWER_BOUND = 0x80

        /**
         * Computes SHA-256 hex digest of the canonical JSON, excluding the "hash" field.
         * Mirrors the backend's `compute_workflow_json_hash` which uses
         * `json.dumps(sort_keys=True, separators=(',', ':'))` on the payload without the hash key.
         */
        internal fun computeCanonicalHash(json: String): String {
            val parsed = JSONObject(json)
            parsed.remove("hash")
            val canonical = toCanonicalJson(parsed)
            val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun toCanonicalJson(obj: Any?): String = buildString {
            when (obj) {
                is JSONObject -> {
                    append('{')
                    obj.keys().asSequence().sorted().forEachIndexed { index, key ->
                        if (index > 0) append(',')
                        append('"')
                        append(escapeJsonString(key))
                        append('"')
                        append(':')
                        append(toCanonicalJson(obj.get(key)))
                    }
                    append('}')
                }
                is JSONArray -> {
                    append('[')
                    for (i in 0 until obj.length()) {
                        if (i > 0) append(',')
                        append(toCanonicalJson(obj.get(i)))
                    }
                    append(']')
                }
                is String -> {
                    append('"')
                    append(escapeJsonString(obj))
                    append('"')
                }
                is Boolean, is Int, is Long, is Double, is Float -> append(obj)
                JSONObject.NULL, null -> append("null")
                else -> {
                    append('"')
                    append(escapeJsonString(obj.toString()))
                    append('"')
                }
            }
        }

        private fun escapeJsonString(s: String): String = buildString {
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    else -> if (c.code < CONTROL_CHAR_UPPER_BOUND || c.code >= NON_ASCII_LOWER_BOUND) {
                        append("\\u%04x".format(c.code))
                    } else {
                        append(c)
                    }
                }
            }
        }
    }
}
