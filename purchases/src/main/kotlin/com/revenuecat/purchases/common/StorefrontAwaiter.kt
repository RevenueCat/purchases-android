package com.revenuecat.purchases.common

import com.revenuecat.purchases.interfaces.StorefrontProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds backend requests until the Play Store storefront is cached, or until [timeout] passes, whichever comes first.
 *
 * Play Billing only reports the storefront asynchronously after connecting, so on a fresh install the first
 * requests would otherwise go out without `X-Storefront`. The backend evaluates experiment and targeting audiences
 * on those first requests and keeps the enrollment, so a missing storefront can enroll a user permanently in the
 * wrong experiment. iOS doesn't need this because StoreKit returns the storefront synchronously.
 *
 * Once released, it never holds a request again.
 */
internal class StorefrontAwaiter(
    private val storefrontProvider: StorefrontProvider,
    private val scheduleTimeout: (delay: Duration, task: () -> Unit) -> Unit,
    private val timeout: Duration = DEFAULT_TIMEOUT,
) {
    companion object {
        val DEFAULT_TIMEOUT: Duration = 1500.milliseconds
    }

    private val pendingActions = mutableListOf<() -> Unit>()
    private var released = false
    private var timeoutScheduled = false

    fun runWhenStorefrontAvailable(action: () -> Unit) {
        val runNow = synchronized(this) {
            if (!released && storefrontProvider.getStorefront() != null) {
                released = true
            }
            if (!released) {
                pendingActions.add(action)
                if (!timeoutScheduled) {
                    timeoutScheduled = true
                    scheduleTimeout(timeout) { release() }
                }
            }
            released
        }
        if (runNow) action()
    }

    fun release() {
        val actions = synchronized(this) {
            if (released) return
            released = true
            pendingActions.toList().also { pendingActions.clear() }
        }
        actions.forEach { it() }
    }
}
