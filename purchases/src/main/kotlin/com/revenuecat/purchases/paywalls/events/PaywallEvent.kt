package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.utils.serializers.DateSerializer
import com.revenuecat.purchases.utils.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

/**
 * Types of paywall events. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
enum class PaywallEventType(val value: String) {
    /**
     * The paywall was shown to the user.
     */
    IMPRESSION("paywall_impression"),

    /**
     * The user cancelled a purchase.
     */
    CANCEL("paywall_cancel"),

    /**
     * The paywall was dismissed.
     */
    CLOSE("paywall_close"),

    /**
     * The user initiated a purchase through the paywall.
     */
    PURCHASE_INITIATED("paywall_purchase_initiated"),

    /**
     * The user encountered an error during purchase.
     */
    PURCHASE_ERROR("paywall_purchase_error"),

    /**
     * An exit offer will be shown to the user.
     */
    EXIT_OFFER("paywall_exit_offer"),
}

/**
 * Types of exit offers. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
enum class ExitOfferType(val value: String) {
    /**
     * An exit offer shown when the user attempts to dismiss the paywall without interacting.
     */
    DISMISS("dismiss"),
}

/**
 * Type representing a paywall event and associated data. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Serializable
data class PaywallEvent(
    val creationData: CreationData,
    val data: Data,
    val type: PaywallEventType,
) : FeatureEvent {

    @Serializable
    data class CreationData(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = DateSerializer::class)
        val date: Date,
    )

    @Serializable
    data class Data(
        val paywallIdentifier: String?,
        val offeringIdentifier: String,
        val paywallRevision: Int,
        @Serializable(with = UUIDSerializer::class)
        val sessionIdentifier: UUID,
        val displayMode: String, // Refer to PaywallMode in the RevenueCatUI module.
        val localeIdentifier: String,
        val darkMode: Boolean,
        val exitOfferType: ExitOfferType? = null,
        val exitOfferingIdentifier: String? = null,
        val packageIdentifier: String? = null,
        val productIdentifier: String? = null,
        val errorCode: Int? = null,
        val errorMessage: String? = null,
    )

    internal fun toPaywallPostReceiptData(): PaywallPostReceiptData {
        return PaywallPostReceiptData(
            paywallID = data.paywallIdentifier,
            sessionID = data.sessionIdentifier.toString(),
            revision = data.paywallRevision,
            displayMode = data.displayMode,
            darkMode = data.darkMode,
            localeIdentifier = data.localeIdentifier,
            offeringId = data.offeringIdentifier,
        )
    }
}
