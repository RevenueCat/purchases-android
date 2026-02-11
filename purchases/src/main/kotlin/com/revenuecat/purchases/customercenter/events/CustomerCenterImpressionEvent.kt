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
public class CustomerCenterImpressionEvent(
    val creationData: CreationData = CreationData(),
    val data: Data,
) : FeatureEvent {
    @Poko
    class CreationData(
        public val id: UUID = UUID.randomUUID(),
        public val date: Date = Date(),
    )

    @Poko
    @SuppressWarnings("LongParameterList")
    class Data(
        public val timestamp: Date,
        public val darkMode: Boolean,
        public val locale: String,
        public val version: Int = 1,
        public val revisionID: Int = 1,
        public val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        // isSandbox not available in Android
    ) {
        val type: CustomerCenterEventType = CustomerCenterEventType.IMPRESSION
    }
}
