package com.revenuecat.purchases

import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
class ForceServerErrorModeTest {

    private val baseURL = URL("https://api.revenuecat.com")
    private val remoteConfig = Endpoint.GetRemoteConfig(domain = "acme")
    private val remoteConfigFallback = Endpoint.GetRemoteConfigFallback(domain = "acme")
    private val unrelatedEndpoint = Endpoint.LogIn

    @Test
    fun `REMOTE_CONFIG_NOT_FOUND fakes a 404 for the remote-config endpoints`() {
        val strategy = ForceServerErrorMode.REMOTE_CONFIG_NOT_FOUND.toForceServerErrorStrategy()

        val configResult = strategy.fakeResponseWithoutPerformingRequest(baseURL, remoteConfig)
        val fallbackResult = strategy.fakeResponseWithoutPerformingRequest(baseURL, remoteConfigFallback)

        assertThat(configResult?.responseCode).isEqualTo(RCHTTPStatusCodes.NOT_FOUND)
        assertThat(fallbackResult?.responseCode).isEqualTo(RCHTTPStatusCodes.NOT_FOUND)
    }

    @Test
    fun `REMOTE_CONFIG_NOT_FOUND does not touch unrelated endpoints`() {
        val strategy = ForceServerErrorMode.REMOTE_CONFIG_NOT_FOUND.toForceServerErrorStrategy()

        assertThat(strategy.fakeResponseWithoutPerformingRequest(baseURL, unrelatedEndpoint)).isNull()
        assertThat(strategy.shouldForceServerError(baseURL, remoteConfig)).isFalse()
    }

    @Test
    fun `REMOTE_CONFIG_NETWORK_ERROR forces an error only for the remote-config endpoints`() {
        val strategy = ForceServerErrorMode.REMOTE_CONFIG_NETWORK_ERROR.toForceServerErrorStrategy()

        assertThat(strategy.shouldForceServerError(baseURL, remoteConfig)).isTrue()
        assertThat(strategy.shouldForceServerError(baseURL, remoteConfigFallback)).isTrue()
        assertThat(strategy.shouldForceServerError(baseURL, unrelatedEndpoint)).isFalse()
        // Routes to an unreachable host so the request raises a transport error.
        assertThat(strategy.serverErrorURL).endsWith(".invalid/")
        assertThat(strategy.fakeResponseWithoutPerformingRequest(baseURL, remoteConfig)).isNull()
    }
}
