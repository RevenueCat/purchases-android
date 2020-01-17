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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
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
@Config(manifest = Config.NONE)
class BackendTest {

    @Before
    fun setup() = mockkStatic("com.revenuecat.purchases.FactoriesKt")

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
    private var receivedOfferingsJSON: JSONObject? = null
    private var receivedError: PurchasesError? = null

    private val onReceivePurchaserInfoSuccessHandler: (PurchaserInfo) -> Unit = { info ->
            this@BackendTest.receivedPurchaserInfo = info
        }

    private val postReceiptErrorCallback: (PurchasesError, Boolean) -> Unit = { error, _ ->
        this@BackendTest.receivedError = error
    }

    private val onReceivePurchaserInfoErrorHandler: (PurchasesError) -> Unit = {
            this@BackendTest.receivedError = it
        }

    private val onReceiveOfferingsResponseSuccessHandler: (JSONObject) -> Unit = { offeringsJSON ->
        this@BackendTest.receivedOfferingsJSON = offeringsJSON
    }

    private val onReceiveOfferingsErrorHandler: (PurchasesError) -> Unit = {
        this@BackendTest.receivedError = it
    }

    private inner class SyncDispatcher : Dispatcher(mockk()) {

        private var closed = false

        override fun enqueue(call: AsyncCall) {
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
        assertThat(backend).isNotNull
    }

    private fun mockResponse(
        path: String,
        body: Map<String, Any?>?,
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        delayed: Boolean = false
    ): PurchaserInfo {
        val info: PurchaserInfo = mockk()

        val result = HTTPClient.Result()
        result.responseCode = responseCode
        result.body = JSONObject(resultBody ?: "{}")

        val headers = HashMap<String, String>()
        headers["Authorization"] = "Bearer $API_KEY"

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
        clientException: Exception?,
        resultBody: String?,
        offeringIdentifier: String?,
        observerMode: Boolean,
        price: Double?,
        currency: String?
    ): PurchaserInfo {

        val (fetchToken, productID, info) = mockPostReceiptResponse(
            isRestore,
            responseCode,
            clientException,
            resultBody,
            offeringIdentifier = offeringIdentifier,
            observerMode = observerMode,
            price = price,
            currency = currency
        )

        backend.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            isRestore,
            offeringIdentifier,
            observerMode,
            price,
            currency,
            onReceivePurchaserInfoSuccessHandler,
            postReceiptErrorCallback
        )

        return info
    }

    private fun mockPostReceiptResponse(
        isRestore: Boolean,
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        delayed: Boolean = false,
        offeringIdentifier: String?,
        observerMode: Boolean,
        price: Double?,
        currency: String?
    ): Triple<String, String, PurchaserInfo> {
        val fetchToken = "fetch_token"
        val productID = "product_id"
        val body = mapOf(
            "fetch_token" to fetchToken,
            "app_user_id" to appUserID,
            "product_id" to productID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to price,
            "currency" to currency
        ).mapNotNull { entry: Map.Entry<String, Any?> ->
            entry.value?.let { entry.key to it }
        }.toMap()

        val info = mockResponse(
            "/receipts",
            body,
            responseCode,
            clientException,
            resultBody,
            delayed
        )
        return Triple(fetchToken, productID, info)
    }

    private fun getPurchaserInfo(
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?
    ): PurchaserInfo {
        val info =
            mockResponse("/subscribers/$appUserID", null, responseCode, clientException, resultBody)

        backend.getPurchaserInfo(
            appUserID,
            onReceivePurchaserInfoSuccessHandler,
            onReceivePurchaserInfoErrorHandler
        )

        return info
    }

    @Test
    fun getSubscriberInfoCallsProperURL() {

        val info = getPurchaserInfo(200, null, null)

        assertThat(receivedPurchaserInfo).isNotNull
        assertThat(receivedPurchaserInfo).isEqualTo(info)
    }

    @Test
    fun getSubscriberInfoFailsIfNot20X() {
        val failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1)

        getPurchaserInfo(failureCode, null, null)

