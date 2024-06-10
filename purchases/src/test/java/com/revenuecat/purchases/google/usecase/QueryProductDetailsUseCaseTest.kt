package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.google.productId
import com.revenuecat.purchases.google.productList
import com.revenuecat.purchases.google.productType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.mockProductDetails
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.AssertionsForClassTypes
import org.assertj.core.data.Offset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class QueryProductDetailsUseCaseTest: BaseBillingUseCaseTest() {

    private lateinit var mockDetailsList: List<ProductDetails>

    private var storeProducts: List<StoreProduct>? = null

    @Before
    override fun setup() {
        super.setup()
        mockDetailsList = listOf(mockProductDetails())
    }

    @Test
    fun whenProductDetailsIsEmptyPassAnEmptyListToTheListener() {
        mockEmptyProductDetailsResponse()

        val productIDs = setOf("product_a")

        var receivedList: List<StoreProduct>? = null
        wrapper.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = productIDs,
            onReceive = {
                receivedList = it
            },
            onError = {
                AssertionsForClassTypes.fail("shouldn't be an error")
            },
        )

        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isZero
    }

    @Test
    fun `product type defaults to INAPP when querying product details`() {
        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(
                capture(slot),
                any()
            )
        } just Runs

        val productIDs = setOf("product_a")

        wrapper.queryProductDetailsAsync(
            productType = ProductType.UNKNOWN,
            productIds = productIDs,
            onReceive = {
                this@QueryProductDetailsUseCaseTest.storeProducts = it
            },
            onError = {
                AssertionsForClassTypes.fail("shouldn't be an error")
            },
        )

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.productList[0].productType).isEqualTo(BillingClient.ProductType.INAPP)
    }

    @Test
    fun `queryProductDetails filters empty productIds before querying BillingClient`() {
        val productIdsSet = setOf("abcd", "", "1", "")

        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(capture(slot), any())
        } just Runs

        wrapper.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = productIdsSet,
            onReceive = {},
            onError = {
                AssertionsForClassTypes.fail("shouldn't be an error")
            },
        )

        assertThat(slot.captured).isNotNull
        val queryProductDetailsParamsProductList = slot.captured.productList
        val queriedProductIds = queryProductDetailsParamsProductList.map { it.productId }
        assertThat(queriedProductIds).isEqualTo(productIdsSet.filter { it.isNotEmpty() })
    }

    @Test
    fun `queryProductDetails with empty list returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = emptySet(),
            onReceive = {
                assertThat(it).isEmpty()
            },
            onError = {
                AssertionsForClassTypes.fail("shouldn't be an error")
            },
        )

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetails with only empty productIds returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = setOf("", ""),
            onReceive = {
                assertThat(it).isEmpty()
            },
            onError = {
                AssertionsForClassTypes.fail("shouldn't be an error")
            },
        )

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
        }

        wrapper.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = setOf("asdf", "asdf"),
            onReceive = {
                Thread.sleep(200)
                numCallbacks++
            },
            onError = {
                numCallbacks++
            },
        )

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice in separate threads`() {
        val numCallbacks = AtomicInteger(0)

        val slot = slot<ProductDetailsResponseListener>()
        val lock = CountDownLatch(3)
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()
        }

        wrapper.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = setOf("asdf"),
            onReceive = {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                numCallbacks.incrementAndGet()
                lock.countDown()
            },
            onError = {
                AssertionsForClassTypes.fail("shouldn't be an error")
            },
        )

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks.get()).isEqualTo(1)
    }

    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedList: List<StoreProduct>? = null
        var timesExecutedInMainThread = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
                appInBackground = false
            ),
            { received ->
                receivedList = received
            },
            { _ ->
                fail("shouldn't be an error")
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                timesExecutedInMainThread++

                queryProductDetailsStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onProductDetailsResponse(
                            billingClientDisconnectedResult,
                            emptyList()
                        )
                    } else {
                        slot.captured.onProductDetailsResponse(
                            billingClientOKResult,
                            mockDetailsList
                        )
                    }
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesExecutedInMainThread).isEqualTo(2)
        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isOne
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute a max of 3 times`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
                appInBackground = false
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
                appInBackground = true
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { delay, request ->
                capturedDelays.add(delay)
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(capturedDelays.size).isEqualTo(12)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, retry with backoff a few times then error if user in session`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val capturedDelays = mutableListOf<Long>()
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
                appInBackground = false
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { delay, request ->
                capturedDelays.add(delay)
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS_FOREGROUND, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
                appInBackground = false
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
                appInBackground = false
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

    private fun mockEmptyProductDetailsResponse() {
        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
        }
    }

}
