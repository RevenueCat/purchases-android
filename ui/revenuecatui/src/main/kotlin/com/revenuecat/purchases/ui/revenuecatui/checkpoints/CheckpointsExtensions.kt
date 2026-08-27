package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException

/**
 * Registers that [checkpointIdentifier] was hit. Depending on the configured targeting rules, this may
 * auto-present an experience (the call resolves when it finishes) or do nothing.
 *
 * @param checkpointIdentifier The checkpoint identifier, as configured in the RevenueCat dashboard. It must start
 * with an ASCII letter, contain only ASCII letters, numbers, underscores, and hyphens, and be no more than 255
 * characters.
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
): CheckpointResult = checkpointsManager.checkpoint(this, checkpointIdentifier, params)

/**
 * Global listener for checkpoint activity, including the disposition of checkpoint-presented experiences.
 * Held by this [Purchases] instance, so it is cleared when the SDK is reconfigured.
 */
@get:JvmSynthetic
@set:JvmSynthetic
@InternalRevenueCatAPI
public var Purchases.checkpointListener: CheckpointListener?
    get() = checkpointsManager.checkpointListener
    set(value) {
        checkpointsManager.checkpointListener = value
    }

/**
 * The [CheckpointsManager] owned by this [Purchases] instance, created on first use and kept in the
 * instance's opaque `checkpointManagerSlot`. Storing it there rather than in a singleton ties the listener
 * and any in-flight presentation to the lifetime of the SDK instance, so reconfiguring the SDK cannot
 * inherit a stale listener or a presentation that will never complete.
 *
 * Synchronized on the receiver because reading and creating are two separate calls into the slot.
 */
internal val Purchases.checkpointsManager: CheckpointsManager
    get() = synchronized(this) {
        checkpointManagerSlot as? CheckpointsManager
            ?: CheckpointsManager().also { checkpointManagerSlot = it }
    }
