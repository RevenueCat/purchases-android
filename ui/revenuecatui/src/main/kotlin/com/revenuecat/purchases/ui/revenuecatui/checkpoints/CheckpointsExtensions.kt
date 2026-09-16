package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases

/**
 * Registers that [checkpointIdentifier] was hit. Depending on the configured targeting rules, this may present a
 * flow or do nothing. [callback] is invoked at most once, on the main thread, when the user goes through the
 * checkpoint:
 * - with null when nothing was presented: no rule matched, nothing could be served, or presenting failed;
 * - with what the user obtained when the presented flow ended with a purchase or restore, or through a close
 *   action.
 *
 * It is not invoked when the user backs out of the presented flow (system back, or a back action on a flow's
 * first step): the flow is dismissed and the app keeps the user where they were. Don't model a checkpoint as a
 * pending operation the user waits on.
 *
 * Only one checkpoint flow is presented at a time. A checkpoint that resolves to a flow while another one is
 * already on screen is ignored and its callback is never invoked: the call that presented the flow is the one
 * that reports.
 *
 * Every checkpoint is a soft gate today. Hard gates, configured in the dashboard, will additionally require the
 * user to meet a backend-provided rule (typically holding an entitlement) before the callback is invoked.
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
    callback: CheckpointPassedCallback,
) {
    checkpointsManager.checkpoint(this, checkpointIdentifier, params, callback)
}

/**
 * [checkpoint] without per-call parameters.
 */
@InternalRevenueCatAPI
public fun Purchases.checkpoint(
    checkpointIdentifier: String,
    callback: CheckpointPassedCallback,
) {
    checkpoint(checkpointIdentifier, params = null, callback = callback)
}

/**
 * Presents the offerings checkpoints resolve to with app-owned UI, unless the call supplies its own through
 * [CheckpointParams.paywallPresenter]. When neither is set, an offering's configured paywall is presented
 * instead, falling back to the default paywall. Held by this [Purchases] instance, so it is cleared when the SDK
 * is reconfigured.
 */
@InternalRevenueCatAPI
public var Purchases.paywallPresenter: PaywallPresenter?
    get() = checkpointsManager.paywallPresenter
    set(value) {
        checkpointsManager.paywallPresenter = value
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
