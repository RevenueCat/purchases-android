package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.utils.serializers.DateSerializer
import com.revenuecat.purchases.utils.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

/**
 * Type representing a customer center event and associated data. Meant for RevenueCatUI use.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Serializable
data class CustomerCenterEvent(
    val creationData: CreationData,
    val eventData: Data
) {

    @Serializable
    data class CreationData(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = DateSerializer::class)
        val date: Date,
    )

    @Serializable
    data class Data(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID?,
        val type: CustomerCenterEventType,
        @Serializable(with = DateSerializer::class)
        val timestamp: Date,
        val darkMode: Boolean,
        val locale: String,
        val isSandbox: Boolean,

        var version: Int = 1,
        // Always full screen in Android
        val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        // We don't support revisions in the backend yet so hardcoding to 1 for now
        val revisionId: Int = 1
    )
}