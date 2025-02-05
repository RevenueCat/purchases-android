package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class BackendEvent : Event {
    @Serializable
    data class CustomerCenter(
        val id: String,
        @SerialName("revision_id")
        val revisionID: Int,
        val type: String,
        @SerialName("app_user_id")
        val appUserID: String,
        @SerialName("session_id")
        val appSessionID: String,
        val timestamp: Long,
        @SerialName("dark_mode")
        val darkMode: Boolean,
        val locale: String,
        @SerialName("is_sandbox")
        val isSandbox: Boolean,
        @SerialName("display_mode")
        val displayMode: String,
    ) : BackendEvent()

    @Serializable
    @SerialName("paywalls")
    data class Paywalls(
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
    ) : BackendEvent()

    companion object {
        const val PAYWALL_EVENT_SCHEMA_VERSION = 1
    }
}
