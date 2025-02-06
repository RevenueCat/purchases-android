package com.revenuecat.purchases.customercenter.events

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.utils.serializers.DateSerializer
import com.revenuecat.purchases.utils.serializers.UUIDSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID

/**
 * Type representing a customer center event and associated data. Meant for RevenueCatUI use.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
@Serializable
class CustomerCenterEvent(
    val creationData: CreationData,
    val data: Data,
) : FeatureEvent {

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val json = Json.Default
    }

    @ExperimentalPreviewRevenueCatPurchasesAPI
    @Poko
    @Serializable
    class CreationData(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = DateSerializer::class)
        val date: Date,
    )

    @ExperimentalPreviewRevenueCatPurchasesAPI
    @Serializable
    @Poko
    @SuppressWarnings("LongParameterList")
    class Data(
        val type: CustomerCenterEventType,
        @Serializable(with = DateSerializer::class)
        val timestamp: Date,
        @Serializable(with = UUIDSerializer::class)
        val sessionIdentifier: UUID,
        val darkMode: Boolean,
        val locale: String,
        val isSandbox: Boolean,
        val version: Int = 1,
        val revisionID: Int = 1,
        val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
    ) {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
       internal fun copy(
            type: CustomerCenterEventType = this.type,
            timestamp: Date = this.timestamp,
            sessionIdentifier: UUID = this.sessionIdentifier,
            darkMode: Boolean = this.darkMode,
            locale: String = this.locale,
            isSandbox: Boolean = this.isSandbox,
            version: Int = this.version,
            revisionID: Int = this.revisionID,
            displayMode: CustomerCenterDisplayMode = this.displayMode,
        ) = Data(
            type,
            Date(timestamp.time),
            sessionIdentifier,
            darkMode,
            locale,
            isSandbox,
            version,
            revisionID,
            displayMode,
        )
    }
}
