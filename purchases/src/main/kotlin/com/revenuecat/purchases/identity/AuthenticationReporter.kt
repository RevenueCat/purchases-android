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
 *
 * Only the current user's updates are delivered, checked on delivery: tokens saved by a login before its switch
 * commits, or by a refresh for a user since replaced, are never published. [currentUserChanged] publishes the
 * new user's token once a switch commits.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class AuthenticationReporter(
    private val mainDispatch: (() -> Unit) -> Unit,
    private val currentAppUserID: () -> String,
    private val currentAccessToken: (appUserID: String) -> String?,
) {

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
            is TokenUpdate.AccessToken -> delegate?.let { delegate ->
                mainDispatch {
                    if (isCurrent(update.appUserID)) delegate.authenticatorDidUpdateAccessToken(update.accessToken)
                }
            }
            is TokenUpdate.RefreshFailed -> reportError(update.error, forAppUserID = update.appUserID)
        }
    }

    /** Publishes the current user's access token; call after an identity change commits. */
    fun currentUserChanged() {
        delegate?.let { delegate ->
            mainDispatch { delegate.authenticatorDidUpdateAccessToken(currentAccessToken(currentAppUserID())) }
        }
    }

    fun reportError(error: PurchasesError, forAppUserID: String? = null) {
        if (userInitiatedInFlight.get() > 0) return
        delegate?.let { delegate ->
            mainDispatch {
                if (forAppUserID == null || isCurrent(forAppUserID)) delegate.authenticatorDidEncounterError(error)
            }
        }
    }

    private fun isCurrent(appUserID: String) = appUserID == currentAppUserID()
}
