package com.revenuecat.purchases.perf

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class NetworkProfileTest {
    private val server = MockWebServer()
    @After fun tearDown() { server.shutdown() }

    private fun okDispatcher() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(200).setBody("{}")
    }

    @Test
    fun flakyFailsOnlyMatchingPath() {
        server.dispatcher = NetworkProfile.FLAKY.decorate(okDispatcher(), failMatch = "/blob")
        server.start()
        val client = okhttp3.OkHttpClient()
        val ok = client.newCall(okhttp3.Request.Builder().url(server.url("/v1/offerings")).build()).execute()
        val bad = client.newCall(okhttp3.Request.Builder().url(server.url("/blob/x")).build()).execute()
        assertThat(ok.code).isEqualTo(200)
        assertThat(bad.code).isEqualTo(500)
    }

    @Test
    fun badAddsDelay() {
        server.dispatcher = NetworkProfile.BAD.decorate(okDispatcher())
        server.start()
        val start = System.nanoTime()
        okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(server.url("/v1/offerings")).build()).execute()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertThat(elapsedMs).isGreaterThanOrEqualTo(NetworkProfile.BAD.perRequestDelayMs - 50)
    }
}
