package com.revenuecat.purchases.google.usecase

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.BillingConfigResponseListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.utils.MockHandlerFactory
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class GetBillingConfigUseCaseTest {

    private val expectedCountryCode = "JP"

    private val mockConfig = mockk<BillingConfig>().apply {
        every { countryCode } returns expectedCountryCode
    }

    private var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    private var mockClient: BillingClient = mockk()
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null

    private lateinit var handler: Handler

    private lateinit var wrapper: BillingWrapper

    @Before
    fun setup() {
        handler = MockHandlerFactory.createMockHandler()
        purchasesUpdatedListener = null
        billingClientStateListener = null

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
            mockClient.isReady
        } returns false andThen true

        wrapper = BillingWrapper(mockClientFactory, handler, mockk(), mockk(), mockk())
        wrapper.purchasesUpdatedListener = mockk()
        wrapper.startConnectionOnMainThread()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `querying storefront success`() {
        mockGetBillingConfig()
        var countryCode: String? = null
        wrapper.getStorefront(
            onSuccess = { countryCode = it },
            onError = { fail("Should succeed") }
        )
        assertThat(countryCode).isEqualTo(expectedCountryCode)
    }

    @Test
    fun `querying storefront error code`() {
        mockGetBillingConfig(BillingResponseCode.ERROR)
        var error: PurchasesError? = null
        wrapper.getStorefront(
            onSuccess = { fail("Should error") },
            onError = { error = it }
        )
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `querying storefront only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val billingResult = BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build()
        val listenerSlot = slot<BillingConfigResponseListener>()
        every {
            mockClient.getBillingConfigAsync(any(), capture(listenerSlot))
        } answers {
            listenerSlot.captured.onBillingConfigResponse(billingResult, mockConfig)
            listenerSlot.captured.onBillingConfigResponse(billingResult, mockConfig)
        }

        wrapper.getStorefront(
            onSuccess = { numCallbacks++ },
            onError = { numCallbacks++ }
        )

        assertThat(numCallbacks).isEqualTo(1)
    }

    private fun mockGetBillingConfig(
        billingResponseCode: Int = BillingResponseCode.OK,
        billingConfig: BillingConfig = mockConfig
    ) {
        val billingResult = BillingResult.newBuilder().setResponseCode(billingResponseCode).build()
        val listenerSlot = slot<BillingConfigResponseListener>()
        every {
            mockClient.getBillingConfigAsync(any(), capture(listenerSlot))
        } answers {
            listenerSlot.captured.onBillingConfigResponse(billingResult, billingConfig)
        }
    }
}
