package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCheckpoint
import com.revenuecat.purchases.checkpoints.CheckpointParams

/**
 * The one place this app calls the checkpoints API. Every screen goes through here so the try/catch and the
 * result formatting live in a single place.
 */
object CheckpointRunner {

    @OptIn(InternalRevenueCatAPI::class)
    suspend fun run(identifier: String, vararg customProperties: Pair<String, Any?>): CheckpointResultUi =
        try {
            Purchases.sharedInstance.awaitCheckpoint(
                identifier,
                CheckpointParams(mapOf("source" to "checkpoint-tester") + customProperties),
            ).toUi()
        } catch (e: PurchasesException) {
            e.toCheckpointResultUi()
        }
}
