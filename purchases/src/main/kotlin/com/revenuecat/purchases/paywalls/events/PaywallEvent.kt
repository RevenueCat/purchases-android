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
public enum class PaywallEventType(public val value: String) {
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
}

/**
 * Type representing a paywall event and associated data. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Serializable
public data class PaywallEvent(
    public val creationData: CreationData,
    public val data: Data,
    public val type: PaywallEventType,
) : FeatureEvent {

    @Serializable
    public data class CreationData(
        @Serializable(with = UUIDSerializer::class)
        public val id: UUID,
        @Serializable(with = DateSerializer::class)
        public val date: Date,
    )

    @Serializable
    public data class Data(
        public val offeringIdentifier: String,
        public val paywallRevision: Int,
        @Serializable(with = UUIDSerializer::class)
        public val sessionIdentifier: UUID,
        public val displayMode: String, // Refer to PaywallMode in the RevenueCatUI module.
        public val localeIdentifier: String,
        public val darkMode: Boolean,
    )

    internal fun toPaywallPostReceiptData(): PaywallPostReceiptData {
        return PaywallPostReceiptData(
            sessionID = data.sessionIdentifier.toString(),
            revision = data.paywallRevision,
            displayMode = data.displayMode,
            darkMode = data.darkMode,
            localeIdentifier = data.localeIdentifier,
            offeringId = data.offeringIdentifier,
        )
    }
}
