package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases

/**
 * Registers that [checkpointIdentifier] was reached. Depending on the configured targeting rules, this may present a
 * flow or do nothing. [callback] is invoked at most once, on the main thread. If no rules are matched,
 * or if presenting fails, the callback is invoked with null.
 *
 * If a flow is presented, the callback is invoked when the user "goes through" the flow. That means,
 * the flow is "closed" and/or a purchase/restore happens. It will not be called if the user backs out of the flow
 * (system back, or a back action on a flow's first step).
 *
 * The callback [FlowResult] will be:
 * - null when nothing was presented: no rule matched, nothing could be served, or presenting failed;
 * - with what the user obtained when the presented flow ended with a purchase or restore, or through a close
 *   action, if anything.
 *
 * This call never throws. Why nothing was presented, and any failure, are reported in the logs.
 *
 * @param checkpointIdentifier The checkpoint identifier, as configured in the RevenueCat dashboard. It must start
 * with an ASCII letter, contain only ASCII letters, numbers, underscores, and hyphens, and be no more than 255
 * characters.
 * @param params Optional per-call parameters, like custom properties usable in targeting rules.
 * @param callback Receives the [FlowResult], or null, once the user goes through the checkpoint.
 */
@InternalRevenueCatAPI
public fun Purchases.checkpoint(
    checkpointIdentifier: String,
    params: CheckpointParams?,
    callback: CheckpointCallback,
) {
    checkpointsManager.checkpoint(this, checkpointIdentifier, params, callback)
}

/**
 * [checkpoint] without per-call parameters.
 */
@InternalRevenueCatAPI
public fun Purchases.checkpoint(
    checkpointIdentifier: String,
    callback: CheckpointCallback,
) {
    checkpoint(checkpointIdentifier, params = null, callback = callback)
}

/**
 * The [CheckpointsManager] owned by this [Purchases] instance, created on first use and kept in the
 * instance's opaque `internalCpManagerSlot`. Storing it there rather than in a singleton ties any in-flight
 * presentation to the lifetime of the SDK instance, so reconfiguring the SDK cannot inherit a presentation that
 * will never complete.
 *
 * Synchronized on the receiver because reading and creating are two separate calls into the slot.
 */
internal val Purchases.checkpointsManager: CheckpointsManager
    get() = synchronized(this) {
        internalCpManagerSlot as? CheckpointsManager
            ?: CheckpointsManager().also { internalCpManagerSlot = it }
    }
