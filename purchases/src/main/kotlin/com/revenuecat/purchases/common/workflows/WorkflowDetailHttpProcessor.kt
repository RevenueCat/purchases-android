@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.networking.HTTPResult
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Normalizes a successful workflow-detail HTTP payload: `inline` (unwrap `data`) or `use_cdn` (fetch JSON).
 * Non-success [HTTPResult] is returned unchanged with null [WorkflowDetailHttpProcessingResult.enrolledVariants].
 */
internal class WorkflowDetailHttpProcessor(
    private val workflowCdnFetcher: WorkflowCdnFetcher,
) {

    @Throws(JSONException::class, IOException::class)
    fun process(result: HTTPResult): WorkflowDetailHttpProcessingResult {
        if (!result.isSuccessful()) {
            return WorkflowDetailHttpProcessingResult(httpResult = result, enrolledVariants = null)
        }
        val root = JSONObject(result.payload)
        val enrolledVariants = root.optJSONObject("enrolled_variants")?.toEnrolledVariantsMap()
        val action = WorkflowResponseAction.fromValue(root.getString("action"))
        val normalizedPayload = when (action) {
            WorkflowResponseAction.INLINE -> root.getJSONObject("data").toString()
            WorkflowResponseAction.USE_CDN -> workflowCdnFetcher.fetchCompiledWorkflowJson(root.getString("url"))
        }
        return WorkflowDetailHttpProcessingResult(
            httpResult = result.copy(payload = normalizedPayload),
            enrolledVariants = enrolledVariants,
        )
    }

    private fun JSONObject.toEnrolledVariantsMap(): Map<String, String>? {
        val map = mutableMapOf<String, String>()
        val keyIterator = keys()
        while (keyIterator.hasNext()) {
            val key = keyIterator.next()
            map[key] = optString(key)
        }
        return map.takeUnless { it.isEmpty() }
    }
}

internal data class WorkflowDetailHttpProcessingResult(
    val httpResult: HTTPResult,
    val enrolledVariants: Map<String, String>?,
)
