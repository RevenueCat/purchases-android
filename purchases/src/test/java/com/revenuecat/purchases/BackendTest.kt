//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.support.test.runner.AndroidJUnit4
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.assertj.core.api.Fail.fail
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.MalformedURLException
import java.util.HashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadLocalRandom

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BackendTest {
    private var mockClient: HTTPClient = mockk(relaxed = true)
    private var dispatcher: Dispatcher = SyncDispatcher()
    private var backend: Backend = Backend(
        API_KEY,
        dispatcher,
        mockClient
    )
    private val appUserID = "jerry"

    private var receivedPurchaserInfo: PurchaserInfo? = null
    private var receivedCode = -1
    private var receivedMessage: String? = null
    private var receivedEntitlements: Map<String, Entitlement>? = null

    private val onReceivePurchaserInfoSuccessHandler: (PurchaserInfo) -> Unit = { info ->
            this@BackendTest.receivedPurchaserInfo = info
        }

    private val onReceivePurchaserInfoErrorHandler: (PurchasesError) -> Unit = {
            this@BackendTest.receivedCode = -1
            this@BackendTest.receivedMessage = it.message
        }

    private val onReceiveEntitlementsSuccessHandler: (Map<String, Entitlement>) -> Unit = { entitlements ->
        this@BackendTest.receivedEntitlements = entitlements
    }

    private val onReceiveEntitlementsErrorHandler: (PurchasesError) -> Unit = {
        this@BackendTest.receivedCode = it.code
        this@BackendTest.receivedMessage = it.message
    }

    private inner class SyncDispatcher : Dispatcher(mockk()) {

        private var closed = false

        override fun enqueue(call: Dispatcher.AsyncCall) {
            if (closed) {
                throw RejectedExecutionException()
            }
            call.run()
        }

        override fun close() {
            closed = true
        }

        override fun isClosed(): Boolean {
            return closed
        }
    }

    @Test
    fun canBeCreated() {
        assertNotNull(backend)
    }

    @Throws(JSONException::class, HTTPClient.HTTPErrorException::class)
    private fun mockResponse(
        path: String,
        body: Map<String, Any?>?,
        responseCode: Int,
        clientException: HTTPClient.HTTPErrorException?,
        resultBody: String?
    ): PurchaserInfo {
        val info: PurchaserInfo = mockk()

        val result = HTTPClient.Result()
        result.responseCode = responseCode
        result.body = JSONObject(resultBody ?: "{}")

        val headers = HashMap<String, String>()
        headers["Authorization"] = "Bearer $API_KEY"

        mockkStatic("com.revenuecat.purchases.FactoriesKt")

        every {
            result.body!!.buildPurchaserInfo()
        } returns info

        val every = every {
            mockClient.performRequest(
                eq(path),
                (if (body == null) any() else eq(body)) as Map<*, *>,
                eq<Map<String, String>>(headers)
            )
        }

        if (clientException == null) {
            every returns result
        } else {
            every throws clientException
        }

        return info
    }

    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    private fun postReceipt(
        responseCode: Int,
        isRestore: Boolean,
        clientException: HTTPClient.HTTPErrorException?,
        resultBody: String?
    ): PurchaserInfo {

        val fetchToken = "fetch_token"
        val productID = "product_id"

        val body = mapOf(
            "fetch_token" to fetchToken,
            "app_user_id" to appUserID,
            "product_id" to productID,
            "is_restore" to isRestore
        )

        val info = mockResponse("/receipts", body, responseCode, clientException, resultBody)

        backend.postReceiptData(fetchToken, appUserID, productID, isRestore, onReceivePurchaserInfoSuccessHandler, onReceivePurchaserInfoErrorHandler)

        return info
    }

    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    private fun getPurchaserInfo(
        responseCode: Int,
        clientException: HTTPClient.HTTPErrorException?,
        resultBody: String?
    ): PurchaserInfo {
        val info =
            mockResponse("/subscribers/$appUserID", null, responseCode, clientException, resultBody)

        backend.getPurchaserInfo(appUserID, onReceivePurchaserInfoSuccessHandler, onReceivePurchaserInfoErrorHandler)

        return info
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun getSubscriberInfoCallsProperURL() {

        val info = getPurchaserInfo(200, null, null)

        assertNotNull(receivedPurchaserInfo)
        assertEquals(info, receivedPurchaserInfo)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun getSubscriberInfoFailsIfNot20X() {
        val failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1)

        getPurchaserInfo(failureCode, null, null)

        assertNull(receivedPurchaserInfo)
        assertNotNull(receivedMessage)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun clientErrorCallsErrorHandler() {
        getPurchaserInfo(200, HTTPClient.HTTPErrorException(0, ""), null)

        assertNull(receivedPurchaserInfo)
        assertNotNull(receivedMessage)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun attemptsToParseErrorMessageFromServer() {
        getPurchaserInfo(404, null, "{'message': 'Dude not found'}")

        assertNotNull(receivedMessage)
        assertTrue(receivedMessage!!.contains("Dude not found"))
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun handlesMissingMessageInErrorBody() {
        getPurchaserInfo(404, null, "{'no_message': 'Dude not found'}")
        assertNotNull(receivedMessage)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun postReceiptCallsProperURL() {
        val info = postReceipt(200, false, null, null)

        assertNotNull(receivedPurchaserInfo)
        assertSame(info, receivedPurchaserInfo)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun postReceiptCallsFailsFor40X() {
        postReceipt(401, false, null, null)

        assertNull(receivedPurchaserInfo)
        assertNotNull(receivedMessage)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun canGetEntitlementsWhenEmpty() {

        mockResponse("/subscribers/$appUserID/products", null, 200, null, "{'entitlements': {}}")

        backend.getEntitlements(appUserID, onReceiveEntitlementsSuccessHandler, onReceiveEntitlementsErrorHandler)

        assertNotNull(receivedEntitlements)
        assertEquals(0, receivedEntitlements!!.size)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun canHandleBadEntitlementsResponse() {

        mockResponse("/subscribers/$appUserID/products", null, 200, null, "{}")

        backend.getEntitlements(appUserID, onReceiveEntitlementsSuccessHandler, onReceiveEntitlementsErrorHandler)

        assertNull(receivedEntitlements)
        assertNotNull(receivedMessage)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun passesEntitlementsFieldToFactory() {
        mockResponse(
            "/subscribers/$appUserID/products",
            null,
            200,
            null,
            "{'entitlements': {'pro': {}}}"
        )

        every  {
            (any() as JSONObject).buildEntitlementsMap()
        } returns HashMap()

        backend.getEntitlements(appUserID, onReceiveEntitlementsSuccessHandler, onReceiveEntitlementsErrorHandler)

        verify {
            (any() as JSONObject).buildEntitlementsMap()
        }
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun canPostBasicAttributionData() {
        val path = "/subscribers/$appUserID/attribution"

        val `object` = JSONObject()
        `object`.put("string", "value")

        val expectedBody = JSONObject()
        expectedBody.put("network", Purchases.AttributionNetwork.APPSFLYER)
        expectedBody.put("data", `object`)

        backend.postAttributionData(appUserID, Purchases.AttributionNetwork.APPSFLYER, `object`)

        val headers = HashMap<String, String>()
        headers["Authorization"] = "Bearer $API_KEY"
        val slot = slot<JSONObject>()
        verify {
            mockClient.performRequest(
                eq(path),
                capture(slot),
                eq(headers)
            )
        }
        val captured = slot.captured
        assertTrue(captured.has("network") && captured.has("data") &&
                captured.getInt("network") == Purchases.AttributionNetwork.APPSFLYER.serverValue)
    }

    @Test
    @Throws(HTTPClient.HTTPErrorException::class, JSONException::class)
    fun doesntPostEmptyAttributionData() {
        backend.postAttributionData(
            appUserID,
            Purchases.AttributionNetwork.APPSFLYER,
            JSONObject()
        )
        verify {
            mockClient wasNot Called
        }
    }

    @Test
    @Throws(
        JSONException::class,
        MalformedURLException::class,
        HTTPClient.HTTPErrorException::class
    )
    fun encodesAppUserId() {
        val encodeableUserID = "userid with spaces"

        val encodedUserID = "userid%20with%20spaces"
        val path = "/subscribers/$encodedUserID/attribution"

        val `object` = JSONObject()
        `object`.put("string", "value")

        backend.postAttributionData(
            encodeableUserID,
            Purchases.AttributionNetwork.APPSFLYER,
            `object`
        )

        verify {
            mockClient.performRequest(
                eq(path),
                any() as JSONObject,
                any()
            )
        }
    }

    @Test
    fun doesntDispatchIfClosed() {
        backend.postAttributionData("id", Purchases.AttributionNetwork.APPSFLYER, JSONObject())

        backend.close()

        backend.postAttributionData("id", Purchases.AttributionNetwork.APPSFLYER, JSONObject())
    }

    @Test
    fun `given an alias token, alias calls properly`() {
        val body = mapOf(
            "new_app_user_id" to "newId"
        )
        mockResponse(
            "/subscribers/$appUserID/alias",
            body,
            200,
            null,
            null
        )

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        backend.createAlias(
            appUserID,
            "newId",
            onSuccess,
            {
                fail("Should have called success")
            }
        )

        verify {
            onSuccess.invoke()
        }
    }
}
