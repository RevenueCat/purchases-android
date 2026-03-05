package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import dev.drewhamilton.poko.Poko
import java.util.Date
import java.util.UUID

/**
 * Sealed class representing custom paywall events. Each subtype represents a specific event type.
 */
@OptIn(InternalRevenueCatAPI::class)
internal sealed class CustomPaywallEvent : FeatureEvent {

    /**
     * Type representing a custom paywall impression event. Meant for tracking custom paywall views.
     */
    @Poko
    internal class Impression(
        val creationData: CreationData = CreationData(),
        val data: Data,
    ) : CustomPaywallEvent() {
        @Poko
        internal class CreationData(
            val id: UUID = UUID.randomUUID(),
            val date: Date = Date(),
        )

        @Poko
        internal class Data(
            val paywallId: String?,
        )
    }
}
