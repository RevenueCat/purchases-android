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
    public val paywallID: String?,
    @SerialName("session_id")
    public val sessionID: String,
    @SerialName("revision")
    public val revision: Int,
    @SerialName("display_mode")
    public val displayMode: String,
    @SerialName("dark_mode")
    public val darkMode: Boolean,
    @SerialName("locale")
    public val localeIdentifier: String,
    @SerialName("offering_id")
    public val offeringId: String,
) {
    public companion object {
        val json = Json.Default
    }

    public fun toMap(): Map<String, Any>? {
        val map = json.encodeToJsonElement(this).asMap() ?: return null
        return map.filterNotNullValues()
    }
}
