package com.revenuecat.purchases.perf

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class DispatcherDecoratorsTest {
    private val server = MockWebServer()
    @After fun tearDown() { server.shutdown() }

    private fun okDispatcher() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(200).setBody("{}")
    }

    @Test
    fun withPathFailureFailsOnlyMatchingPath() {
        server.dispatcher = okDispatcher().withPathFailure("/config")
        server.start()
        val client = OkHttpClient()

        val configResp = client.newCall(Request.Builder().url(server.url("/v1/config/app")).build()).execute()
        val offeringsResp = client.newCall(Request.Builder().url(server.url("/v1/offerings")).build()).execute()

        assertThat(configResp.code).isEqualTo(500)
        assertThat(offeringsResp.code).isEqualTo(200)
        assertThat(offeringsResp.body!!.string()).isEqualTo("{}")
    }
}
