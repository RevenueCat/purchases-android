package com.revenuecat.purchases.common

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests for HTTPClient's IAM path routing (IAM phase 5, step 21): whether a request is sent to an
 * endpoint's legacy `pathTemplate` or its IAM-namespaced `iamPathTemplate`, based solely on whether
 * the injected [TokenManager] is enabled -- unlike the header override (step 20), this doesn't
 * depend on an `appUserID` or a stored token, since path routing is an app-wide switch, not a
 * per-user one.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class HTTPClientIAMPathRoutingTest : BaseHTTPClientTest() {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val headers = mapOf("Authorization" to "Bearer api-key")

    private fun tokenManager(enabled: Boolean): TokenManager =
        TokenManager(context, "test_api_key", enabled = enabled, scope = testScope())

    @Before
    fun setupSigningManager() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
    }

    // region no tokenManager / disabled -- unchanged (regression)

    @Test
    fun `no tokenManager injected uses the legacy path`() {
        client = createClient(tokenManager = null)
        enqueue("/v1/subscribers/user", HTTPResult.createResult())

        client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers)

        assertThat(server.takeRequest().path).isEqualTo("/v1/subscribers/user")
    }

    @Test
    fun `a disabled tokenManager uses the legacy path`() {
        client = createClient(tokenManager = tokenManager(enabled = false))
        enqueue("/v1/subscribers/user", HTTPResult.createResult())

        client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers)

        assertThat(server.takeRequest().path).isEqualTo("/v1/subscribers/user")
    }

    // endregion

    // region enabled -- routed to the IAM path

    @Test
    fun `an enabled tokenManager routes GetCustomerInfo to its IAM path`() {
        client = createClient(tokenManager = tokenManager(enabled = true))
        enqueue("/v1/customer", HTTPResult.createResult())

        client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers)

        assertThat(server.takeRequest().path).isEqualTo("/v1/customer")
    }

    @Test
    fun `an enabled tokenManager routes GetOfferings to its IAM path`() {
        client = createClient(tokenManager = tokenManager(enabled = true))
        enqueue("/v1/customer/offerings", HTTPResult.createResult())

        client.performRequest(baseURL, Endpoint.GetOfferings("user"), null, null, headers)

        assertThat(server.takeRequest().path).isEqualTo("/v1/customer/offerings")
    }

    @Test
    fun `an enabled tokenManager still uses the legacy path for an endpoint with no IAM path`() {
        client = createClient(tokenManager = tokenManager(enabled = true))
        enqueue(Endpoint.LogIn.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, Endpoint.LogIn, null, null, headers)

        assertThat(server.takeRequest().path).isEqualTo(Endpoint.LogIn.getPath())
    }

    // endregion

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
}
