package com.revenuecat.purchases.identity

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Hears about authentication changes that no explicit call reports: tokens refreshed or cleared in the
 * background, and errors from refreshing them or from the silent anonymous login. Called on the main thread.
 *
 * An error usually means the SDK needs a new [Identity] passed to [Authentication.logIn]. Failures of
 * [Authentication.logIn] and [Authentication.logOut] themselves go to their callbacks instead.
 */
@InternalRevenueCatAPI
public interface AuthenticationDelegate {

    /**
     * The SDK hit an authentication error it couldn't recover from while doing other work, e.g. a token refresh
     * failed. The call that triggered it reports its own failure too. Treat every access token previously passed
     * to [authenticatorDidUpdateAccessToken] as invalid.
     */
    public fun authenticatorDidEncounterError(error: PurchasesError)

    /**
     * The current user's access token changed: a new one after login or refresh, or `null` once cleared. It can
     * be used to call the RevenueCat backend directly on that user's behalf.
     */
    public fun authenticatorDidUpdateAccessToken(newAccessToken: String?) {
        // Optional.
    }
}
