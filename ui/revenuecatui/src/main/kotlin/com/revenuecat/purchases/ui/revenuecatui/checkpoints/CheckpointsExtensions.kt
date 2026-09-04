package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException

/**
 * Registers that [checkpointIdentifier] was hit. Depending on the configured targeting rules, this may
 * auto-present an experience or do nothing. [callback] may be invoked at most once, on the main thread, when the
 * checkpoint finishes:
 * - If the checkpoint is configured as a hard gate in the dashboard, only if the customer
 * does not match any rule when entering the checkpoint or if getting an entitlement while going through the checkpoint.
 * - If the checkpoint is configured as a soft gate in the dashboard (default), only if the customer
 * does not match any rule when entering the checkpoint, or if the user finishes the entire flow experience.
 *
 * It reports what the user obtained while going through the checkpoint, if anything.
 *
 * This call never throws; failures are reported through the result's
 * [error][CheckpointGateResult.error].
 *
 * @param checkpointIdentifier The checkpoint identifier, as configured in the RevenueCat dashboard. It must start
 * with an ASCII letter, contain only ASCII letters, numbers, underscores, and hyphens, and be no more than 255
 * characters.
 * @param params Optional per-call parameters, like custom properties usable in targeting rules.
 * @param callback Receives the [CheckpointGateResult] once the checkpoint finishes.
 */
@InternalRevenueCatAPI
public fun Purchases.checkpoint(
    checkpointIdentifier: String,
    params: CheckpointParams?,
    callback: CheckpointGateCallback,
) {
    checkpointsManager.checkpointGate(this, checkpointIdentifier, params, callback)
}

/**
 * [checkpoint] without per-call parameters.
 */
@InternalRevenueCatAPI
public fun Purchases.checkpoint(
    checkpointIdentifier: String,
    callback: CheckpointGateCallback,
) {
    checkpoint(checkpointIdentifier, params = null, callback = callback)
}

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
@Throws(PurchasesException::class)
internal suspend fun Purchases.awaitCheckpoint(
    checkpointIdentifier: String,
    params: CheckpointParams? = null,
): CheckpointResult = checkpointsManager.checkpoint(this, checkpointIdentifier, params)

/**
 * Presents the offerings checkpoints resolve to with app-owned UI. When null, an offering's configured paywall
 * is presented instead, falling back to the default paywall. Held by this [Purchases] instance, so it is
 * cleared when the SDK is reconfigured.
 */
@InternalRevenueCatAPI
public var Purchases.checkpointOfferingPresenter: CheckpointOfferingPresenter?
    get() = checkpointsManager.checkpointOfferingPresenter
    set(value) {
        checkpointsManager.checkpointOfferingPresenter = value
    }

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
