//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.Thread.sleep
import java.util.HashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
@Config(manifest= Config.NONE)
class BackendTest {
    private var mockClient: HTTPClient = mockk(relaxed = true)
    private var backend: Backend = Backend(
        API_KEY,
        SyncDispatcher(),
        mockClient
    )
    private var asyncBackend: Backend = Backend(
        API_KEY,
        Dispatcher(
            ThreadPoolExecutor(
                1,
                2,
                0,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue()
            )
        ),
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

    private fun mockResponse(
        path: String,
        body: Map<String, Any?>?,
        responseCode: Int,
        clientException: HTTPClient.HTTPErrorException?,
        resultBody: String?,
        delayed: Boolean = false
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

        val everyMockedCall = every {
            mockClient.performRequest(
                eq(path),
                (if (body == null) any() else eq(body)) as Map<*, *>,
                eq<Map<String, String>>(headers)
            )
        }

        if (clientException == null) {
            everyMockedCall answers {
                if (delayed) sleep(200)
                result
            }
        } else {
            everyMockedCall throws clientException
        }

        return info
    }

    private fun postReceipt(
        responseCode: Int,
        isRestore: Boolean,
        clientException: HTTPClient.HTTPErrorException?,
        resultBody: String?
    ): PurchaserInfo {

        val (fetchToken, productID, info) = mockPostReceiptResponse(
            isRestore,
            responseCode,
            clientException,
            resultBody
        )

        backend.postReceiptData(fetchToken, appUserID, productID, isRestore, onReceivePurchaserInfoSuccessHandler, onReceivePurchaserInfoErrorHandler)

        return info
    }

    private fun mockPostReceiptResponse(
        isRestore: Boolean,
        responseCode: Int,
        clientException: HTTPClient.HTTPErrorException?,
        resultBody: String?,
        delayed: Boolean = false
    ): Triple<String, String, PurchaserInfo> {
        val fetchToken = "fetch_token"
        val productID = "product_id"
        val body = mapOf(
            "fetch_token" to fetchToken,
            "app_user_id" to appUserID,
            "product_id" to productID,
            "is_restore" to isRestore
        )
        val info = mockResponse("/receipts", body, responseCode, clientException, resultBody, delayed)
        return Triple(fetchToken, productID, info)
    }

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
    fun getSubscriberInfoCallsProperURL() {

        val info = getPurchaserInfo(200, null, null)

        assertNotNull(receivedPurchaserInfo)
        assertEquals(info, receivedPurchaserInfo)
    }

    @Test
    fun getSubscriberInfoFailsIfNot20X() {
        val failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1)

        getPurchaserInfo(failureCode, null, null)

        assertNull(receivedPurchaserInfo)
        assertNotNull(receivedMessage)
    }

    @Test
    fun clientErrorCallsErrorHandler() {
        getPurchaserInfo(200, HTTPClient.HTTPErrorException(0, ""), null)

        assertNull(receivedPurchaserInfo)
        assertNotNull(receivedMessage)
    }

    @Test
    fun attemptsToParseErrorMessageFromServer() {
        getPurchaserInfo(404, null, "{'message': 'Dude not found'}")

        assertNotNull(receivedMessage)
        assertTrue(receivedMessage!!.contains("Dude not found"))
    }

    @Test
    fun handlesMissingMessageInErrorBody() {
        getPurchaserInfo(404, null, "{'no_message': 'Dude not found'}")
        assertNotNull(receivedMessage)
    }

    @Test
    fun postReceiptCallsProperURL() {
        val info = postReceipt(200, false, null, null)

        assertNotNull(receivedPurchaserInfo)
        assertSame(info, receivedPurchaserInfo)
    }

    @Test
    fun postReceiptCallsFailsFor40X() {
        postReceipt(401, false, null, null)

        assertNull(receivedPurchaserInfo)
        assertNotNull(receivedMessage)
    }

    @Test
    fun canGetEntitlementsWhenEmpty() {

        mockResponse("/subscribers/$appUserID/products", null, 200, null, "{'entitlements': {}}")

        backend.getEntitlements(appUserID, onReceiveEntitlementsSuccessHandler, onReceiveEntitlementsErrorHandler)

        assertNotNull(receivedEntitlements)
        assertEquals(0, receivedEntitlements!!.size)
    }

    @Test
    fun canHandleBadEntitlementsResponse() {

        mockResponse("/subscribers/$appUserID/products", null, 200, null, "{}")

        backend.getEntitlements(appUserID, onReceiveEntitlementsSuccessHandler, onReceiveEntitlementsErrorHandler)

        assertNull(receivedEntitlements)
        assertNotNull(receivedMessage)
    }

    @Test
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

    @Test
    fun `given multiple get calls for same subscriber, only one is triggered`() {
        mockResponse(
            "/subscribers/$appUserID",
            null,
            200,
            null,
            null,
            true
        )
        val lock = CountDownLatch(2)
        asyncBackend.getPurchaserInfo(appUserID, {
            lock.countDown()
        }, onReceivePurchaserInfoErrorHandler)
        asyncBackend.getPurchaserInfo(appUserID, {
            lock.countDown()
        }, onReceivePurchaserInfoErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify (exactly = 1) {
            mockClient.performRequest(
                "/subscribers/" + Uri.encode(appUserID),
                null as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber, only one is triggered`() {
        val (fetchToken, productID, _) = mockPostReceiptResponse(
            false,
            200,
            null,
            null,
            true
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(fetchToken, appUserID, productID, false, {
            lock.countDown()
        }, onReceivePurchaserInfoErrorHandler)
        asyncBackend.postReceiptData(fetchToken, appUserID, productID, false, {
            lock.countDown()
        }, onReceivePurchaserInfoErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify (exactly = 1) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun `given multiple entitlement get calls for same user, only one is triggered`() {
        mockResponse(
            "/subscribers/$appUserID/products",
            null,
            200,
            null,
            "{'entitlements': {}}",
            true
        )
        val lock = CountDownLatch(2)
        asyncBackend.getEntitlements(appUserID, {
            lock.countDown()
        }, onReceiveEntitlementsErrorHandler)
        asyncBackend.getEntitlements(appUserID, {
            lock.countDown()
        }, onReceiveEntitlementsErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify (exactly = 1) {
            mockClient.performRequest(
                "/subscribers/$appUserID/products",
                null as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun `given multiple entitlement get calls for different user, both are triggered`() {
        mockResponse(
            "/subscribers/$appUserID/products",
            null,
            200,
            null,
            "{'entitlements': {}}",
            true
        )
        val lock = CountDownLatch(2)
        asyncBackend.getEntitlements(appUserID, {
            lock.countDown()
        }, onReceiveEntitlementsErrorHandler)
        asyncBackend.getEntitlements("anotherUser", {
            lock.countDown()
        }, onReceiveEntitlementsErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify (exactly = 1) {
            mockClient.performRequest(
                "/subscribers/$appUserID/products",
                null as Map<*, *>?,
                any()
            )
        }
        verify (exactly = 1) {
            mockClient.performRequest(
                "/subscribers/anotherUser/products",
                null as Map<*, *>?,
                any()
            )
        }
    }

}
