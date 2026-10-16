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
 * The delegate only ever hears the current user's token, read on the main thread when an update is delivered, and
 * never the same token twice in a row. Saves for a user who isn't current then are dropped; [currentUserChanged]
 * covers the switch itself, so a login's token is published whichever of the two is delivered first.
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

    // The token last given to the delegate; only touched on the main thread, except when a delegate is set.
    @Volatile
    private var lastPublished: LastPublished = LastPublished.None

    var delegate: AuthenticationDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
            lastPublished = LastPublished.None
        }

    fun userInitiatedStarted() {
        userInitiatedInFlight.incrementAndGet()
    }

    fun userInitiatedFinished() {
        userInitiatedInFlight.decrementAndGet()
    }

    fun report(update: TokenUpdate) {
        when (update) {
            is TokenUpdate.AccessToken -> mainDispatch { if (isCurrent(update.appUserID)) publishCurrentToken() }
            is TokenUpdate.RefreshFailed -> reportError(update.error, forAppUserID = update.appUserID)
        }
    }

    /** Publishes the current user's access token, if it changed; call after an identity change commits. */
    fun currentUserChanged() {
        mainDispatch { publishCurrentToken() }
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

    private fun publishCurrentToken() {
        val delegate = delegate ?: return
        val token = currentAccessToken(currentAppUserID())
        val last = lastPublished
        if (last is LastPublished.Token && last.accessToken == token) return
        lastPublished = LastPublished.Token(token)
        delegate.authenticatorDidUpdateAccessToken(token)
    }

    private sealed interface LastPublished {
        object None : LastPublished
        class Token(val accessToken: String?) : LastPublished
    }
}
