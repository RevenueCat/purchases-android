package com.revenuecat.purchases

import junit.framework.Assert
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONException
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HTTPClientTest {

    companion object {
        @JvmStatic
        private lateinit var server: MockWebServer
        @JvmStatic
        private lateinit var baseURL: URL

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = MockWebServer()
            baseURL = server.url("/v1").url()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            server.shutdown()
        }
    }

    @Test
    fun canBeCreated() {
        HTTPClient(baseURL)
    }

    @Test
    fun canPerformASimpleGet() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        HTTPClient(baseURL)
            .apply {
                performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))
            }

        val request = server.takeRequest()
        Assert.assertEquals(request.method, "GET")
        Assert.assertEquals(request.path, "/v1/resource")
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, InterruptedException::class)
    fun forwardsTheResponseCode() {
        val response = MockResponse().setBody("{}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(baseURL)
        val result = client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        server.takeRequest()

        Assert.assertEquals(223, result.responseCode)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, InterruptedException::class, JSONException::class)
    fun parsesTheBody() {
        val response = MockResponse().setBody("{'response': 'OK'}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(baseURL)
        val result = client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        server.takeRequest()

        Assert.assertEquals("OK", result.body!!.getString("response"))
    }

    // Errors

    @Test(expected = HTTPClient.HTTPErrorException::class)
    @Throws(HTTPClient.HTTPErrorException::class, InterruptedException::class)
    fun reWrapsBadJSONError() {
        val response = MockResponse().setBody("not uh jason")
        server.enqueue(response)

        val client = HTTPClient(baseURL)
        try {
            client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))
        } finally {
            server.takeRequest()
        }

    }

    // Headers
    @Test
    @Throws(HTTPClient.HTTPErrorException::class, InterruptedException::class)
    fun addsHeadersToRequest() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        val client = HTTPClient(baseURL)
        client.performRequest("/resource", null as Map<*, *>?, headers)

        val request = server.takeRequest()
        Assert.assertEquals(request.getHeader("Authentication"), "Bearer todd")
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, InterruptedException::class)
    fun addsDefaultHeadersToRequest() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val client = HTTPClient(baseURL)
        client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        val request = server.takeRequest()

        Assert.assertEquals(request.getHeader("Content-Type"), "application/json")
        Assert.assertEquals(request.getHeader("X-Platform"), "android")
        Assert.assertEquals(
            request.getHeader("X-Platform-Version"),
            Integer.toString(android.os.Build.VERSION.SDK_INT)
        )
        Assert.assertEquals(request.getHeader("X-Version"), Purchases.getFrameworkVersion())
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, InterruptedException::class, JSONException::class)
    fun addsPostBody() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        val client = HTTPClient(baseURL)
        client.performRequest("/resource", body, mapOf("" to ""))

        val request = server.takeRequest()
        Assert.assertEquals("POST", request.method)
        assertNotNull(request.body)
        assertTrue(request.body.size() > 0)
    }

}
