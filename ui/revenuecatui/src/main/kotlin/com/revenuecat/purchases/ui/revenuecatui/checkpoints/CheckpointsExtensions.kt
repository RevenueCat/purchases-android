package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException

/**
 * Registers that [checkpointIdentifier] was hit. Depending on the configured targeting rules, this may
 * auto-present an experience (the call resolves when it finishes) or do nothing.
 *
 * @param checkpointIdentifier The checkpoint identifier, as configured in the RevenueCat dashboard.
 * @param params Optional per-call parameters, like custom properties usable in targeting rules.
 * @throws [PurchasesException] with a [PurchasesError] if the checkpoint could not be handled.
 * @return The [CheckpointResult] for this checkpoint.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
@InternalRevenueCatAPI
public suspend fun Purchases.awaitCheckpoint(
    checkpointIdentifier: String,
    params: CheckpointParams? = null,
): CheckpointResult = CheckpointsCoordinator.checkpoint(this, checkpointIdentifier, params)

/**
 * Global listener for checkpoint activity, including the disposition of checkpoint-presented experiences.
 * Stored on the [Purchases] instance, so it is cleared when the SDK is reconfigured.
 */
@get:JvmSynthetic
@set:JvmSynthetic
@InternalRevenueCatAPI
public var Purchases.checkpointListener: CheckpointListener?
    // Only this file ever writes the slot, so a type mismatch is unreachable.
    get() = checkpointListenerSlot as? CheckpointListener
    set(value) {
        checkpointListenerSlot = value
    }
