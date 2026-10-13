package com.revenuecat.purchases.identity

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.networking.TokenManager.TokenUpdate
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * Relays token updates and background authentication errors to the [AuthenticationDelegate], which is held
 * weakly, on the main thread via [mainDispatch]. Errors are dropped while a user-initiated login or logout is in
 * flight, since that call reports its own failure (mirrors iOS's `ongoingUserInitiatedRequestCount`).
 */
@OptIn(InternalRevenueCatAPI::class)
internal class AuthenticationReporter(private val mainDispatch: (() -> Unit) -> Unit) {

    @Volatile
    private var delegateReference: WeakReference<AuthenticationDelegate>? = null

    private val userInitiatedInFlight = AtomicInteger(0)

    var delegate: AuthenticationDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
        }

    fun userInitiatedStarted() {
        userInitiatedInFlight.incrementAndGet()
    }

    fun userInitiatedFinished() {
        userInitiatedInFlight.decrementAndGet()
    }

    fun report(update: TokenUpdate) {
        when (update) {
            is TokenUpdate.AccessToken ->
                delegate?.let { mainDispatch { it.authenticatorDidUpdateAccessToken(update.accessToken) } }
            is TokenUpdate.RefreshFailed -> reportError(update.error)
        }
    }

    fun reportError(error: PurchasesError) {
        if (userInitiatedInFlight.get() > 0) return
        delegate?.let { mainDispatch { it.authenticatorDidEncounterError(error) } }
    }
}
