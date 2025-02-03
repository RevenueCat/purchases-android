package com.revenuecat.purchases.paywalls.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PaywallBackendEvent(
    @get:JvmSynthetic
    val id: String,

    @get:JvmSynthetic
    val version: Int,

    @get:JvmSynthetic
    val type: String,

    @get:JvmSynthetic
    @SerialName("app_user_id")
    val appUserID: String,

    @get:JvmSynthetic
    @SerialName("session_id")
    val sessionID: String,

    @get:JvmSynthetic
    @SerialName("offering_id")
    val offeringID: String,

    @get:JvmSynthetic
    @SerialName("paywall_revision")
    val paywallRevision: Int,

    @get:JvmSynthetic
    val timestamp: Long,

    @get:JvmSynthetic
    @SerialName("display_mode")
    val displayMode: String,

    @get:JvmSynthetic
    @SerialName("dark_mode")
    val darkMode: Boolean,

    @get:JvmSynthetic
    @SerialName("locale")
    val localeIdentifier: String,
) {
    companion object {
        const val PAYWALL_EVENT_SCHEMA_VERSION = 1
    }
}