        assertThat(receivedPurchaserInfo).isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun clientErrorCallsErrorHandler() {
        getPurchaserInfo(200, IOException(), null)

        assertThat(receivedPurchaserInfo).isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun attemptsToParseErrorMessageFromServer() {
        getPurchaserInfo(404, null, "{'code': 7225, 'message': 'Dude not found'}")

        assertThat(receivedError).`as`("Received error is not null").isNotNull
        assertThat(receivedError!!.underlyingErrorMessage).`as`("Received underlying message is not null").isNotNull()
        assertThat(receivedError!!.underlyingErrorMessage!!).contains("Dude not found")
    }

    @Test
    fun handlesMissingMessageInErrorBody() {
        getPurchaserInfo(404, null, "{'no_message': 'Dude not found'}")
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun postReceiptCallsProperURL() {
        val info = postReceipt(
            200,
            false,
            null,
            null,
            null,
            false,
            null,
            null
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun postReceiptCallsFailsFor40X() {
        postReceipt(
            401,
            false,
            null,
            null,
            null,
            false,
            null,
            null
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is null").isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    private val noOfferingsResponse = "{'offerings': [], 'current_offering_id': null}"
    @Test
    fun `given a no offerings response`() {

        mockResponse("/subscribers/$appUserID/offerings", null, 200, null, noOfferingsResponse)

        backend.getOfferings(appUserID, onReceiveOfferingsResponseSuccessHandler, onReceiveOfferingsErrorHandler)

        assertThat(receivedOfferingsJSON).`as`("Received offerings response is not null").isNotNull
        assertThat(receivedOfferingsJSON!!.getJSONArray("offerings").length()).isZero()
        assertThat(receivedOfferingsJSON!!.getNullableString("current_offering_id")).isNull()
    }

    @Test
    fun canPostBasicAttributionData() {
        val path = "/subscribers/$appUserID/attribution"

        val `object` = JSONObject()
        `object`.put("string", "value")

        val expectedBody = JSONObject()
        expectedBody.put("network", Purchases.AttributionNetwork.APPSFLYER)
        expectedBody.put("data", `object`)

        backend.postAttributionData(appUserID, Purchases.AttributionNetwork.APPSFLYER, `object`) {

        }

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
        assertThat(captured.has("network") && captured.has("data") &&
                captured.getInt("network") == Purchases.AttributionNetwork.APPSFLYER.serverValue).isTrue()
    }

    @Test
    fun doesntPostEmptyAttributionData() {
        backend.postAttributionData(
            appUserID,
            Purchases.AttributionNetwork.APPSFLYER,
            JSONObject()
        ) {}
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
        ) { }

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
        backend.postAttributionData("id", Purchases.AttributionNetwork.APPSFLYER, JSONObject()) { }

        backend.close()

        backend.postAttributionData("id", Purchases.AttributionNetwork.APPSFLYER, JSONObject()) { }
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
                fail<String>("Should have called success")
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
        verify(exactly = 1) {
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
            true,
            "offering_a",
            false,
            null,
            null
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            "offering_a",
            false,
            null,
            null,
            {
                lock.countDown()
            },
            postReceiptErrorCallback
        )
        asyncBackend.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            "offering_a",
            false,
            null,
            null,
            {
                lock.countDown()
            },
            postReceiptErrorCallback
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun `given multiple get offerings calls for same user, only one is triggered`() {
        mockResponse(
            "/subscribers/$appUserID/offerings",
            null,
            200,
            null,
            noOfferingsResponse,
            true
        )
        val lock = CountDownLatch(2)
        asyncBackend.getOfferings(appUserID, {
            lock.countDown()
        }, onReceiveOfferingsErrorHandler)
        asyncBackend.getOfferings(appUserID, {
            lock.countDown()
        }, onReceiveOfferingsErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/$appUserID/offerings",
                null as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun `given multiple offerings get calls for different user, both are triggered`() {
        mockResponse(
            "/subscribers/$appUserID/offerings",
            null,
            200,
            null,
            noOfferingsResponse,
            true
        )
        val lock = CountDownLatch(2)
        asyncBackend.getOfferings(appUserID, {
            lock.countDown()
        }, onReceiveOfferingsErrorHandler)
        asyncBackend.getOfferings("anotherUser", {
            lock.countDown()
        }, onReceiveOfferingsErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/$appUserID/offerings",
                null as Map<*, *>?,
                any()
            )
        }
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/anotherUser/offerings",
                null as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber different offering, both are triggered`() {
        val (fetchToken, productID, _) = mockPostReceiptResponse(
            false,
            200,
            null,
            null,
            true,
            "offering_a",
            false,
            null,
            null
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            "offering_a",
            false,
            null,
            null,
            {
                lock.countDown()
            },
            postReceiptErrorCallback
        )
        val (fetchToken1, productID1, _) = mockPostReceiptResponse(
            false,
            200,
            null,
            null,
            true,
            "offering_b",
            false,
            null,
            null
        )
        asyncBackend.postReceiptData(
            fetchToken1,
            appUserID,
            productID1,
            false,
            "offering_b",
            false,
            null,
            null,
            {
                lock.countDown()
            },
            postReceiptErrorCallback
        )
        lock.await()
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 2) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<*, *>?,
                any()
            )
        }
    }

    @Test
    fun postReceiptObserverMode() {
        val info = postReceipt(
            200,
            false,
            null,
            null,
            null,
            true,
            null,
            null
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun `postReceipt passes price and currency`() {
        val info = postReceipt(
            200,
            false,
            null,
            null,
            null,
            true,
            2.5,
            "USD"
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun `given multiple post calls for same subscriber different price, both are triggered`() {
        val (fetchToken, productID, _) = mockPostReceiptResponse(
            false,
            200,
            null,
            null,
            true,
            "offering_a",
            false,
            null,
            null
        )
        val (fetchToken1, productID1, _) = mockPostReceiptResponse(
            false,
            200,
            null,
            null,
            true,
            "offering_a",
            false,
            2.5,
            "USD"
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            "offering_a",
            false,
            null,
            null,
            {
                lock.countDown()
            },
            postReceiptErrorCallback
        )
        asyncBackend.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            "offering_a",
            false,
            2.5,
            "USD",
            {
                lock.countDown()
            },
            postReceiptErrorCallback
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 2) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<*, *>?,
                any()
            )
        }
    }
}
