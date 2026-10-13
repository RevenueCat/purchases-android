@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.identity

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.networking.TokenManager.TokenUpdate
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val reporter = AuthenticationReporter(mainDispatch = { it() })
    private var currentUser = "user"

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
    fun `a refresh failure during passive use reaches the delegate`() = runTest {
        val tokenManager = tokenManager()
        authentication(tokenManager).delegate = delegate

        tokenManager.reportRefreshFailure(networkError)

        assertThat(delegate.errors).containsExactly(networkError)
    }

    @Test
    fun `a refresh failure during an explicit login is reported only through the login`() = runTest {
        val tokenManager = tokenManager()
        val authentication = authentication(tokenManager).apply { delegate = this@AuthenticationTest.delegate }
        val callback = RecordingCallback()

        authentication.logIn(Identity.anonymous, callback)
        tokenManager.reportRefreshFailure(networkError)
        flows.pendingLogIn!!.onError(networkError)

        assertThat(delegate.errors).isEmpty()
        assertThat(callback.errors).containsExactly(networkError)

        // Once the explicit call has finished, passive failures are reported again.
        tokenManager.reportRefreshFailure(networkError)
        assertThat(delegate.errors).containsExactly(networkError)
    }

    @Test
    fun `a refresh failure during an explicit logout is reported only through the logout`() = runTest {
        val tokenManager = tokenManager()
        val authentication = authentication(tokenManager).apply { delegate = this@AuthenticationTest.delegate }

        authentication.logOut(RecordingCallback())
        tokenManager.reportRefreshFailure(networkError)
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
        tokenManager.reportRefreshFailure(networkError)

        assertThat(delegate.tokens).isEmpty()
        assertThat(delegate.errors).isEmpty()
    }

    // endregion

    // region logIn / logOut

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

    private val networkError = PurchasesError(PurchasesErrorCode.NetworkError)

    private fun authentication(tokenManager: TokenManager, customEntitlementComputation: Boolean = false) =
        Authentication(
            reporter,
            tokenManager,
            currentAppUserID = { currentUser },
            flows = flows,
            customEntitlementComputation = customEntitlementComputation,
            mainDispatch = { it() },
        ).also { tokenManager.setTokenUpdateListener { update: TokenUpdate -> reporter.report(update) } }

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
