package com.revenuecat.purchases

import android.os.Handler
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.CustomerInfoStrings
import java.util.Date

/**
 * Reports the moment the earliest active promotional entitlement of the last tracked [CustomerInfo] expires
 * according to the device clock.
 *
 * Only promotional entitlements are tracked because they are the only ones without a request-date grace period
 * (see `DateHelper.isDateActive`), so building the cached [CustomerInfo] again after they expire is enough to make
 * them inactive.
 *
 * It is purely local: it never touches the network.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class PromotionalEntitlementExpirationScheduler(
    private val handler: Handler,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    private var onExpirationReached: (() -> Unit)? = null
    private var nextExpiration: Date? = null
    private var closed = false

    private val expirationRunnable = Runnable {
        val onExpirationReached = synchronized(this) {
            nextExpiration = null
            onExpirationReached
        }
        log(LogIntent.DEBUG) { CustomerInfoStrings.PROMOTIONAL_ENTITLEMENT_EXPIRATION_REACHED }
        onExpirationReached?.invoke()
    }

    /**
     * Replaces whatever was tracked before with the active promotional entitlements of [customerInfo].
     *
     * [onExpirationReached] is invoked on the [handler]'s thread. The expired entitlement is not tracked anymore
     * by then: [track] the re-evaluated [CustomerInfo] to wait for the next one.
     */
    fun track(customerInfo: CustomerInfo, onExpirationReached: () -> Unit) {
        val expiration = customerInfo.entitlements.active.values
            .filter { it.store == Store.PROMOTIONAL }
            .mapNotNull { it.expirationDate }
            .minOrNull()
        synchronized(this) {
            if (closed) return
            nextExpiration = expiration
            this.onExpirationReached = onExpirationReached
            schedule()
        }
        if (expiration != null) {
            log(LogIntent.DEBUG) {
                CustomerInfoStrings.PROMOTIONAL_ENTITLEMENT_EXPIRATION_SCHEDULED.format(expiration)
            }
        }
    }

    /**
     * Schedules again from the wall clock. Call when the app comes to the foreground: the [handler] measures
     * delays in uptime, which does not advance while the device is in deep sleep.
     */
    @Synchronized
    fun reschedule() {
        schedule()
    }

    /**
     * Stops reporting for good: later [track] calls are ignored, so nothing is left posted to the [handler].
     */
    @Synchronized
    fun close() {
        closed = true
        nextExpiration = null
        onExpirationReached = null
        handler.removeCallbacks(expirationRunnable)
    }

    private fun schedule() {
        handler.removeCallbacks(expirationRunnable)
        val expiration = nextExpiration ?: return
        handler.postDelayed(expirationRunnable, (expiration.time - dateProvider.now.time).coerceAtLeast(0))
    }
}
