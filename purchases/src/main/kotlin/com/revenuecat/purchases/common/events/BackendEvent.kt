package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.events.CustomerCenterDisplayMode
import com.revenuecat.purchases.customercenter.events.CustomerCenterEventType
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
    @OptIn(InternalRevenueCatAPI::class)
    @Serializable
    @SerialName("customer_center")
    data class CustomerCenter constructor(
        val id: String,
        @SerialName("revision_id")
        val revisionID: Int,
        val type: CustomerCenterEventType,
        @SerialName("app_user_id")
        val appUserID: String,
        @SerialName("app_session_id")
        val appSessionID: String,
        val timestamp: Long,
        @SerialName("dark_mode")
        val darkMode: Boolean,
        val locale: String,
        @SerialName("display_mode")
        val displayMode: CustomerCenterDisplayMode,

        // only valid for survey option chosen
        val path: CustomerCenterConfigData.HelpPath.PathType?,
        val url: String?,
        @SerialName("survey_option_id")
        val surveyOptionID: String?,
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
     * @property exitOfferType The type of exit offer shown. Only for exit offer events.
     * @property exitOfferingID The offering ID of the exit offer shown. Only for exit offer events.
     * @property packageID The package ID of the purchase attempted. Only for purchase attempt events.
     * @property productID The product ID of the purchase attempted. Only for purchase attempt events.
     * @property errorCode The error code if an error occurred. Only for purchase attempt error events.
     * @property errorMessage The error message if an error occurred. Only for purchase attempt error events.
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
        @SerialName("paywall_id")
        val paywallID: String?,
        @SerialName("paywall_revision")
        val paywallRevision: Int,
        val timestamp: Long,
        @SerialName("display_mode")
        val displayMode: String,
        @SerialName("dark_mode")
        val darkMode: Boolean,
        @SerialName("locale")
        val localeIdentifier: String,
        @SerialName("exit_offer_type")
        val exitOfferType: String? = null,
        @SerialName("exit_offering_id")
        val exitOfferingID: String? = null,
        @SerialName("package_id")
        val packageID: String? = null,
        @SerialName("product_id")
        val productID: String? = null,
        @SerialName("error_code")
        val errorCode: Int? = null,
        @SerialName("error_message")
        val errorMessage: String? = null,
    ) : BackendEvent()

    @Serializable
    @SerialName("ad")
    data class Ad(
        val id: String,
        val version: Int,
        val type: String,
        @SerialName("timestamp_ms")
        val timestamp: Long,
        @SerialName("network_name")
        val networkName: String,
        @SerialName("mediator_name")
        val mediatorName: String,
        @SerialName("ad_format")
        // support deserializing events before this field
        val adFormat: String? = null,
        val placement: String?,
        @SerialName("ad_unit_id")
        val adUnitId: String,
        @SerialName("impression_id")
        val impressionId: String?,
        @SerialName("app_user_id")
        val appUserID: String,
        @SerialName("app_session_id")
        val appSessionID: String,

        // Revenue event only fields
        @SerialName("revenue_micros")
        val revenueMicros: Long? = null,
        val currency: String? = null,
        val precision: String? = null,

        // Failed to load event only fields
        @SerialName("mediator_error_code")
        val mediatorErrorCode: Int? = null,
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

        /**
         * Defines the version number of the ad event schema.
         */
        const val AD_EVENT_SCHEMA_VERSION = 1
    }
}
