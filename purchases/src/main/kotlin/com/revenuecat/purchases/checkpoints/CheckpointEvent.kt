package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import java.util.Date
import java.util.UUID

/**
 * Records that a checkpoint was hit. This is persisted and sent through the shared analytics events pipeline.
 */
@OptIn(InternalRevenueCatAPI::class)
internal data class CheckpointEvent(
    val identifier: String,
    val id: UUID = UUID.randomUUID(),
    val timestamp: Date = Date(),
) : FeatureEvent
