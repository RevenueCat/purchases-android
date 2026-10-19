package com.revenuecat.purchases.identity

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.safeResumeWithException
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * IAM login and logout for other RevenueCat SDKs, reached through `Purchases.sharedInstance.authentication`.
 * Mirrors iOS's `Authentication`. With IAM disabled, [logIn] fails with
 * [PurchasesErrorCode.ConfigurationError] and makes no request.
 */
@InternalRevenueCatAPI
public class Authentication internal constructor(
    private val reporter: AuthenticationReporter,
    private val tokenManager: TokenManager,
    private val currentAppUserID: () -> String,
    private val flows: IdentityFlows,
    private val customEntitlementComputation: Boolean,
    private val mainDispatch: (() -> Unit) -> Unit,
) {

    /**
     * Hears about authentication errors no explicit call reports, and access-token changes. Held weakly:
     * keep your own reference to it.
     */
    public var delegate: AuthenticationDelegate?
        get() = reporter.delegate
        set(value) {
            reporter.delegate = value
        }

    /** The current user's access token, if there is one and the token cache has loaded. */
    public val currentAccessToken: String?
        get() = tokenManager.currentAccessToken(currentAppUserID())

    /**
     * Logs in with [identity]. From an anonymous user with an ID token, the two identities are linked. [callback]
     * gets the new user's [CustomerInfo] on the main thread.
     */
    public fun logIn(identity: Identity, callback: ReceiveCustomerInfoCallback) {
        reporter.userInitiatedStarted()
        flows.logIn(identity, finishing(callback))
    }

    /**
     * Logs the current user out, reverting to an anonymous user. [callback] gets that user's [CustomerInfo] on the
     * main thread.
     */
    public fun logOut(callback: ReceiveCustomerInfoCallback? = null) {
        if (customEntitlementComputation) {
            val error = PurchasesError(
                PurchasesErrorCode.UnsupportedError,
                "Logging out isn't available in custom entitlement computation mode.",
            )
            mainDispatch { callback?.onError(error) }
            return
        }
        reporter.userInitiatedStarted()
        flows.logOut(finishing(callback))
    }

    /**
     * Coroutine version of [logIn]. Cancelling stops the wait, not the login: it still completes, and the
     * identity still changes.
     *
     * @throws PurchasesException with the login's error.
     */
    @JvmSynthetic
    @Throws(PurchasesException::class)
    public suspend fun awaitLogIn(identity: Identity): CustomerInfo =
        suspendCancellableCoroutine { continuation -> logIn(identity, resuming(continuation)) }

    /**
     * Coroutine version of [logOut]. Cancelling stops the wait, not the logout.
     *
     * @throws PurchasesException with the logout's error.
     */
    @JvmSynthetic
    @Throws(PurchasesException::class)
    public suspend fun awaitLogOut(): CustomerInfo =
        suspendCancellableCoroutine { continuation -> logOut(resuming(continuation)) }

    private fun resuming(continuation: CancellableContinuation<CustomerInfo>) =
        object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) = continuation.safeResume(customerInfo)
            override fun onError(error: PurchasesError) =
                continuation.safeResumeWithException(PurchasesException(error))
        }

    // Results go to the main thread whatever thread the flow finished on; mainDispatch runs inline when already there.
    private fun finishing(callback: ReceiveCustomerInfoCallback?) = object : ReceiveCustomerInfoCallback {
        override fun onReceived(customerInfo: CustomerInfo) {
            reporter.userInitiatedFinished()
            mainDispatch { callback?.onReceived(customerInfo) }
        }

        override fun onError(error: PurchasesError) {
            reporter.userInitiatedFinished()
            mainDispatch { callback?.onError(error) }
        }
    }

    /** The orchestrator-level login and logout, so listeners, offerings and remote config follow the switch. */
    internal interface IdentityFlows {
        fun logIn(identity: Identity, callback: ReceiveCustomerInfoCallback)
        fun logOut(callback: ReceiveCustomerInfoCallback)
    }
}
