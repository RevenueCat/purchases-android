package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend events
 */
@Serializable
internal sealed class BackendEvent : Event {

    /**
     * Represents an event related to the Customer Center.
     *
     * @property id Unique identifier for the event.
     * @property revisionID The revision identifier.
     * @property type Type of the event.
     * @property appUserID The app user identifier associated with this event.
     * @property appSessionID The session ID of the app session when this event occurred.
     * Differs from PaywallEvent.sessionID
     * @property timestamp Unix timestamp representing when the event occurred.
     * @property darkMode Whether the app was in dark mode at the time of the event.
     * @property locale The locale setting of the device.
     * @property isSandbox Whether the event originated from a sandbox environment.
     * @property displayMode The display mode of the Customer Center.
     */
    @Serializable
    data class CustomerCenter(
        val id: String,
        @SerialName("revision_id")
        val revisionID: Int,
        val type: String,
        @SerialName("app_user_id")
        val appUserID: String,
        @SerialName("app_session_id")
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

    /**
     * Represents an event related to Paywalls.
     *
     * @property id Unique identifier for the event.
     * @property version Version number of the Paywall.
     * @property type Type of the event.
     * @property appUserID The app user identifier associated with this event.
     * @property sessionID The session ID of the app session when this event occurred.
     * @property offeringID The offering ID related to this paywall event.
     * @property paywallRevision Revision number of the paywall.
     * @property timestamp Unix timestamp representing when the event occurred.
     * @property displayMode The display mode of the Paywall.
     * @property darkMode Whether the app was in dark mode at the time of the event.
     * @property localeIdentifier The locale identifier of the device.
     */
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

    /**
     * Companion object containing constants related to backend events.
     */
    companion object {
        /**
         * Defines the version number of the paywall event schema.
         */
        const val PAYWALL_EVENT_SCHEMA_VERSION = 1

        /**
         * Defines the version number of the customer center event schema.
         */
        const val CUSTOMER_CENTER_EVENT_SCHEMA_VERSION = 1
    }
}
