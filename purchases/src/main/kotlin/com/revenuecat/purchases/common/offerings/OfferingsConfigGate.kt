package com.revenuecat.purchases.common.offerings

/**
 * Gates offerings delivery on remote-config readiness — the config-endpoint replacement for the old
 * "wait for the workflows list before returning offerings" behavior.
 *
 * In production this is backed by `RemoteConfigManager.awaitConfigReady` (it ensures a config sync has happened
 * before signalling ready). `OfferingsManager` stays ignorant of which topics matter and of coroutines: it just
 * calls [awaitReady] and delivers in [onReady]. When no gate is wired, offerings deliver immediately.
 */
internal fun interface OfferingsConfigGate {
    /** Invokes [onReady] once the config required before delivering offerings is in place. */
    fun awaitReady(appInBackground: Boolean, appUserID: String, onReady: () -> Unit)
}
