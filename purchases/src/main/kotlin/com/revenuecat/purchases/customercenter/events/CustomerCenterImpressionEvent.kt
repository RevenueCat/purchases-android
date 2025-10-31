package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import dev.drewhamilton.poko.Poko
import java.util.Date
import java.util.UUID

/**
 * Type representing a customer center event and associated data. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Poko
class CustomerCenterImpressionEvent(
    val creationData: CreationData = CreationData(),
    val data: Data,
) : FeatureEvent {
    @Poko
    class CreationData(
        val id: UUID = UUID.randomUUID(),
        val date: Date = Date(),
    )

    @Poko
    class Data(
        val timestamp: Date,
        val darkMode: Boolean,
        val locale: String,
        val version: Int = 1,
        val revisionID: Int = 1,
        val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        // isSandbox not available in Android
    ) {
        val type: CustomerCenterEventType = CustomerCenterEventType.IMPRESSION
    }
}
