package com.revenuecat.purchases.google.usecase

import android.app.Activity
import android.content.Intent
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.google.productId
import com.revenuecat.purchases.google.productList
import com.revenuecat.purchases.google.productType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.mockProductDetails
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class QueryProductDetailsUseCaseTest {

    private var onConnectedCalled: Boolean = false
    private var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    private var mockClient: BillingClient = mockk()
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null
    private var handler: Handler = mockk()
    private var mockDeviceCache: DeviceCache = mockk()
    private var mockDiagnosticsTracker: DiagnosticsTracker = mockk()
    private var mockDateProvider: DateProvider = mockk()

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    private var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    private var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    private lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<ProductDetails>

    private var storeProducts: List<StoreProduct>? = null

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private var mockActivity = mockk<Activity>()

    @Before
    fun setup() {
        storeProducts = null
        purchasesUpdatedListener = null
        billingClientStateListener = null

        mockRunnables()
        mockDiagnosticsTracker()
        every { mockDateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT

        val listenerSlot = slot<PurchasesUpdatedListener>()
        every {
            mockClientFactory.buildClient(capture(listenerSlot))
        } answers {
            purchasesUpdatedListener = listenerSlot.captured
            mockClient
        }

        val billingClientStateListenerSlot = slot<BillingClientStateListener>()
        every {
            mockClient.startConnection(capture(billingClientStateListenerSlot))
        } answers {
            billingClientStateListener = billingClientStateListenerSlot.captured
        }

        every {
            mockClient.endConnection()
        } just Runs

        every {
            mockClient.acknowledgePurchase(
                capture(capturedAcknowledgePurchaseParams),
                capture(capturedAcknowledgeResponseListener)
            )
        } just Runs

        mockConsumeAsync(billingClientOKResult)

        every {
            mockClient.isReady
        } returns false andThen true

        val featureSlot = slot<String>()
        every {
            mockClient.isFeatureSupported(capture(featureSlot))
        } returns billingClientOKResult

        mockDetailsList = listOf(mockProductDetails())

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache, mockDiagnosticsTracker, mockDateProvider)
        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.startConnectionOnMainThread()
        onConnectedCalled = false
        wrapper.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                onConnectedCalled = true
            }
        }

        every {
            mockActivity.intent
        } returns Intent()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun whenProductDetailsIsEmptyPassAnEmptyListToTheListener() {
        mockEmptyProductDetailsResponse()

        val productIDs = setOf("product_a")

        var receivedList: List<StoreProduct>? = null
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            productIDs, {
                receivedList = it
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })
        wrapper.onBillingSetupFinished(billingClientOKResult)
        Assertions.assertThat(receivedList).isNotNull
        Assertions.assertThat(receivedList!!.size).isZero
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
            ProductType.UNKNOWN,
            productIDs,
            {
                this@QueryProductDetailsUseCaseTest.storeProducts = it
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        Assertions.assertThat(slot.isCaptured).isTrue
        Assertions.assertThat(slot.captured.productList[0].productType).isEqualTo(BillingClient.ProductType.INAPP)
    }

    @Test
    fun `queryProductDetails filters empty productIds before querying BillingClient`() {
        val productIdsSet = setOf("abcd", "", "1", "")

        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(capture(slot), any())
        } just Runs

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            productIdsSet,
            {}, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        Assertions.assertThat(slot.captured).isNotNull
        val queryProductDetailsParamsProductList = slot.captured.productList
        val queriedProductIds = queryProductDetailsParamsProductList.map { it.productId }
        Assertions.assertThat(queriedProductIds).isEqualTo(productIdsSet.filter { it.isNotEmpty() })
    }

    @Test
    fun `queryProductDetails with empty list returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            emptySet(),
            {
                Assertions.assertThat(it.isEmpty())
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetails with only empty productIds returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("", ""),
            {
                Assertions.assertThat(it.isEmpty())
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

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
            ProductType.SUBS,
            setOf("asdf", "asdf"),
            {
                Thread.sleep(200)
                numCallbacks++
            }, {
                numCallbacks++
            })

        Assertions.assertThat(numCallbacks == 1)
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice in separate threads`() {
        var numCallbacks = 0

        val slot = slot<ProductDetailsResponseListener>()
        val lock = CountDownLatch(2)
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
            ProductType.SUBS,
            setOf("asdf"),
            {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                handler.post {
                    numCallbacks++
                }
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        lock.await()
        Assertions.assertThat(lock.count).isEqualTo(0)

        Assertions.assertThat(numCallbacks).isEqualTo(1)
    }

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

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockConsumeAsync(billingResult: BillingResult) {
        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } answers {
            capturedConsumeResponseListener.captured.onConsumeResponse(
                billingResult,
                capturedConsumeParams.captured.purchaseToken
            )
        }
    }

    private fun mockRunnables() {
        val slot = slot<Runnable>()
        every {
            handler.post(capture(slot))
        } answers {
            slot.captured.run()
            true
        }

        val delayedSlot = slot<Runnable>()
        every {
            handler.postDelayed(capture(delayedSlot), any())
        } answers {
            delayedSlot.captured.run()
            true
        }
    }

    private fun mockDiagnosticsTracker() {
        every {
            mockDiagnosticsTracker.trackGoogleQueryProductDetailsRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackProductDetailsNotSupported(any(), any())
        } just Runs
    }

}