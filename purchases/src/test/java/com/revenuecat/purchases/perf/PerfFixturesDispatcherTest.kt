package com.revenuecat.purchases.perf

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PerfFixturesDispatcherTest {
    private val server = MockWebServer()

    @After fun tearDown() { server.shutdown() }

    @Test
    fun returnsMatchingFixtureBodyAndRewritesPlaceholderHost() {
        val base = "http://127.0.0.1:1/"
        server.dispatcher = PerfFixtures.dispatcher(mockBaseUrl = base)
        server.start()
        val client = okhttp3.OkHttpClient()
        val entry = PerfFixtures.loadManifest().first()
        val resp = client.newCall(
            okhttp3.Request.Builder().url(server.url("/v1${entry.match}")).build(),
        ).execute()
        assertThat(resp.code).isEqualTo(entry.status)
        assertThat(resp.body!!.string()).doesNotContain("PERF_MOCK_HOST")
    }
}
