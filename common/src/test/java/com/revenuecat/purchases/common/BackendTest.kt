//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.getNullableString
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
    fun setup() = mockkStatic("com.revenuecat.purchases.common.CustomerInfoFactoriesKt")

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

    private var receivedCustomerInfo: CustomerInfo? = null
    private var receivedCreated: Boolean? = null
    private var receivedOfferingsJSON: JSONObject? = null
    private var receivedError: PurchasesError? = null
    private var receivedShouldConsumePurchase: Boolean? = null

    private val headersSlot = slot<Map<String, String>>()

    private val onReceiveCustomerInfoSuccessHandler: (CustomerInfo) -> Unit = { info ->
            this@BackendTest.receivedCustomerInfo = info
        }

    private val onReceivePostReceiptSuccessHandler: (CustomerInfo, JSONObject?) -> Unit =
        { info, _ ->
            this@BackendTest.receivedCustomerInfo = info
        }

    private val postReceiptErrorCallback: (PurchasesError, Boolean, JSONObject?) -> Unit =
        { error, shouldConsumePurchase, _ ->
        this@BackendTest.receivedError = error
        this@BackendTest.receivedShouldConsumePurchase = shouldConsumePurchase
    }

    private val onReceiveCustomerInfoErrorHandler: (PurchasesError) -> Unit = {
            this@BackendTest.receivedError = it
        }

    private val onReceiveOfferingsResponseSuccessHandler: (JSONObject) -> Unit = { offeringsJSON ->
        this@BackendTest.receivedOfferingsJSON = offeringsJSON
    }

    private val onReceiveOfferingsErrorHandler: (PurchasesError) -> Unit = {
        this@BackendTest.receivedError = it
    }

    private val onLoginSuccessHandler: (CustomerInfo, Boolean) -> Unit = { customerInfo, created ->
        this@BackendTest.receivedCustomerInfo = customerInfo
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
        shouldMockCustomerInfo: Boolean = true
    ): CustomerInfo {
        val info: CustomerInfo = mockk()

        val result = HTTPResult(responseCode, resultBody ?: "{}")

        if (shouldMockCustomerInfo) {
            every {
                result.body.buildCustomerInfo()
            } returns info
        }
        val everyMockedCall = every {
            mockClient.performRequest(
                eq(path),
                (if (body == null) any() else eq(body)),
                capture(headersSlot)
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
        receiptInfo: ReceiptInfo,
        storeAppUserID: String?,
        marketplace: String? = null
    ): CustomerInfo {
        val (fetchToken, info) = mockPostReceiptResponse(
            isRestore,
            responseCode,
            clientException,
            resultBody = resultBody,
            observerMode = observerMode,
            receiptInfo = receiptInfo,
            storeAppUserID = storeAppUserID
        )

        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = isRestore,
            observerMode = observerMode,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = storeAppUserID,
            marketplace = marketplace,
            onSuccess = onReceivePostReceiptSuccessHandler,
            onError = postReceiptErrorCallback
        )

        return info
    }

    private val productIDs = listOf("product_id_0", "product_id_1")

    private fun mockPostReceiptResponse(
        isRestore: Boolean,
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        delayed: Boolean = false,
        observerMode: Boolean,
        receiptInfo: ReceiptInfo,
        storeAppUserID: String?
    ): Pair<String, CustomerInfo> {
        val fetchToken = "fetch_token"
        val body = mapOf(
            "fetch_token" to fetchToken,
            "app_user_id" to appUserID,
            "product_ids" to receiptInfo.productIDs,
            "is_restore" to isRestore,
            "presented_offering_identifier" to receiptInfo.offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to receiptInfo.price,
            "currency" to receiptInfo.currency,
            "normal_duration" to receiptInfo.duration,
            "intro_duration" to receiptInfo.introDuration,
            "trial_duration" to receiptInfo.trialDuration,
            "store_user_id" to storeAppUserID
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

    private fun getCustomerInfo(
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        appInBackground: Boolean = false
    ): CustomerInfo {
        val info =
            mockResponse("/subscribers/$appUserID", null, responseCode, clientException, resultBody)

        backend.getCustomerInfo(
            appUserID,
            appInBackground,
            onReceiveCustomerInfoSuccessHandler,
            onReceiveCustomerInfoErrorHandler
        )

        return info
    }

    @Test
    fun getSubscriberInfoCallsProperURL() {

        val info = getCustomerInfo(200, null, null)

        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo).isEqualTo(info)
    }

    @Test
    fun getSubscriberInfoFailsIfNot20X() {
        val failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1)

        getCustomerInfo(failureCode, null, null)

        assertThat(receivedCustomerInfo).isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun clientErrorCallsErrorHandler() {
        getCustomerInfo(200, IOException(), null)

        assertThat(receivedCustomerInfo).isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun attemptsToParseErrorMessageFromServer() {
        getCustomerInfo(404, null, "{'code': 7225, 'message': 'Dude not found'}")

        assertThat(receivedError).`as`("Received error is not null").isNotNull
        assertThat(receivedError!!.underlyingErrorMessage).`as`("Received underlying message is not null").isNotNull()
        assertThat(receivedError!!.underlyingErrorMessage!!).contains("Dude not found")
    }

    @Test
    fun handlesMissingMessageInErrorBody() {
        getCustomerInfo(404, null, "{'no_message': 'Dude not found'}")
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
            receiptInfo = ReceiptInfo(productIDs),
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedCustomerInfo)
    }

    @Test
    fun postReceiptCallsFailsFor4XX() {
        postReceipt(
            responseCode = 401,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = false,
            receiptInfo = ReceiptInfo(productIDs),
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is null").isNull()
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
    fun encodesAppUserId() {
        val encodeableUserID = "userid with spaces"

        val encodedUserID = "userid%20with%20spaces"
        val path = "/subscribers/$encodedUserID/offerings"

        val `object` = JSONObject()
        `object`.put("string", "value")

        backend.getOfferings(
            encodeableUserID,
            appInBackground = false,
            onSuccess = {},
            onError = {}
        )

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
        backend.getOfferings(
            appUserID = "id",
            appInBackground = false,
            onSuccess = {},
            onError = {}
        )

        backend.close()

        backend.getOfferings(
            appUserID = "id",
            appInBackground = false,
            onSuccess = {},
            onError = {}
        )
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
        asyncBackend.getCustomerInfo(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceiveCustomerInfoErrorHandler)
        asyncBackend.getCustomerInfo(appUserID, appInBackground = false, onSuccess = {
            lock.countDown()
        }, onError = onReceiveCustomerInfoErrorHandler)
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
        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a"
        )
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = ReceiptInfo(
                productIDs,
                offeringIdentifier = "offering_a"
            ),
            storeAppUserID = null
        )
        val lock = CountDownLatch(2)

        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        val productInfo1 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = productInfo1,
            storeAppUserID = null,
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
    fun `gets updated subscriber after post`() {
        val initialInfo = JSONObject(Responses.validFullPurchaserResponse).buildCustomerInfo()
        val updatedInfo = JSONObject(Responses.validEmptyPurchaserResponse).buildCustomerInfo()

        assertThat(initialInfo).isEqualTo(initialInfo.rawData.buildCustomerInfo())

        mockResponse(
            "/subscribers/$appUserID",
            null,
            200,
            null,
            initialInfo.rawData.toString(),
            true,
            shouldMockCustomerInfo = false
        )
        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a"
        )
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null
        )
        val lock = CountDownLatch(3)

        // Given calls

        asyncBackend.getCustomerInfo(appUserID, appInBackground = false, onSuccess = {
            assertThat(it).isEqualTo(initialInfo)
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                mockResponse(
                    "/subscribers/$appUserID",
                    null,
                    200,
                    null,
                    updatedInfo.rawData.toString(),
                    true,
                    shouldMockCustomerInfo = false
                )

                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        asyncBackend.getCustomerInfo(appUserID, appInBackground = false, onSuccess = {
            assertThat(it).isEqualTo(updatedInfo)
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)

        // Expect requests:

        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/receipts",
                any(),
                any()
            )
        }
        verify(exactly = 2) {
            mockClient.performRequest(
                "/subscribers/$appUserID",
                null,
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
            receiptInfo = ReceiptInfo(
                productIDs,
                offeringIdentifier = "offering_a"
            ),
            storeAppUserID = null
        )
        val lock = CountDownLatch(2)
        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
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
            receiptInfo = ReceiptInfo(
                productIDs,
                offeringIdentifier = "offering_b"
            ),
            storeAppUserID = null
        )
        val productInfo1 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_b"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken1,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = productInfo1,
            storeAppUserID = null,
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
            receiptInfo = ReceiptInfo(productIDs),
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedCustomerInfo)
    }

    @Test
    fun `postReceipt passes price and currency`() {
        val storeProduct = mockStoreProduct()

        val info = postReceipt(
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            receiptInfo = ReceiptInfo(
                productIDs,
                storeProduct = storeProduct
            ),
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedCustomerInfo)
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
            receiptInfo = ReceiptInfo(
                productIDs,
                offeringIdentifier = "offering_a"
            ),
            storeAppUserID = null
        )

        val storeProduct = mockStoreProduct()
        val storeProduct1 = mockStoreProduct(price = 350000)
        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct
        )
        val productInfo1 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct1
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = productInfo1,
            storeAppUserID = null
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
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
            receiptInfo = productInfo1,
            storeAppUserID = null,
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
            receiptInfo = ReceiptInfo(
                productIDs,
                offeringIdentifier = "offering_a"
            ),
            storeAppUserID = null
        )

        val storeProduct = mockStoreProduct()
        val storeProduct1 = mockStoreProduct(duration = "P2M")

        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct
        )
        val productInfo1 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct1
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null
        )
        mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = productInfo1,
            storeAppUserID = null
        )
        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
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
            receiptInfo = productInfo1,
            storeAppUserID = null,
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
        val storeProduct = mockStoreProduct()

        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct
        )
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null
        )

        val lock = CountDownLatch(2)
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
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
            receiptInfo = receiptInfo,
            storeAppUserID = null,
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
        val storeProduct = mockStoreProduct(
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
            receiptInfo = ReceiptInfo(
                productIDs,
                storeProduct = storeProduct
            ),
            storeAppUserID = null,
        )

        assertThat(receivedCustomerInfo).`as`("Received purchaser info is not null").isNotNull
        assertThat(info).isEqualTo(receivedCustomerInfo)
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
    fun `customer info call is enqueued with delay if on background`() {
        dispatcher.calledWithRandomDelay = null

        getCustomerInfo(200, clientException = null, resultBody = null, appInBackground = true)

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
    fun `logIn correctly parses customerInfo`() {
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
            shouldMockCustomerInfo = false
        )
        val expectedCustomerInfo = JSONObject(resultBody).buildCustomerInfo()

        backend.logIn(
            appUserID,
            newAppUserID,
            onLoginSuccessHandler,
            {
                fail("Should have called success")
            }
        )
        assertThat(receivedCustomerInfo).isEqualTo(expectedCustomerInfo)
        assertThat(receivedCreated).isEqualTo(true)
    }

    @Test
    fun `logIn calls OnError if customerInfo can't be parsed`() {
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
            shouldMockCustomerInfo = false
        )

        backend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                fail("Should have called error")
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

    @Test
    fun `given multiple login calls for same ids, only one is triggered`() {
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
            resultBody = resultBody,
            delayed = true
        )

        val lock = CountDownLatch(2)
        asyncBackend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                lock.countDown()
            },
            {
                fail("Should have called success")
            }
        )
        asyncBackend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                lock.countDown()
            },
            {
                fail("Should have called success")
            }
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/identify",
                requestBody,
                any()
            )
        }
    }

    @Test
    fun `given multiple login calls for same ids, only one http call is triggered, and all onError callbacks are called if customerInfo can't be parsed`() {
        val newAppUserID = "newId"
        val requestBody = mapOf(
            "new_app_user_id" to newAppUserID,
            "app_user_id" to appUserID
        )
        val resultBody = "{}"
        mockResponse(
            "/subscribers/identify",
            requestBody,
            responseCode = 200,
            clientException = null,
            resultBody = resultBody,
            delayed = true
        )

        val lock = CountDownLatch(2)
        asyncBackend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                fail("Should have called error")
            },
            {
                lock.countDown()
            }
        )
        asyncBackend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                fail("Should have called error")
            },
            {
                lock.countDown()
            }
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/identify",
                requestBody,
                any()
            )
        }
    }

    @Test
    fun `given multiple login calls for same ids, only one http call is triggered, and all onError callbacks are called if call is not successful`() {
        val newAppUserID = "newId"
        val requestBody = mapOf(
            "new_app_user_id" to newAppUserID,
            "app_user_id" to appUserID
        )
        val resultBody = "{}"
        mockResponse(
            "/subscribers/identify",
            requestBody,
            responseCode = 500,
            clientException = null,
            resultBody = resultBody,
            delayed = true
        )

        val lock = CountDownLatch(2)
        asyncBackend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                fail("Should have called error")
            },
            {
                lock.countDown()
            }
        )
        asyncBackend.logIn(
            appUserID,
            newAppUserID,
            { _, _ ->
                fail("Should have called error")
            },
            {
                lock.countDown()
            }
        )
        lock.await(2000, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            mockClient.performRequest(
                "/subscribers/identify",
                requestBody,
                any()
            )
        }
    }

    @Test
    fun `given multiple post calls for same subscriber different store user ID, both are triggered`() {

        val lock = CountDownLatch(2)
        val receiptInfo = ReceiptInfo(productIDs)
        val (fetchToken, _) = mockPostReceiptResponse(
            isRestore = false,
            responseCode = 200,
            clientException = null,
            resultBody = null,
            delayed = true,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = null,
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
            receiptInfo = receiptInfo,
            storeAppUserID = "store_app_user_id"
        )
        asyncBackend.postReceiptData(
            purchaseToken = fetchToken1,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = "store_app_user_id",
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
    fun `clearing caches clears http caches`() {
        backend.clearCaches()

        verify {
            mockClient.clearCaches()
        }
    }

    @Test
    fun `postReceipt calls fail for multiple product ids errors`() {
        postReceipt(
            responseCode = 400,
            isRestore = false,
            clientException = null,
            resultBody = """
                {"code":7662,
                "message":"The product IDs list provided is not an array or does not contain only a single element."
                }""".trimIndent(),
            observerMode = false,
            receiptInfo = ReceiptInfo(productIDs),
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is null").isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
        assertThat(receivedError!!.code)
            .`as`("Received error code is the right one")
            .isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(receivedShouldConsumePurchase).`as`("Purchase shouldn't be consumed").isFalse()
    }

    @Test
    fun `postReceipt passes formatted price as header`() {
        val storeProduct = mockStoreProduct()

        postReceipt(
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            receiptInfo = ReceiptInfo(
                productIDs,
                storeProduct = storeProduct
            ),
            storeAppUserID = null,
        )

        assertThat(headersSlot.isCaptured).isTrue
        assertThat(headersSlot.captured.keys).contains("price_string")
        assertThat(headersSlot.captured["price_string"]).isEqualTo("$25")
    }

    @Test
    fun `postReceipt passes marketplace as header`() {
        val storeProduct = mockStoreProduct()

        postReceipt(
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            receiptInfo = ReceiptInfo(
                productIDs,
                storeProduct = storeProduct
            ),
            storeAppUserID = null,
            marketplace = "DE"
        )

        assertThat(headersSlot.isCaptured).isTrue
        assertThat(headersSlot.captured.keys).contains("price_string")
        assertThat(headersSlot.captured["price_string"]).isEqualTo("$25")
        assertThat(headersSlot.captured["marketplace"]).isEqualTo("DE")
    }

    private fun mockStoreProduct(
        price: Long = 25_000_000,
        duration: String = "P1M",
        introDuration: String = "P1M",
        trialDuration: String = "P1M"
    ): StoreProduct {
        val storeProduct = mockk<StoreProduct>()
        every { storeProduct.priceAmountMicros } returns price
        every { storeProduct.priceCurrencyCode } returns "USD"
        every { storeProduct.subscriptionPeriod } returns duration
        every { storeProduct.introductoryPricePeriod } returns introDuration
        every { storeProduct.freeTrialPeriod } returns trialDuration
        every { storeProduct.price } returns "$25"
        return storeProduct
    }

}
