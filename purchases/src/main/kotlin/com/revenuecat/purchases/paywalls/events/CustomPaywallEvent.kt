package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import dev.drewhamilton.poko.Poko
import java.util.Date
import java.util.UUID

/**
 * Sealed class representing custom paywall events. Each subtype represents a specific event type.
 */
@InternalRevenueCatAPI
public sealed class CustomPaywallEvent : FeatureEvent {

    override val isPriorityEvent: Boolean get() = true

    /**
     * Type representing a custom paywall impression event. Meant for tracking custom paywall views.
     */
    @Poko
    public class Impression(
        public val creationData: CreationData = CreationData(),
        public val data: Data,
    ) : CustomPaywallEvent() {
        @Poko
        public class CreationData(
            public val id: UUID = UUID.randomUUID(),
            public val date: Date = Date(),
        )

        @Poko
        public class Data(
            public val paywallId: String?,
            public val offeringId: String? = null,
            public val placementIdentifier: String? = null,
            public val targetingRevision: Int? = null,
            public val targetingRuleId: String? = null,
        )
    }
}
