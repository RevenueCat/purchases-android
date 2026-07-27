package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.APISourceFailover.FailureDecision
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSource
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceProvider
import com.revenuecat.purchases.utils.TestUrlConnection
import com.revenuecat.purchases.utils.TestUrlConnectionFactory
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class APISourceFailoverTest {

    private class FakeSourceProvider(urls: List<String>) : RemoteConfigSourceProvider {
        private val sources = urls.map { RemoteConfigSource(url = it, priority = 0, weight = 0) }
        private var index = 0
        private var token = 0
        val unhealthyReports = mutableListOf<RemoteConfigSourceHandle>()

        override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle? =
            sources.getOrNull(index)?.let { RemoteConfigSourceHandle(purpose, it, token) }

        override fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
            unhealthyReports.add(handle)
            if (handle.token == token) {
                index++
                token++
            }
        }

        override fun restart(purpose: RemoteConfigSourceHandle.Purpose) {
            index = 0
            token++
        }

        override fun restartIfExhausted(purpose: RemoteConfigSourceHandle.Purpose): Boolean {
            if (index < sources.size) return false
            restart(purpose)
            return true
        }
    }

    private val defaultBaseURL = URL(AppConfig.baseUrlString)
    private val eligibleEndpoint = Endpoint.GetCustomerInfo("test-user-id")

    private lateinit var appConfig: AppConfig

    @Before
    fun setUp() {
        appConfig = mockk()
        every { appConfig.usesRemoteConfigAPISources } returns true
    }

    private fun healthConnection(responseCode: Int) =
        TestUrlConnection(responseCode, ByteArrayInputStream(ByteArray(0)))

    private fun failover(
        provider: RemoteConfigSourceProvider,
        connectionFactory: TestUrlConnectionFactory = TestUrlConnectionFactory(
            connectionProvider = { healthConnection(200) },
        ),
    ) = APISourceFailover(appConfig, provider, SourceHealthChecker(connectionFactory))

    // region currentSource eligibility

    @Test
    fun `currentSource resolves the provider's current source when eligible`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/"))
        val source = failover(provider).currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)
        assertThat(source?.url).isEqualTo(URL("https://a.revenuecat.com/"))
        assertThat(source?.handle?.url).isEqualTo("https://a.revenuecat.com/")
    }

    @Test
    fun `currentSource is null when the dangerous setting is disabled`() {
        every { appConfig.usesRemoteConfigAPISources } returns false
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/"))
        assertThat(failover(provider).currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false))
            .isNull()
    }

    @Test
    fun `currentSource is null for endpoint fallback attempts`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/"))
        assertThat(failover(provider).currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = true))
            .isNull()
    }

    @Test
    fun `currentSource is null for endpoints that do not use API sources`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/"))
        assertThat(failover(provider).currentSource(Endpoint.PostDiagnostics, defaultBaseURL, isFallbackAttempt = false))
            .isNull()
    }

    @Test
    fun `currentSource is null when the base URL is not the default host`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/"))
        val proxyURL = URL("https://proxy.example.com/")
        assertThat(failover(provider).currentSource(eligibleEndpoint, proxyURL, isFallbackAttempt = false))
            .isNull()
    }

    @Test
    fun `currentSource is null when the provider is exhausted`() {
        val provider = FakeSourceProvider(emptyList())
        assertThat(failover(provider).currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false))
            .isNull()
    }

    @Test
    fun `currentSource skips and reports sources with malformed urls`() {
        val provider = FakeSourceProvider(listOf("not a url", "https://b.revenuecat.com/"))
        val source = failover(provider).currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)
        assertThat(source?.url).isEqualTo(URL("https://b.revenuecat.com/"))
        assertThat(provider.unhealthyReports.map { it.url }).containsExactly("not a url")
    }

    @Test
    fun `currentSource is null when every source url is malformed`() {
        val provider = FakeSourceProvider(listOf("not a url", "also not a url"))
        assertThat(failover(provider).currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false))
            .isNull()
        assertThat(provider.unhealthyReports).hasSize(2)
    }

    // endregion

    // region onRequestFailure

    @Test
    fun `onRequestFailure does not fail over when the source's health check passes`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/", "https://b.revenuecat.com/"))
        val factory = TestUrlConnectionFactory(connectionProvider = { healthConnection(200) })
        val failover = failover(provider, factory)

        val source = failover.currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)!!
        val decision = failover.onRequestFailure(source)

        assertThat(decision).isEqualTo(FailureDecision.SourceHealthy)
        assertThat(provider.unhealthyReports).isEmpty()
    }

    @Test
    fun `onRequestFailure fails over when the health check returns a non-2xx response`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/", "https://b.revenuecat.com/"))
        val factory = TestUrlConnectionFactory(connectionProvider = { healthConnection(503) })
        val failover = failover(provider, factory)

        val source = failover.currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)!!
        val decision = failover.onRequestFailure(source)

        assertThat(decision).isInstanceOf(FailureDecision.RetryNextSource::class.java)
        assertThat((decision as FailureDecision.RetryNextSource).next.url)
            .isEqualTo(URL("https://b.revenuecat.com/"))
        assertThat(provider.unhealthyReports.map { it.url }).containsExactly("https://a.revenuecat.com/")
    }

    @Test
    fun `onRequestFailure fails over when the health check cannot connect`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/", "https://b.revenuecat.com/"))
        val factory = TestUrlConnectionFactory(connectionProvider = { throw IOException("unreachable") })
        val failover = failover(provider, factory)

        val source = failover.currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)!!
        val decision = failover.onRequestFailure(source)

        assertThat(decision).isInstanceOf(FailureDecision.RetryNextSource::class.java)
        assertThat((decision as FailureDecision.RetryNextSource).next.url)
            .isEqualTo(URL("https://b.revenuecat.com/"))
    }

    @Test
    fun `onRequestFailure reports exhaustion once the last source fails its health check`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/"))
        val factory = TestUrlConnectionFactory(connectionProvider = { throw IOException("unreachable") })
        val failover = failover(provider, factory)

        val source = failover.currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)!!
        assertThat(failover.onRequestFailure(source)).isEqualTo(FailureDecision.SourcesExhausted)
        assertThat(provider.unhealthyReports.map { it.url }).containsExactly("https://a.revenuecat.com/")
    }

    @Test
    fun `onRequestFailure health-checks the current source's health path`() {
        val provider = FakeSourceProvider(listOf("https://a.revenuecat.com/", "https://b.revenuecat.com/"))
        val factory = TestUrlConnectionFactory(connectionProvider = { healthConnection(200) })
        val failover = failover(provider, factory)

        val source = failover.currentSource(eligibleEndpoint, defaultBaseURL, isFallbackAttempt = false)!!
        failover.onRequestFailure(source)

        assertThat(factory.createdConnections)
            .containsExactly("https://a.revenuecat.com/v1/health/connectivity")
    }

    // endregion
}
