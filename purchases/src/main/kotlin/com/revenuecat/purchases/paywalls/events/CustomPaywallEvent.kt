package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
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
        public class Data private constructor(
            public val paywallId: String?,
            public val offeringId: String?,
            public val placementIdentifier: String?,
            public val targetingRevision: Int?,
            public val targetingRuleId: String?,
            @get:JvmSynthetic
            internal val workflowId: String?,
            @get:JvmSynthetic
            internal val stepId: String?,
            @get:JvmSynthetic
            internal val blobRef: String?,
        ) {
            public constructor(
                paywallId: String?,
                offeringId: String? = null,
                placementIdentifier: String? = null,
                targetingRevision: Int? = null,
                targetingRuleId: String? = null,
            ) : this(paywallId, offeringId, placementIdentifier, targetingRevision, targetingRuleId, null, null, null)

            internal constructor(
                paywallId: String?,
                offeringId: String?,
                presentedOfferingContext: PresentedOfferingContext?,
            ) : this(
                paywallId = paywallId,
                offeringId = offeringId,
                placementIdentifier = presentedOfferingContext?.placementIdentifier,
                targetingRevision = presentedOfferingContext?.targetingContext?.revision,
                targetingRuleId = presentedOfferingContext?.targetingContext?.ruleId,
                workflowId = presentedOfferingContext?.workflowId,
                stepId = presentedOfferingContext?.stepId,
                blobRef = presentedOfferingContext?.blobRef,
            )
        }
    }
}
