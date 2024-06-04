//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.models.BillingFeature
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesCanMakePaymentsTest : BasePurchasesTest() {

    @Before
    fun setup() {
        mockHandlerPost()
        mockkStatic(BillingClient::class)
    }

    @After
    fun removeMocks() {
        unmockkStatic(BillingClient::class)
        unmockkConstructor(Handler::class)
    }

    // region canMakePayments

    @Test
    fun `when calling canMakePayments and billing service disconnects, return false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)

        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            receivedCanMakePayments = it
        }
        listener.captured.onBillingServiceDisconnected()
        assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `canMakePayments with no features and OK BillingResponse returns true`() {
        var receivedCanMakePayments = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(receivedCanMakePayments).isTrue
    }

    @Test
    fun `when no play services, canMakePayments returns false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>(relaxed = true)
        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when calling canMakePayments, enablePendingPurchases is called`() {
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        every { mockLocalBillingClient.startConnection(any()) } just Runs

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {}
        verify(exactly = 1) { mockBuilder.enablePendingPurchases() }
    }

    fun `canMakePayments returns true for Amazon configurations`() {
        purchases.purchasesOrchestrator.appConfig = AppConfig(
            mockContext,
            PurchasesAreCompletedBy.REVENUECAT,
            false,
            PlatformInfo("", null),
            null,
            Store.AMAZON
        )
        Purchases.canMakePayments(mockContext, listOf()) {
            assertThat(it).isTrue()
        }
    }

    @Test
    fun `when billing is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        mockHandlerPost()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.buildResult())

        assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when one feature in list is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        Purchases.canMakePayments(
            mockContext,
            listOf(
                BillingFeature.SUBSCRIPTIONS,
                BillingFeature.SUBSCRIPTIONS_UPDATE
            )
        ) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when single feature is supported and billing is supported, canMakePayments is true`() {
        var receivedCanMakePayments = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        mockHandlerPost()
        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(receivedCanMakePayments).isTrue
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature list is empty, canMakePayments does not check billing client for feature support`() {
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {}

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        verify(exactly = 0) { mockLocalBillingClient.isFeatureSupported(any()) }
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when calling canMakePayments and billing service disconnects twice, callback is called only once`() {
        var timesReceived = 0
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)

        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            timesReceived++
        }
        listener.captured.onBillingServiceDisconnected()
        listener.captured.onBillingServiceDisconnected()
        assertThat(timesReceived).isEqualTo(1)
        verify(exactly = 2) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `canMakePayments only calls callback once if onBillingSetupFinished is called twice`() {
        var timesReceived = 0
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            timesReceived++
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(timesReceived).isOne()
    }

    @Test
    fun `canMakePayments only calls callback once if onBillingServiceDisconnected after onBillingSetupFinished`() {
        var timesReceived = 0
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            timesReceived++
        }

        listener.captured.onBillingServiceDisconnected()
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(timesReceived).isOne()
    }

    // endregion

    // region Private Methods
    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun setUpMockBillingClientBuilderAndListener(
        mockLocalBillingClient: BillingClient,
    ): CapturingSlot<BillingClientStateListener> {
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        return listener
    }

    private fun mockHandlerPost() {
        mockkConstructor(Handler::class)
        val lst = slot<Runnable>()
        every {
            anyConstructed<Handler>().post(capture(lst))
        } answers {
            lst.captured.run()
            true
        }
    }

    // endregion

}
