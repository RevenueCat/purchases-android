@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.identity

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.networking.TokenManager.TokenUpdate
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AuthenticationTest {

    private val customerInfo = mockk<CustomerInfo>()
    private val flows = FakeFlows()
    private val delegate = RecordingDelegate()
    private val mainQueue = ArrayDeque<() -> Unit>()
    private var deferMain = false
    private var currentUser = "user"
    private var tokens: TokenManager? = null
    private val mainDispatch: (() -> Unit) -> Unit = { if (deferMain) mainQueue.add(it) else it() }
    private val reporter = AuthenticationReporter(
        mainDispatch = mainDispatch,
        currentAppUserID = { currentUser },
        currentAccessToken = { tokens?.currentAccessToken(it) },
    )

    // region delegate reporting

    @Test
    fun `token updates reach the delegate`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.saveTokens("user", "access", "refresh", null)
        tokenManager.deleteTokens("user")

        assertThat(delegate.tokens).containsExactly("access", null)
    }

    @Test
    fun `tokens saved or deleted for another user are not published`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.saveTokens("incoming-user", "incoming-access", "refresh", null)
        tokenManager.deleteTokens("previous-user")

        assertThat(delegate.tokens).isEmpty()
    }

    @Test
    fun `a token whose user stopped being current before delivery is not published`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate
        deferMain = true

        tokenManager.saveTokens("user", "stale-access", "refresh", null)
        currentUser = "next-user"
        mainQueue.forEach { it() }

        assertThat(delegate.tokens).isEmpty()
    }

    @Test
    fun `a committed user change publishes the new user's token`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate
        tokenManager.saveTokens("next-user", "next-access", "refresh", null)

        currentUser = "next-user"
        reporter.currentUserChanged()

        assertThat(delegate.tokens).containsExactly("next-access")
    }

    @Test
    fun `a committed change to a user without tokens publishes null`() = runTest {
        authentication(tokenManager()).delegate = delegate

        currentUser = "tokenless-user"
        reporter.currentUserChanged()

        assertThat(delegate.tokens).containsExactly(null)
    }

    @Test
    fun `a login's token is published once when its save is delivered after the switch`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate
        deferMain = true

        tokenManager.saveTokens("next-user", "next-access", "refresh", null)
        currentUser = "next-user"
        reporter.currentUserChanged()
        mainQueue.forEach { it() }

        assertThat(delegate.tokens).containsExactly("next-access")
    }

    @Test
    fun `a login's token is published when its save is delivered before the switch`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.saveTokens("next-user", "next-access", "refresh", null)
        currentUser = "next-user"
        reporter.currentUserChanged()

        assertThat(delegate.tokens).containsExactly("next-access")
    }

    @Test
    fun `an unchanged token is not published again`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.saveTokens("user", "access", "refresh", null)
        reporter.currentUserChanged()
        tokenManager.saveTokens("user", "access", "refresh-2", null)

        assertThat(delegate.tokens).containsExactly("access")
    }

    @Test
    fun `a new delegate hears the current token on the next update`() = runTest {
        val tokenManager = tokenManager()
        val authentication = authentication(tokenManager).apply { delegate = this@AuthenticationTest.delegate }
        tokenManager.saveTokens("user", "access", "refresh", null)
        val second = RecordingDelegate()

        authentication.delegate = second
        reporter.currentUserChanged()

        assertThat(second.tokens).containsExactly("access")
    }

    @Test
    fun `a refresh failure for another user is not reported`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.reportRefreshFailure("previous-user", networkError)

        assertThat(delegate.errors).isEmpty()
    }

    @Test
    fun `a refresh failure during passive use reaches the delegate`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.reportRefreshFailure("user", networkError)

        assertThat(delegate.errors).containsExactly(networkError)
    }

    @Test
    fun `a refresh failure during an explicit login is reported only through the login`() = runTest {
        val tokenManager = tokenManager()
        val authentication = authentication(tokenManager).apply { delegate = this@AuthenticationTest.delegate }
        val callback = RecordingCallback()

        authentication.logIn(Identity.anonymous, callback)
        tokenManager.reportRefreshFailure("user", networkError)
        flows.pendingLogIn!!.onError(networkError)

        assertThat(delegate.errors).isEmpty()
        assertThat(callback.errors).containsExactly(networkError)

        // Once the explicit call has finished, passive failures are reported again.
        tokenManager.reportRefreshFailure("user", networkError)
        assertThat(delegate.errors).containsExactly(networkError)
    }

    @Test
    fun `a refresh failure during an explicit logout is reported only through the logout`() = runTest {
        val tokenManager = tokenManager()
        val authentication = authentication(tokenManager).apply { delegate = this@AuthenticationTest.delegate }

        authentication.logOut(RecordingCallback())
        tokenManager.reportRefreshFailure("user", networkError)
        flows.pendingLogOut!!.onReceived(customerInfo)

        assertThat(delegate.errors).isEmpty()
    }

    @Test
    fun `the delegate is not retained`() = runTest {
        val authentication = authentication(tokenManager())
        authentication.delegate = RecordingDelegate()

        val deadline = System.currentTimeMillis() + 5_000
        while (authentication.delegate != null && System.currentTimeMillis() < deadline) {
            System.gc()
            Thread.sleep(10)
        }

        assertThat(authentication.delegate).isNull()
    }

    @Test
    fun `nothing is reported without a delegate`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager)

        tokenManager.saveTokens("user", "access", null, null)
        tokenManager.reportRefreshFailure("user", networkError)

        assertThat(delegate.tokens).isEmpty()
        assertThat(delegate.errors).isEmpty()
    }

    // endregion

    // region logIn / logOut

    @Test
    fun `logOut results are delivered on the main thread`() = runTest {
        val authentication = authentication(tokenManager())
        val callback = RecordingCallback()
        deferMain = true

        authentication.logOut(callback)
        flows.pendingLogOut!!.onError(networkError)
        assertThat(callback.errors).isEmpty()

        mainQueue.forEach { it() }
        assertThat(callback.errors).containsExactly(networkError)
    }

    @Test
    fun `logIn results are delivered on the main thread`() = runTest {
        val authentication = authentication(tokenManager())
        val callback = RecordingCallback()
        deferMain = true

        authentication.logIn(Identity.anonymous, callback)
        flows.pendingLogIn!!.onReceived(customerInfo)
        assertThat(callback.infos).isEmpty()

        mainQueue.forEach { it() }
        assertThat(callback.infos).containsExactly(customerInfo)
    }

    @Test
    fun `logIn passes the identity through and returns the CustomerInfo`() = runTest {
        val callback = RecordingCallback()

        authentication(tokenManager()).logIn(Identity.anonymous, callback)
        flows.pendingLogIn!!.onReceived(customerInfo)

        assertThat(flows.loggedInWith).isEqualTo(Identity.anonymous)
        assertThat(callback.infos).containsExactly(customerInfo)
    }

    @Test
    fun `logOut returns the anonymous user's CustomerInfo`() = runTest {
        val callback = RecordingCallback()

        authentication(tokenManager()).logOut(callback)
        flows.pendingLogOut!!.onReceived(customerInfo)

        assertThat(callback.infos).containsExactly(customerInfo)
    }

    @Test
    fun `logOut is unsupported in custom entitlement computation mode`() = runTest {
        val callback = RecordingCallback()

        authentication(tokenManager(), customEntitlementComputation = true).logOut(callback)

        assertThat(flows.pendingLogOut).isNull()
        assertThat(callback.errors.single().code).isEqualTo(PurchasesErrorCode.UnsupportedError)
    }

    @Test
    fun `currentAccessToken is the current user's`() = runTest {
        val tokenManager = tokenManager()
        tokenManager.saveTokens("user", "user-access", null, null)
        tokenManager.saveTokens("other", "other-access", null, null)
        val authentication = authentication(tokenManager)

        assertThat(authentication.currentAccessToken).isEqualTo("user-access")
        currentUser = "other"
        assertThat(authentication.currentAccessToken).isEqualTo("other-access")
        currentUser = "nobody"
        assertThat(authentication.currentAccessToken).isNull()
    }

    // endregion

    // region coroutines

    @Test
    fun `awaitLogIn returns the CustomerInfo`() = runTest {
        val authentication = authentication(tokenManager())
        val result = async { authentication.awaitLogIn(Identity.anonymous) }
        runCurrent()

        flows.pendingLogIn!!.onReceived(customerInfo)

        assertThat(result.await()).isEqualTo(customerInfo)
    }

    @Test
    fun `awaitLogIn throws the login error`() = runTest {
        val authentication = authentication(tokenManager())
        val result = async { runCatching { authentication.awaitLogIn(Identity.anonymous) } }
        runCurrent()

        flows.pendingLogIn!!.onError(networkError)

        assertThat((result.await().exceptionOrNull() as PurchasesException).error).isEqualTo(networkError)
    }

    @Test
    fun `awaitLogOut returns the anonymous user's CustomerInfo, or throws its error`() = runTest {
        val authentication = authentication(tokenManager())
        val success = async { authentication.awaitLogOut() }
        runCurrent()
        flows.pendingLogOut!!.onReceived(customerInfo)
        assertThat(success.await()).isEqualTo(customerInfo)

        val failure = async { runCatching { authentication.awaitLogOut() } }
        runCurrent()
        flows.pendingLogOut!!.onError(networkError)
        assertThat((failure.await().exceptionOrNull() as PurchasesException).error).isEqualTo(networkError)
    }

    @Test
    fun `cancelling awaitLogIn leaves the login to finish and passive reporting intact`() = runTest {
        val tokenManager = tokenManager()
        val authentication = authentication(tokenManager).apply { delegate = this@AuthenticationTest.delegate }
        val job = launch { authentication.awaitLogIn(Identity.anonymous) }
        runCurrent()

        job.cancelAndJoin()
        flows.pendingLogIn!!.onReceived(customerInfo)

        assertThat(job.isCancelled).isTrue()
        tokenManager.reportRefreshFailure("user", networkError)
        assertThat(delegate.errors).containsExactly(networkError)
    }

    // endregion

    private val networkError = PurchasesError(PurchasesErrorCode.NetworkError)

    private fun authentication(tokenManager: TokenManager, customEntitlementComputation: Boolean = false) =
        Authentication(
            reporter,
            tokenManager,
            currentAppUserID = { currentUser },
            flows = flows,
            customEntitlementComputation = customEntitlementComputation,
            mainDispatch = mainDispatch,
        ).also {
            tokens = tokenManager
            tokenManager.setTokenUpdateListener { update: TokenUpdate -> reporter.report(update) }
        }

    private fun TestScope.tokenManager(): TokenManager {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return TokenManager(
            ApplicationProvider.getApplicationContext(),
            "test_api_key",
            enabled = true,
            computationDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        ).also { advanceUntilIdle() }
    }

    private class FakeFlows : Authentication.IdentityFlows {
        var loggedInWith: Identity? = null
        var pendingLogIn: ReceiveCustomerInfoCallback? = null
        var pendingLogOut: ReceiveCustomerInfoCallback? = null

        override fun logIn(identity: Identity, callback: ReceiveCustomerInfoCallback) {
            loggedInWith = identity
            pendingLogIn = callback
        }

        override fun logOut(callback: ReceiveCustomerInfoCallback) {
            pendingLogOut = callback
        }
    }

    private class RecordingDelegate : AuthenticationDelegate {
        val errors = mutableListOf<PurchasesError>()
        val tokens = mutableListOf<String?>()

        override fun authenticatorDidEncounterError(error: PurchasesError) {
            errors.add(error)
        }

        override fun authenticatorDidUpdateAccessToken(newAccessToken: String?) {
            tokens.add(newAccessToken)
        }
    }

    private class RecordingCallback : ReceiveCustomerInfoCallback {
        val infos = mutableListOf<CustomerInfo>()
        val errors = mutableListOf<PurchasesError>()

        override fun onReceived(customerInfo: CustomerInfo) {
            infos.add(customerInfo)
        }

        override fun onError(error: PurchasesError) {
            errors.add(error)
        }
    }
}
