package com.revenuecat.purchases.paywalls.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PaywallBackendEvent(
    val id: String,
    val version: Int,
    val type: String,
    @SerialName("app_user_id")
    val appUserID: String,
    @SerialName("session_id")
    val sessionID: String,
    @SerialName("offering_id")
    val offeringID: String,
    @SerialName("paywall_revision")
    val paywallRevision: Int,
    val timestamp: Long,
    @SerialName("display_mode")
    val displayMode: String,
    @SerialName("dark_mode")
    val darkMode: Boolean,
    @SerialName("locale")
    val localeIdentifier: String,
) {
    companion object {
        const val PAYWALL_EVENT_SCHEMA_VERSION = 1
    }
}
