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
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.getNullableString
import com.revenuecat.purchases.utils.stubSubscriptionOption
import com.revenuecat.purchases.utils.stubStoreProduct
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
    private val storeProduct = stubStoreProduct("productID")
    private val productIDs = listOf("product_id_0", "product_id_1")
    private val basicReceiptInfo = ReceiptInfo(
        productIDs,
        offeringIdentifier = "offering_a"
    )
    private val fetchToken = "fetch_token"

    private var receivedCustomerInfo: CustomerInfo? = null
    private var receivedCustomerInfoCreated: Boolean? = null
    private var receivedOfferingsJSON: JSONObject? = null
    private var receivedError: PurchasesError? = null
    private var receivedShouldConsumePurchase: Boolean? = null
    private val noOfferingsResponse = "{'offerings': [], 'current_offering_id': null}"

    private val headersSlot = slot<Map<String, String>>()
    private val requestBodySlot = slot<Map<String, Any?>>()

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
        this@BackendTest.receivedCustomerInfoCreated = created
    }

    private val onReceiveLoginErrorHandler: (PurchasesError) -> Unit = {
        this@BackendTest.receivedError = it
    }

    // region general backend functionality
    @Test
    fun canBeCreated() {
        assertThat(backend).isNotNull
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
        assertThat(receivedError!!.underlyingErrorMessage).`as`("Received underlying message is not null").isNotNull
        assertThat(receivedError!!.underlyingErrorMessage!!).contains("Dude not found")
    }

    @Test
    fun handlesMissingMessageInErrorBody() {
        getCustomerInfo(404, null, "{'no_message': 'Dude not found'}")
        assertThat(receivedError).`as`("Received error is not null").isNotNull
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
    fun `clearing caches clears http caches`() {
        backend.clearCaches()

        verify {
            mockClient.clearCaches()
        }
    }

    // endregion

    // region getCustomerInfo

    @Test
    fun getCustomerInfoCallsProperURL() {

        val info = getCustomerInfo(200, null, null)

        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo).isEqualTo(info)
    }

    @Test
    fun getCustomerInfoFailsIfNot20X() {
        val failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1)

        getCustomerInfo(failureCode, null, null)

        assertThat(receivedCustomerInfo).isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun `given multiple getCustomerInfo calls for same subscriber same body, only one is triggered`() {
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
    fun `customer info call is enqueued with delay if on background`() {
        dispatcher.calledWithRandomDelay = null

        getCustomerInfo(200, clientException = null, resultBody = null, appInBackground = true)

        val calledWithRandomDelay: Boolean? = dispatcher.calledWithRandomDelay
        assertThat(calledWithRandomDelay).isNotNull
        assertThat(calledWithRandomDelay).isTrue
    }

    // endregion

    // region postReceipt

    @Test
    fun postReceiptCallsProperURL() {
        val info = mockPostReceiptResponseAndPost(
            backend,
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = false,
            receiptInfo = basicReceiptInfo,
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is not null").isNotNull
        assertThat(info).isEqualTo(receivedCustomerInfo)
    }

    @Test
    fun `postReceipt calls backend once`() {
        mockPostReceiptResponseAndPost(
            backend,
            isRestore = false,
            observerMode = false,
            receiptInfo = basicReceiptInfo,
            storeAppUserID = null
        )

        verify(exactly = 1) {
            mockClient.performRequest(
                "/receipts",
                any(),
                any()
            )
        }
    }

    @Test
    fun `postReceipt passes pricing phases as maps in body`() {
        val subscriptionOption = storeProduct.subscriptionOptions[0]
        val receiptInfo = ReceiptInfo(
            productIDs = productIDs,
            storeProduct = storeProduct,
            subscriptionOptionId = subscriptionOption.id
        )

        mockPostReceiptResponseAndPost(
            backend,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null
        )

        val expectedPricingPhases = receiptInfo.pricingPhases
        val mappedExpectedPricingPhases = expectedPricingPhases?.map { it.toMap() }
        assertThat(requestBodySlot.isCaptured).isTrue
        assertThat(requestBodySlot.captured.keys).contains("pricing_phases")
        assertThat(requestBodySlot.captured["pricing_phases"]).isEqualTo(mappedExpectedPricingPhases)

        expectedPricingPhases?.forEachIndexed { index, pricingPhase ->
            val mappedPricingPhase = mappedExpectedPricingPhases?.get(index)
            assertThat(mappedPricingPhase).isNotNull.withFailMessage(
                "there should be a mapped version for every pricingPhase"
            )
            assertThat(mappedPricingPhase?.get("billingPeriod")).isEqualTo(pricingPhase.billingPeriod)
            assertThat(mappedPricingPhase?.get("billingCycleCount")).isEqualTo(pricingPhase.billingCycleCount)
            assertThat(mappedPricingPhase?.get("formattedPrice")).isEqualTo(pricingPhase.formattedPrice)
            assertThat(mappedPricingPhase?.get("priceCurrencyCode")).isEqualTo(pricingPhase.priceCurrencyCode)
            assertThat(mappedPricingPhase?.get("recurrenceMode")).isEqualTo(pricingPhase.recurrenceMode.identifier)
        }
    }

    @Test
    fun `postReceipt passes normal duration in body`() {
        val receiptInfo = ReceiptInfo(
            productIDs = productIDs,
            storeProduct = storeProduct
        )

        val expectedDuration = receiptInfo.duration

        mockPostReceiptResponseAndPost(
            backend,
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            receiptInfo = receiptInfo,
            storeAppUserID = null,
        )

        assertThat(requestBodySlot.isCaptured).isTrue
        assertThat(requestBodySlot.captured.keys).contains("normal_duration")
        assertThat(requestBodySlot.captured["normal_duration"]).isEqualTo(expectedDuration)
    }

    @Test
    fun `postReceipt passes store user ID in body`() {
        val receiptInfo = ReceiptInfo(
            productIDs = productIDs,
            storeProduct = storeProduct
        )

        val expectedStoreUserId = "id"

        mockPostReceiptResponseAndPost(
            backend,
            responseCode = 200,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = true,
            receiptInfo = receiptInfo,
            storeAppUserID = expectedStoreUserId,
        )

        assertThat(requestBodySlot.isCaptured).isTrue
        assertThat(requestBodySlot.captured.keys).contains("store_user_id")
        assertThat(requestBodySlot.captured["store_user_id"]).isEqualTo(expectedStoreUserId)
    }

    @Test
    fun postReceiptCallsFailsFor4XX() {
        mockPostReceiptResponseAndPost(
            backend,
            responseCode = 401,
            isRestore = false,
            clientException = null,
            resultBody = null,
            observerMode = false,
            receiptInfo = basicReceiptInfo,
            storeAppUserID = null
        )

        assertThat(receivedCustomerInfo).`as`("Received info is null").isNull()
        assertThat(receivedError).`as`("Received error is not null").isNotNull
    }

    @Test
    fun `given multiple post calls for same subscriber, only one is triggered`() {
        val lock = CountDownLatch(2)

        mockPostReceiptResponseAndPost(
            asyncBackend,
            isRestore = false,
            observerMode = false,
            delayed = true,
            receiptInfo = basicReceiptInfo,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )

        mockPostReceiptResponseAndPost(
            asyncBackend,
            isRestore = false,
            observerMode = false,
            delayed = true,
            receiptInfo = basicReceiptInfo,
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

        val lock = CountDownLatch(3)

        // Given calls

        asyncBackend.getCustomerInfo(appUserID, appInBackground = false, onSuccess = {
            assertThat(it).isEqualTo(initialInfo)
            lock.countDown()
        }, onError = onReceiveOfferingsErrorHandler)
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = basicReceiptInfo,
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
    fun postReceiptObserverMode() {
        val info = mockPostReceiptResponseAndPost(
            backend,
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
        val info = mockPostReceiptResponseAndPost(
            backend,
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
        val receiptInfo1 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct,
            subscriptionOptionId = "abc"
        )

        val receiptInfo2 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct,
            subscriptionOptionId = "ef"
        )

        val lock = CountDownLatch(2)
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo1,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo2,
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
        val receiptInfo1 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct
        )

        val originalSubscriptionOption = storeProduct.subscriptionOptions[0]
        val originalDuration = originalSubscriptionOption.pricingPhases[0].billingPeriod
        val subscriptionOption = stubSubscriptionOption(originalSubscriptionOption.id, originalDuration + "a")
        val storeProduct2 = stubStoreProduct(
            storeProduct.productId,
            subscriptionOption
        )

        val receiptInfo2 = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct2
        )

        val lock = CountDownLatch(2)
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo1,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo2,
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
    fun `given multiple post calls for same subscriber different offering, both are triggered`() {
        val lock = CountDownLatch(2)
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = basicReceiptInfo,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )

        val receiptInfo2 = ReceiptInfo(
            basicReceiptInfo.productIDs,
            basicReceiptInfo.offeringIdentifier + "a"
        )

        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo2,
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
    fun `given multiple post calls for same subscriber same durations, only one is triggered`() {
        val receiptInfo = ReceiptInfo(
            productIDs,
            offeringIdentifier = "offering_a",
            storeProduct = storeProduct
        )

        val lock = CountDownLatch(2)
        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )

        mockPostReceiptResponseAndPost(
            asyncBackend,
            delayed = true,
            isRestore = false,
            observerMode = false,
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
        val info = mockPostReceiptResponseAndPost(
            backend,
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
    fun `given multiple post calls for same subscriber different store user ID, both are triggered`() {
        val lock = CountDownLatch(2)
        val receiptInfo = ReceiptInfo(productIDs)

        mockPostReceiptResponseAndPost(
            asyncBackend,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo,
            storeAppUserID = null,
            delayed = true,
            onSuccess = { _, _ ->
                lock.countDown()
            },
            onError = postReceiptErrorCallback
        )

        mockPostReceiptResponseAndPost(
            asyncBackend,
            isRestore = false,
            observerMode = false,
            receiptInfo = receiptInfo,
            delayed = true,
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
    fun `postReceipt calls fail for multiple product ids errors`() {
        mockPostReceiptResponseAndPost(
            backend,
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
        assertThat(receivedShouldConsumePurchase).`as`("Purchase shouldn't be consumed").isFalse
    }

    @Test
    fun `postReceipt passes marketplace as header`() {
        mockPostReceiptResponseAndPost(
            backend,
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
        assertThat(headersSlot.captured.keys).contains("marketplace")
        assertThat(headersSlot.captured["marketplace"]).isEqualTo("DE")
    }

    // endregion

    // region getOfferings

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
        assertThat(receivedOfferingsJSON!!.getJSONArray("offerings").length()).isZero
        assertThat(receivedOfferingsJSON!!.getNullableString("current_offering_id")).isNull()
    }

    @Test
    fun encodesAppUserId() {
        val encodeableUserID = "userid with spaces"

        val encodedUserID = "userid%20with%20spaces"
        val path = "/subscribers/$encodedUserID/offerings"

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
        assertThat(calledWithRandomDelay).isNotNull
        assertThat(calledWithRandomDelay).isTrue
    }

    // endregion

    // region identity
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
            onLoginSuccessHandler
        ) {
            fail("Should have called success")
        }
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
            onLoginSuccessHandler
        ) {
            fail("Should have called success")
        }
        assertThat(receivedCustomerInfo).isEqualTo(expectedCustomerInfo)
        assertThat(receivedCustomerInfoCreated).isEqualTo(true)
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
            onLoginSuccessHandler
        ) {
            fail("Should have called success")
        }
        assertThat(receivedCustomerInfoCreated).isTrue
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
            onLoginSuccessHandler
        ) {
            fail("Should have called success")
        }
        assertThat(receivedCustomerInfoCreated).isFalse
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

    // region helpers

    private fun mockResponse(
        path: String,
        requestBody: Map<String, Any?>?,
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
                (if (requestBody == null) any() else capture(requestBodySlot)),
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

    private fun mockPostReceiptResponseAndPost(
        backend: Backend,
        token: String = fetchToken,
        responseCode: Int = 200,
        isRestore: Boolean,
        clientException: Exception? = null,
        resultBody: String? = null,
        observerMode: Boolean,
        receiptInfo: ReceiptInfo,
        storeAppUserID: String?,
        delayed: Boolean = false,
        marketplace: String? = null,
        onSuccess: (CustomerInfo, JSONObject?) -> Unit = onReceivePostReceiptSuccessHandler,
        onError: (PurchasesError, Boolean, JSONObject?) -> Unit = postReceiptErrorCallback
    ): CustomerInfo {
        val info = mockPostReceiptResponse(
            isRestore = isRestore,
            responseCode = responseCode,
            clientException = clientException,
            resultBody = resultBody,
            observerMode = observerMode,
            receiptInfo = receiptInfo,
            storeAppUserID = storeAppUserID,
            delayed = delayed
        )

        backend.postReceiptData(
            purchaseToken = token,
            appUserID = appUserID,
            isRestore = isRestore,
            observerMode = observerMode,
            subscriberAttributes = emptyMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = storeAppUserID,
            marketplace = marketplace,
            onSuccess = onSuccess,
            onError = onError
        )

        return info
    }

    private fun mockPostReceiptResponse(
        token: String = fetchToken,
        isRestore: Boolean,
        responseCode: Int,
        clientException: Exception?,
        resultBody: String?,
        delayed: Boolean = false,
        observerMode: Boolean,
        receiptInfo: ReceiptInfo,
        storeAppUserID: String?
    ): CustomerInfo {
        val body = mapOf(
            "fetch_token" to token,
            "app_user_id" to appUserID,
            "product_ids" to receiptInfo.productIDs,
            "is_restore" to isRestore,
            "presented_offering_identifier" to receiptInfo.offeringIdentifier,
            "observer_mode" to observerMode,
            "price" to receiptInfo.price,
            "currency" to receiptInfo.currency,
            "normal_duration" to receiptInfo.duration,
            "store_user_id" to storeAppUserID
        ).mapNotNull { entry: Map.Entry<String, Any?> ->
            entry.value?.let { entry.key to it }
        }.toMap()

        return mockResponse(
            "/receipts",
            body,
            responseCode,
            clientException,
            resultBody,
            delayed
        )
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

    // endregion
}
