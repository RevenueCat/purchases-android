package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.utils.asMap
import com.revenuecat.purchases.utils.filterNotNullValues
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
internal data class PaywallPostReceiptData(
    @SerialName("paywall_id")
    val paywallID: String?,
    @SerialName("session_id")
    val sessionID: String,
    @SerialName("revision")
    val revision: Int,
    @SerialName("display_mode")
    val displayMode: String,
    @SerialName("dark_mode")
    val darkMode: Boolean,
    @SerialName("locale")
    val localeIdentifier: String,
    @SerialName("offering_id")
    val offeringId: String,
    /**
     * Identifies a single traversal of a workflow, so the resulting transaction can be attributed to
     * the run that produced it. Sent nested here rather than as a top-level receipt param because the
     * post-receipt body rejects unknown top-level keys. `null` for standalone paywalls.
     */
    @SerialName("trace_id")
    val traceId: String? = null,
) {
    companion object {
        val json = Json.Default
    }

    fun toMap(): Map<String, Any>? {
        val map = json.encodeToJsonElement(this).asMap() ?: return null
        return map.filterNotNullValues()
    }
}
