package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.BillingConfigResponseListener
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class GetBillingConfigUseCaseTest: BaseBillingUseCaseTest() {

    private val expectedCountryCode = "JP"

    private val mockConfig = mockk<BillingConfig>().apply {
        every { countryCode } returns expectedCountryCode
    }

    @Before
    override fun setup() {
        super.setup()
        every { mockDeviceCache.setStorefront(expectedCountryCode) } just Runs
        every { mockDeviceCache.getStorefront() } returns null
        every { mockDeviceCache.markInitialStorefrontLoadTracked() } returns true
    }

    @Test
    fun `first storefront load on a fresh install tracks the time since the billing wrapper was created`() {
        mockGetBillingConfig()
        every { mockDateProvider.now } returns Date(timestamp500)

        wrapper.getStorefront(onSuccess = { }, onError = { fail("Should succeed") })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleInitialStorefrontLoad(successful = true, responseTime = 500.milliseconds)
        }
    }

    @Test
    fun `failed first storefront load on a fresh install is tracked as unsuccessful`() {
        mockGetBillingConfig(BillingResponseCode.ERROR)
        every { mockDateProvider.now } returns Date(timestamp123)

        wrapper.getStorefront(onSuccess = { fail("Should error") }, onError = { })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleInitialStorefrontLoad(successful = false, responseTime = 123.milliseconds)
        }
    }

    @Test
    fun `storefront load is not tracked when already tracked on this device`() {
        every { mockDeviceCache.markInitialStorefrontLoadTracked() } returns false
        mockGetBillingConfig()

        wrapper.getStorefront(onSuccess = { }, onError = { fail("Should succeed") })

        verify(exactly = 0) { mockDiagnosticsTracker.trackGoogleInitialStorefrontLoad(any(), any()) }
    }

    @Test
    fun `storefront load is not tracked when a storefront was already cached`() {
        every { mockDeviceCache.getStorefront() } returns "ES"
        mockGetBillingConfig()

        wrapper.getStorefront(onSuccess = { }, onError = { fail("Should succeed") })

        verify(exactly = 0) { mockDiagnosticsTracker.trackGoogleInitialStorefrontLoad(any(), any()) }
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
    fun `querying store country code stores country code in cache`() {
        mockGetBillingConfig()
        wrapper.getStorefront(
            onSuccess = { },
            onError = { fail("Should succeed") }
        )
        verify(exactly = 1) { mockDeviceCache.setStorefront(expectedCountryCode) }
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

    @Test
    fun `querying store country code does not store country code on error`() {
        mockGetBillingConfig(BillingResponseCode.ERROR)
        wrapper.getStorefront(
            onSuccess = { fail("Should error") },
            onError = { }
        )
        verify(exactly = 0) { mockDeviceCache.setStorefront(expectedCountryCode) }
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
