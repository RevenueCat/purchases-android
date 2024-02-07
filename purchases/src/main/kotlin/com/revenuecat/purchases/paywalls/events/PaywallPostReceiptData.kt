package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.utils.asMap
import com.revenuecat.purchases.utils.filterNotNullValues
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
internal data class PaywallPostReceiptData(
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
) {
    companion object {
        val json = Json.Default
    }

    fun toMap(): Map<String, Any>? {
        val map = json.encodeToJsonElement(this).asMap() ?: return null
        return map.filterNotNullValues()
    }
}
