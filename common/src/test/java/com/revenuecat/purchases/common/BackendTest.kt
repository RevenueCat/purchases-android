//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.attribution.AttributionNetwork
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.getNullableString
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
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BackendTest {

    @Before
    fun setup() = mockkStatic("com.revenuecat.purchases.common.FactoriesKt")

    private var mockClient: HTTPClient = mockk(relaxed = true)
    private val dispatcher = SyncDispatcher()
    private var backend: Backend = Backend(
        API_KEY,
        dispatcher,
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
    private var receivedCreated: Boolean? = null
    private var receivedOfferingsJSON: JSONObject? = null
    private var receivedError: PurchasesError? = null

    private val onReceivePurchaserInfoSuccessHandler: (PurchaserInfo) -> Unit = { info ->
            this@BackendTest.receivedPurchaserInfo = info
        }

    private val onReceivePostReceiptSuccessHandler: (PurchaserInfo, JSONObject?) -> Unit =
        { info, _ ->
            this@BackendTest.receivedPurchaserInfo = info
        }

    private val postReceiptErrorCallback: (PurchasesError, Boolean, JSONObject?) -> Unit =
        { error, _, _ ->
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

    private val onLoginSuccessHandler: (PurchaserInfo, Boolean) -> Unit = { purchaserInfo, created ->
        this@BackendTest.receivedPurchaserInfo = purchaserInfo
        this@BackendTest.receivedCreated = created
    }

    private val onReceiveLoginErrorHandler: (PurchasesError) -> Unit = {
        this@BackendTest.receivedError = it
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
        delayed: Boolean = false,
        shouldMockPurchaserInfo: Boolean = true
    ): PurchaserInfo {
        val info: PurchaserInfo = mockk()

        val result = HTTPClient.Result(responseCode, JSONObject(resultBody ?: "{}"))

        val headers = HashMap<String, String>()
        headers["Authorization"] = "Bearer $API_KEY"

        if (shouldMockPurchaserInfo) {
            every {
                result.body.buildPurchaserInfo()
            } returns info
        }
        val everyMockedCall = every {
            mockClient.performRequest(
                eq(path),
                (if (body == null) any() else eq(body)),
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
        observerMode: Boolean,
        productInfo: ProductInfo
    ): PurchaserInfo {
        val (fetchToken, info) = mockPostReceiptResponse(
            isRestore,
            responseCode,
            clientException,
            resultBody = resultBody,
            observerMode = observerMode,
            productInfo = productInfo
        )

        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = isRestore,
            observerMode = observerMode,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = onReceivePostReceiptSuccessHandler,
            onError = postReceiptErrorCallback
        )

        return info
    }

    private val productID = "product_id"

    private fun mockPostReceiptResponse(
        isRestore: Boolean,
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        delayed: Boolean = false,
        observerMode: Boolean,
        productInfo: ProductInfo
    ): Pair<String, PurchaserInfo> {
        val fetchToken = "fetch_token"
        val body = mapOf(
            "fetch_token" to fetchToken,
            "app_user_id" to appUserID,
            "product_id" to productInfo.productID,
            "is_restore" to isRestore,
            "presented_offering_identifier" to productInfo.offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to productInfo.price,
            "currency" to productInfo.currency,
            "normal_duration" to productInfo.duration,
            "intro_duration" to productInfo.introDuration,
            "trial_duration" to productInfo.trialDuration
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
        return fetchToken to info
    }

    private fun getPurchaserInfo(
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        appInBackground: Boolean = false
    ): PurchaserInfo {
        val info =
            mockResponse("/subscribers/$appUserID", null, responseCode, clientException, resultBody)

        backend.getPurchaserInfo(
            appUserID,
            appInBackground,
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
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = false,
            productInfo = ProductInfo(productID)
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun postReceiptCallsFailsFor40X() {
        postReceipt(
            responseCode = 401,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = false,
            productInfo = ProductInfo(productID)
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is null").isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    private val noOfferingsResponse = "{'offerings': [], 'current_offering_id': null}"
    @Test
    fun `given a no offerings response`() {

        mockResponse("/subscribers/$appUserID/offerings", null, 200, null, noOfferingsResponse)

        backend.getOfferings(
            appUserID,
            appInBackground = false,
            onSuccess = onReceiveOfferingsResponseSuccessHandler,
            onError = onReceiveOfferingsErrorHandler
        )

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
        expectedBody.put("network", AttributionNetwork.APPSFLYER)
        expectedBody.put("data", `object`)

        backend.postAttributionData(appUserID, AttributionNetwork.APPSFLYER, `object`) {

        }

        val headers = HashMap<String, String>()
        headers["Authorization"] = "Bearer $API_KEY"
        val slot = slot<Map<String, Any?>>()
        verify {
            mockClient.performRequest(
                eq(path),
                capture(slot),
                eq(headers)
            )
        }
        val captured = slot.captured
        assertThat(captured.containsKey("network") && captured.containsKey("data") &&
                captured["network"] == AttributionNetwork.APPSFLYER.serverValue).isTrue()
    }

    @Test
    fun doesntPostEmptyAttributionData() {
        backend.postAttributionData(
            appUserID,
            AttributionNetwork.APPSFLYER,
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
            AttributionNetwork.APPSFLYER,
            `object`
        ) { }

        verify {
            mockClient.performRequest(
                eq(path),
                any(),
                any()
            )
        }
    }

    @Test
    fun doesntDispatchIfClosed() {
        backend.postAttributionData("id", AttributionNetwork.APPSFLYER, JSONObject()) { }

        backend.close()

        backend.postAttributionData("id", AttributionNetwork.APPSFLYER, JSONObject()) { }
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
        asyncBackend.getPurchaserInfo(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceivePurchaserInfoErrorHandler)
        asyncBackend.getPurchaserInfo(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceivePurchaserInfoErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/" + Uri.encode(appUserID),
                null,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber, only one is triggered`() {
        val productInfo = ProductInfo(
            productID = productID,
            offeringIdentifier = "offering_a"
        )
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = ProductInfo(
                productID,
                offeringIdentifier = "offering_a"
            )
        )
        val lock = CountDownLatch(2)

        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        val productInfo1 = ProductInfo(
            productID = productID,
            offeringIdentifier = "offering_a"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo1,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/receipts",
                any(),
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
        asyncBackend.getOfferings(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)
        asyncBackend.getOfferings(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/$appUserID/offerings",
                null,
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
        asyncBackend.getOfferings(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)
        asyncBackend.getOfferings("anotherUser", appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/$appUserID/offerings",
                null,
                any()
            )
        }
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/anotherUser/offerings",
                null,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber different offering, both are triggered`() {
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = ProductInfo(
                productID,
                offeringIdentifier = "offering_a"
            )
        )
        val lock = CountDownLatch(2)
        val productInfo = ProductInfo(
            productID = productID,
            offeringIdentifier = "offering_a"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        val (fetchToken1, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = ProductInfo(
                productID,
                offeringIdentifier = "offering_b"
            )
        )
        val productInfo1 = ProductInfo(
            productID = productID,
            offeringIdentifier = "offering_b"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken1,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo1,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        lock.await()
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 2) {
            mockClient.performRequest(
                "/receipts",
                any(),
                any()
            )
        }
    }

    @Test
    fun postReceiptObserverMode() {
        val info = postReceipt(
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            productInfo = ProductInfo(productID)
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun `postReceipt passes price and currency`() {
        val skuDetails = mockSkuDetails()

        val info = postReceipt(
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            productInfo = ProductInfo(
                productID,
                skuDetails = skuDetails
            )
        )

        assertThat(receivedPurchaserInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun `given multiple post calls for same subscriber different price, both are triggered`() {
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = ProductInfo(
                productID,
                offeringIdentifier = "offering_a"
            )
        )

        val skuDetails = mockSkuDetails()
        val skuDetails1 = mockSkuDetails(price = 350000)
        val productInfo = ProductInfo(
            productID,
            offeringIdentifier = "offering_a",
            skuDetails = skuDetails
        )
        val productInfo1 = ProductInfo(
            productID = productID,
            offeringIdentifier = "offering_a",
            skuDetails = skuDetails1
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = productInfo
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = productInfo1
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo1,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 2) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<String, Any?>,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber different durations, both are triggered`() {
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = ProductInfo(
                productID,
                offeringIdentifier = "offering_a"
            )
        )

        val skuDetails = mockSkuDetails()
        val skuDetails1 = mockSkuDetails(duration = "P2M")

        val productInfo = ProductInfo(
            productID = productID,
            offeringIdentifier = "offering_a",
            skuDetails = skuDetails
        )
        val productInfo1 = ProductInfo(
            productID,
            offeringIdentifier = "offering_a",
            skuDetails = skuDetails1
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = productInfo
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = productInfo1
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo1,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        lock.await()
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 2) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<String, Any?>,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber same durations, only one is triggered`() {
        val skuDetails = mockSkuDetails()

        val productInfo = ProductInfo(
            productID,
            offeringIdentifier = "offering_a",
            skuDetails = skuDetails
        )
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            productInfo = productInfo
        )

        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        lock.await()
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/receipts",
                any() as Map<String, Any?>,
                any()
            )
        }
    }

    @Test
    fun `postReceipt passes durations`() {
        val skuDetails = mockSkuDetails(
            duration = "P1M",
            introDuration = "P2M",
            trialDuration = "P3M"
        )

        val info = postReceipt(
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            productInfo = ProductInfo(
                productID = productID,
                skuDetails = skuDetails
            )
        )

        assertThat(receivedPurchaserInfo).`as`("Received purchaser info is not null").isNotNull
        assertThat(info).isEqualTo(receivedPurchaserInfo)
    }

    @Test
    fun `offerings call is enqueued with delay if on background`() {
        mockResponse("/subscribers/$appUserID/offerings", null, 200, null, noOfferingsResponse)
        dispatcher.calledWithRandomDelay = null
        backend.getOfferings(
            appUserID,
            appInBackground = true,
            onSuccess = onReceiveOfferingsResponseSuccessHandler,
            onError = onReceiveOfferingsErrorHandler
        )

        val calledWithRandomDelay: Boolean? = dispatcher.calledWithRandomDelay
        assertThat(calledWithRandomDelay).isNotNull()
        assertThat(calledWithRandomDelay).isTrue()
    }

    @Test
    fun `purchaser info call is enqueued with delay if on background`() {
        dispatcher.calledWithRandomDelay = null

        getPurchaserInfo(200, clientException = null, resultBody = null, appInBackground = true)

        val calledWithRandomDelay: Boolean? = dispatcher.calledWithRandomDelay
        assertThat(calledWithRandomDelay).isNotNull()
        assertThat(calledWithRandomDelay).isTrue()
    }

    @Test
    fun `logIn makes the right http call`() {
        val newAppUserID = "newId"
        val body = mapOf(
            "new_app_user_id" to newAppUserID,
            "app_user_id" to appUserID
        )
        mockResponse(
            "/subscribers/identify",
            body,
            201,
            null,
            Responses.validFullPurchaserResponse
        )

        backend.logIn(
            appUserID,
            newAppUserID,
            onLoginSuccessHandler,
            {
                fail("Should have called success")
            }
        )
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/identify",
                body,
                any()
            )
        }
    }

    @Test
    fun `logIn correctly parses purchaserInfo`() {
        val newAppUserID = "newId"
        val requestBody = mapOf(
            "new_app_user_id" to newAppUserID,
            "app_user_id" to appUserID
        )
        val resultBody = Responses.validFullPurchaserResponse
        mockResponse(
            "/subscribers/identify",
            requestBody,
            responseCode = 201,
            clientException = null,
            resultBody = resultBody,
            delayed = false,
            shouldMockPurchaserInfo = false
        )
        val expectedPurchaserInfo = JSONObject(resultBody).buildPurchaserInfo()

        backend.logIn(
            appUserID,
            newAppUserID,
            onLoginSuccessHandler,
            {
                fail("Should have called success")
            }
        )
        assertThat(receivedPurchaserInfo).isEqualTo(expectedPurchaserInfo)
        assertThat(receivedCreated).isEqualTo(true)
    }

    @Test
    fun `logIn calls OnError if purchaserInfo can't be parsed`() {
        val newAppUserID = "newId"
        val requestBody = mapOf(
            "new_app_user_id" to newAppUserID
        )
        val resultBody = "{}"
        mockResponse(
            "/subscribers/$appUserID/identify",
            requestBody,
            responseCode = 201,
            clientException = null,
            resultBody = resultBody,
            delayed = false,
            shouldMockPurchaserInfo = false
        )

        backend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                fail("Should have called success")
            },
            onReceiveLoginErrorHandler
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `logIn returns created true if status is 201`() {
        val newAppUserID = "newId"
        val requestBody = mapOf(
            "new_app_user_id" to newAppUserID,
            "app_user_id" to appUserID
        )
        val resultBody = Responses.validFullPurchaserResponse
        mockResponse(
            "/subscribers/identify",
            requestBody,
            responseCode = 201,
            clientException = null,
            resultBody = resultBody
        )

        backend.logIn(
            appUserID,
            newAppUserID,
            onLoginSuccessHandler,
            {
                fail("Should have called success")
            }
        )
        assertThat(receivedCreated).isTrue()
    }

    @Test
    fun `logIn returns created false if status isn't 201`() {
        val newAppUserID = "newId"
        val requestBody = mapOf(
            "new_app_user_id" to newAppUserID,
            "app_user_id" to appUserID
        )
        val resultBody = Responses.validFullPurchaserResponse
        mockResponse(
            "/subscribers/identify",
            requestBody,
            responseCode = 200,
            clientException = null,
            resultBody = resultBody
        )

        backend.logIn(
            appUserID,
            newAppUserID,
            onLoginSuccessHandler,
            {
                fail("Should have called success")
            }
        )
        assertThat(receivedCreated).isFalse
    }

    private fun mockSkuDetails(
        price: Long = 25000000,
        duration: String = "P1M",
        introDuration: String = "P1M",
        trialDuration: String = "P1M"
    ): SkuDetails {
        val skuDetails = mockk<SkuDetails>()
        every { skuDetails.priceAmountMicros } returns price
        every { skuDetails.priceCurrencyCode } returns "USD"
        every { skuDetails.subscriptionPeriod } returns duration
        every { skuDetails.introductoryPricePeriod } returns introDuration
        every { skuDetails.freeTrialPeriod } returns trialDuration
        return skuDetails
    }

}
