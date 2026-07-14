package com.revenuecat.purchases.perf

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PerfFixturesDispatcherTest {
    private val server = MockWebServer()

    @After fun tearDown() { server.shutdown() }

    @Test
    fun returnsMatchingFixtureBody() {
        val base = "http://127.0.0.1:1/"
        server.dispatcher = PerfFixtures.dispatcher(mockBaseUrl = base)
        server.start()
        val client = OkHttpClient()
        val entry = PerfFixtures.loadManifest().first()
        val resp = client.newCall(
            Request.Builder().url(server.url("/v1${entry.match}")).build(),
        ).execute()
        assertThat(resp.code).isEqualTo(entry.status)
    }

    // The SDK's getOfferings path never requests /blob (assets are inlined), so this fixture
    // is not exercised by request-count assertions elsewhere. It exists purely to genuinely
    // exercise PerfFixtures.dispatcher's PLACEHOLDER_HOST rewrite, since no other fixture
    // committed under perf-fixtures/ actually contains the placeholder host string.
    @Test
    fun rewritesPlaceholderHostToMockUrl() {
        server.start()
        val mockBaseUrl = server.url("/").toString()
        server.dispatcher = PerfFixtures.dispatcher(mockBaseUrl = mockBaseUrl)
        val client = OkHttpClient()
        val resp = client.newCall(
            Request.Builder().url(server.url("/blob/paywall/image")).build(),
        ).execute()
        val body = resp.body!!.string()
        assertThat(body).contains(mockBaseUrl)
        assertThat(body).doesNotContain("PERF_MOCK_HOST")
    }

    @Test
    fun unmatchedPathReturns404() {
        server.dispatcher = PerfFixtures.dispatcher(mockBaseUrl = "http://127.0.0.1:1/")
        server.start()
        val client = OkHttpClient()
        val resp = client.newCall(
            Request.Builder().url(server.url("/nothing/here")).build(),
        ).execute()
        assertThat(resp.code).isEqualTo(404)
    }

    @Test
    fun routesAmbiguousSubscriberPathsCorrectly() {
        server.dispatcher = PerfFixtures.dispatcher(mockBaseUrl = "http://127.0.0.1:1/")
        server.start()
        val client = OkHttpClient()

        val offeringsResp = client.newCall(
            Request.Builder().url(server.url("/v1/subscribers/u/offerings")).build(),
        ).execute()
        assertThat(offeringsResp.body!!.string()).contains("offering_a")

        val productsResp = client.newCall(
            Request.Builder().url(server.url("/rcbilling/v1/subscribers/u/products?id=x")).build(),
        ).execute()
        assertThat(productsResp.body!!.string()).contains("monthly_freetrial")
    }
}
