package com.revenuecat.purchases.common.offerings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.BaseHTTPClientTest
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.caching.PayloadFileStore
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class OfferingsResponseETagCacheTest : BaseHTTPClientTest() {

    private companion object {
        const val API_KEY = "test_api_key"
        const val ETAG = "an-etag"
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `an offerings response is ETag-cached without the contents of paywall components`() {
        val payload = OfferingsResponsePayloads.withHeavyPaywallComponents(targetBytes = 256 * 1024)
        val eTagDirectory = temporaryFolder.newFolder()
        val backend = createBackend(eTagDirectory)
        server.enqueue(MockResponse().setBody(payload).setHeader(HTTPResult.ETAG_HEADER_NAME, ETAG))
        server.enqueue(MockResponse().setResponseCode(304).setHeader(HTTPResult.ETAG_HEADER_NAME, ETAG))

        val (fetchedResponse, fetchedPayload) = fetchOfferings(backend)
        val storedBytes = eTagDirectory.walkTopDown().filter(File::isFile).sumOf(File::length)
        val (notModifiedResponse, notModifiedPayload) = fetchOfferings(backend)

        assertThat(fetchedPayload.length).isLessThan(payload.length / 10)
        assertThat(storedBytes).isEqualTo(fetchedPayload.toByteArray().size.toLong())
        assertThat(server.takeRequest().getHeader(HTTPResult.ETAG_HEADER_NAME)).isEmpty()
        assertThat(server.takeRequest().getHeader(HTTPResult.ETAG_HEADER_NAME)).isEqualTo(ETAG)
        assertThat(notModifiedPayload).isEqualTo(fetchedPayload)
        assertThat(notModifiedResponse.toString()).isEqualTo(fetchedResponse.toString())
        assertThat(notModifiedResponse.getJSONArray("offerings").length())
            .isEqualTo(OfferingsResponsePayloads.OFFERING_COUNT)
    }

    private fun fetchOfferings(backend: Backend): Pair<JSONObject, String> {
        lateinit var result: Pair<JSONObject, String>
        backend.getOfferings(
            "appUserID",
            appInBackground = false,
            onSuccess = { body, _, responsePayload -> result = body to responsePayload },
            onError = { error, _ -> error("Unexpected error fetching offerings: $error") },
        )
        return result
    }

    private fun createBackend(eTagDirectory: File): Backend {
        val appConfig = createAppConfig()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("offerings_response_etag_cache_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val httpClient = createClient(
            appConfig = appConfig,
            eTagManager = ETagManager(context, lazy { prefs }, payloadStore = PayloadFileStore(eTagDirectory)),
            signingManager = SigningManager(SignatureVerificationMode.Disabled, appConfig, API_KEY),
        )
        val dispatcher = SyncDispatcher()
        return Backend(
            appConfig,
            dispatcher,
            dispatcher,
            httpClient,
            BackendHelper(API_KEY, dispatcher, appConfig, httpClient),
        )
    }
}
