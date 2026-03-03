package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import dev.drewhamilton.poko.Poko
import java.util.Date
import java.util.UUID

/**
 * Parameters for tracking a custom paywall impression event.
 */
internal data class CustomPaywallImpressionParams(
    val paywallId: String? = null,
)

/**
 * Type representing a custom paywall impression event. Meant for tracking custom paywall views.
 */
@OptIn(InternalRevenueCatAPI::class)
@Poko
internal class CustomPaywallImpressionEvent(
    val creationData: CreationData = CreationData(),
    val data: Data,
) : FeatureEvent {
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
