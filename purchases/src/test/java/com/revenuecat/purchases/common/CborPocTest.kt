package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.networking.CborPocEntitlement
import com.revenuecat.purchases.common.networking.CborPocMetadata
import com.revenuecat.purchases.common.networking.CborPocResponse
import com.revenuecat.purchases.common.networking.Endpoint
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as AnnotationConfig

@OptIn(ExperimentalSerializationApi::class, InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@AnnotationConfig(manifest = AnnotationConfig.NONE)
internal class CborPocTest : BaseHTTPClientTest() {

    private val sampleResponse = CborPocResponse(
        requestDateMs = 1_717_400_000_000,
        subscriberId = "app_user_12345",
        isSandbox = false,
        entitlements = listOf(
            CborPocEntitlement(
                identifier = "premium",
                productIdentifier = "com.revenuecat.monthly",
                expiresDateMs = 1_720_000_000_000,
                isActive = true,
            ),
            CborPocEntitlement(
                identifier = "pro",
                productIdentifier = "com.revenuecat.annual",
                expiresDateMs = null,
                isActive = false,
            ),
        ),
        metadata = CborPocMetadata(
            schemaVersion = 3,
            tags = listOf("a", "b", "c"),
        ),
    )

    @Before
    fun setupClient() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        client = createClient()
    }

    @Test
    fun `requests CBOR and decodes a CBOR response body end-to-end through HTTPClient`() {
        val cborBytes = CborTools.cbor.encodeToByteArray(sampleResponse)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", HTTPClient.CBOR_CONTENT_TYPE)
                .setBody(Buffer().write(cborBytes)),
        )

        val result = client.performRequest(
            baseURL,
            Endpoint.LogIn,
            body = null,
            postFieldsToSign = null,
            requestHeaders = mapOf("" to ""),
            preferCbor = true,
        )

        // The request advertised CBOR support.
        val request = server.takeRequest()
        assertThat(request.getHeader("Accept")).isEqualTo(HTTPClient.CBOR_CONTENT_TYPE)

        // Raw bytes were carried through HTTPResult and decode back to the original model.
        assertThat(result.payloadBytes).isNotNull
        val decoded = CborPocResponse.fromHTTPResult(result)
        assertThat(decoded).isEqualTo(sampleResponse)
    }

    @Test
    fun `CBOR payload is smaller than JSON and both decode to the same model`() {
        val jsonString = JsonTools.json.encodeToString(sampleResponse)
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        val cborBytes = CborTools.cbor.encodeToByteArray(sampleResponse)

        val iterations = 10_000

        val jsonNanos = measureDecodeNanos(iterations) {
            JsonTools.json.decodeFromString<CborPocResponse>(jsonString)
        }
        val cborNanos = measureDecodeNanos(iterations) {
            CborTools.cbor.decodeFromByteArray<CborPocResponse>(cborBytes)
        }

        // Sanity: both formats round-trip to the same model.
        assertThat(JsonTools.json.decodeFromString<CborPocResponse>(jsonString)).isEqualTo(sampleResponse)
        assertThat(CborTools.cbor.decodeFromByteArray<CborPocResponse>(cborBytes)).isEqualTo(sampleResponse)

        // Print numbers for evaluation (no brittle timing assertions).
        println(
            "[CBOR PoC] payload size: JSON=${jsonBytes.size}B CBOR=${cborBytes.size}B " +
                "(${percentSmaller(jsonBytes.size, cborBytes.size)}% smaller)",
        )
        println(
            "[CBOR PoC] decode time over $iterations iterations: " +
                "JSON=${jsonNanos / 1_000_000.0}ms CBOR=${cborNanos / 1_000_000.0}ms",
        )

        assertThat(cborBytes.size).isLessThan(jsonBytes.size)
    }

    private inline fun measureDecodeNanos(iterations: Int, block: () -> Any): Long {
        // Warm up the JIT before measuring.
        repeat(iterations / 10) { block() }
        val start = System.nanoTime()
        repeat(iterations) { block() }
        return System.nanoTime() - start
    }

    private fun percentSmaller(jsonSize: Int, cborSize: Int): Int {
        return ((jsonSize - cborSize) * 100) / jsonSize
    }
}
