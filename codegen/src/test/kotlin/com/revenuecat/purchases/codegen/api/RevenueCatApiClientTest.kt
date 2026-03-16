package com.revenuecat.purchases.codegen.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for RevenueCatApiClient using MockWebServer as a real HTTP server.
 *
 * Note: the production code uses Thread.sleep() for REQUEST_DELAY_MS (500ms) between
 * paginated pages, and for INITIAL_BACKOFF_MS (1000ms) on 429 retries. Tests that exercise
 * retries use a small backoff_ms value in the response body to keep execution fast.
 */
class RevenueCatApiClientTest {

    private val server = MockWebServer()
    private lateinit var client: RevenueCatApiClient

    @BeforeTest
    fun setUp() {
        server.start()
        // Trim the trailing slash that server.url() appends so it matches how baseUrl is used
        val baseUrl = server.url("/v2").toString().trimEnd('/')
        client = RevenueCatApiClient("sk_test", baseUrl = baseUrl)
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    // --- fetchEntitlements ---

    @Test
    fun `fetchEntitlements returns items from single page response`() {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "items": [
                    {"id": "ent_1", "lookup_key": "premium_access", "display_name": "Premium"},
                    {"id": "ent_2", "lookup_key": "basic", "display_name": "Basic"}
                  ],
                  "next_page": null
                }
                """.trimIndent()
            )
        )

        val result = client.fetchEntitlements("proj_123")

        assertEquals(2, result.size)
        assertEquals("ent_1", result[0].id)
        assertEquals("premium_access", result[0].lookupKey)
        assertEquals("Premium", result[0].displayName)
        assertEquals("basic", result[1].lookupKey)
    }

    @Test
    fun `fetchEntitlements returns empty list when items array is empty`() {
        server.enqueue(MockResponse().setBody("""{"items": [], "next_page": null}"""))

        val result = client.fetchEntitlements("proj_123")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `fetchEntitlements concatenates items from multiple pages`() {
        val page2Url = server.url("/v2/projects/proj_123/entitlements?page=2").toString()
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"id": "ent_1", "lookup_key": "premium", "display_name": "Premium"}], "next_page": "$page2Url"}"""
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"id": "ent_2", "lookup_key": "basic", "display_name": "Basic"}], "next_page": null}"""
            )
        )

        val result = client.fetchEntitlements("proj_123")

        assertEquals(2, result.size)
        assertEquals("premium", result[0].lookupKey)
        assertEquals("basic", result[1].lookupKey)
    }

    @Test
    fun `fetchEntitlements sends Authorization Bearer header on every request`() {
        server.enqueue(MockResponse().setBody("""{"items": [], "next_page": null}"""))

        client.fetchEntitlements("proj_123")

        val request = server.takeRequest()
        assertEquals("Bearer sk_test", request.getHeader("Authorization"))
    }

    @Test
    fun `fetchEntitlements retries on 429 using backoff_ms from response body`() {
        // Use a very small backoff_ms so the test doesn't take long
        server.enqueue(
            MockResponse().setResponseCode(429).setBody("""{"backoff_ms": 10}""")
        )
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"id": "ent_1", "lookup_key": "premium", "display_name": "Premium"}], "next_page": null}"""
            )
        )

        val result = client.fetchEntitlements("proj_123")

        assertEquals(1, result.size)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `fetchEntitlements throws immediately on non-retriable HTTP error`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        assertFailsWith<RuntimeException> {
            client.fetchEntitlements("proj_123")
        }

        // Should NOT retry a non-retriable error
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `fetchEntitlements retries on 500 errors`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"id": "ent_1", "lookup_key": "premium", "display_name": "Premium"}], "next_page": null}"""
            )
        )

        val result = client.fetchEntitlements("proj_123")

        assertEquals(1, result.size)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `fetchEntitlements treats string-null next_page as end of pagination`() {
        server.enqueue(MockResponse().setBody("""{"items": [], "next_page": "null"}"""))

        client.fetchEntitlements("proj_123")

        // Must not make a second request
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `fetchEntitlements treats empty string next_page as end of pagination`() {
        server.enqueue(MockResponse().setBody("""{"items": [], "next_page": ""}"""))

        client.fetchEntitlements("proj_123")

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `fetchEntitlements uses lookup_key as displayName when display_name is absent`() {
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"id": "ent_1", "lookup_key": "premium"}], "next_page": null}"""
            )
        )

        val result = client.fetchEntitlements("proj_123")

        assertEquals("premium", result[0].displayName)
    }

    // --- fetchOfferings ---

    @Test
    fun `fetchOfferings hydrates packages for each offering`() {
        // First response: offerings list
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "items": [
                    {"id": "off_1", "lookup_key": "default", "display_name": "Default", "is_current": true}
                  ],
                  "next_page": null
                }
                """.trimIndent()
            )
        )
        // Second response: packages for off_1
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "items": [
                    {"id": "pkg_1", "lookup_key": "${'$'}rc_monthly", "display_name": "Monthly"},
                    {"id": "pkg_2", "lookup_key": "${'$'}rc_annual", "display_name": "Annual"}
                  ],
                  "next_page": null
                }
                """.trimIndent()
            )
        )

        val result = client.fetchOfferings("proj_123")

        assertEquals(1, result.size)
        assertEquals("default", result[0].lookupKey)
        assertTrue(result[0].isCurrent)
        assertEquals(2, result[0].packages.size)
        assertEquals("\$rc_monthly", result[0].packages[0].lookupKey)
        assertEquals("Monthly", result[0].packages[0].displayName)
        assertEquals("\$rc_annual", result[0].packages[1].lookupKey)
    }

    @Test
    fun `fetchOfferings returns offering with empty packages list`() {
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"id": "off_1", "lookup_key": "default", "display_name": "Default", "is_current": false}], "next_page": null}"""
            )
        )
        // Empty packages response
        server.enqueue(MockResponse().setBody("""{"items": [], "next_page": null}"""))

        val result = client.fetchOfferings("proj_123")

        assertEquals(1, result.size)
        assertTrue(result[0].packages.isEmpty())
    }
}
