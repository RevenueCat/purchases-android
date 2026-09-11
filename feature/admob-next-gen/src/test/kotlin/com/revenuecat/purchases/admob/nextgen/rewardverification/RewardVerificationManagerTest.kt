@file:OptIn(InternalRevenueCatAPI::class)
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.revenuecat.purchases.admob.nextgen.rewardverification

import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.ServerSideVerificationOptions
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesServiceDispatcher
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.rewardverification.Outcome
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationToken
import com.revenuecat.purchases.ads.rewardverification.RewardedAdTrackingMetadata
import com.revenuecat.purchases.ads.rewardverification.VerifiedReward
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class RewardVerificationManagerTest {

    private lateinit var originalServiceDispatcher: PurchasesServiceDispatcher

    @Before
    fun setUp() {
        originalServiceDispatcher = Purchases.serviceDispatcher
    }

    @After
    fun tearDown() {
        // Close through the dispatcher so the per-configuration runtime is torn down for the next test.
        Purchases.backingFieldSharedInstance?.let { configured ->
            originalServiceDispatcher.close(configured)
        }
        Purchases.backingFieldSharedInstance = null
        unmockkAll()
    }

    @Test
    fun `verified result flows from install through reward earned to completed callback`() {
        val token = RewardVerificationToken(
            customData = "custom-data",
            clientTransactionId = "client-transaction-id",
            appUserID = "app-user-id",
        )
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.generateRewardVerificationToken("ad-response-id") } returns token
        val polledClientTransactionId = slot<String>()
        val polledTrackingMetadata = slot<RewardedAdTrackingMetadata?>()
        val polledCaptureMethod = slot<AdCaptureMethod>()
        coEvery {
            mockPurchases.pollRewardVerification(
                capture(polledClientTransactionId),
                captureNullable(polledTrackingMetadata),
                capture(polledCaptureMethod),
                any<suspend (String) -> Outcome>(),
            )
        } returns RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "gems", amount = 7))

        // Configure Purchases and drive the dispatcher so the reward verification runtime is created.
        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val ad = mockk<RewardedAd>(relaxed = true)
        every { ad.getResponseInfo().responseId } returns "ad-response-id"
        val ssvOptions = slot<ServerSideVerificationOptions>()
        every { ad.setServerSideVerificationOptions(capture(ssvOptions)) } answers {}

        RewardVerificationManager.install(ad)

        // install() must attach the token's custom data and user id so the backend can correlate.
        assertTrue(ssvOptions.isCaptured)
        assertEquals("app-user-id", ssvOptions.captured.userId)
        assertEquals("custom-data", ssvOptions.captured.customData)

        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)
        RewardVerificationManager.handleRewardEarned(
            ad = ad,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { result ->
                completedResult = result
                completed.countDown()
            },
        )

        val delivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(delivered)
        assertNotNull(completedResult)
        assertFalse(completedResult!!.failed)
        assertEquals(
            VerifiedReward.VirtualCurrency(code = "gems", amount = 7),
            completedResult!!.verifiedReward,
        )
        // The id polled from the backend must match the token's client transaction id so correlation round-trips.
        assertEquals("client-transaction-id", polledClientTransactionId.captured)
        assertNull(polledTrackingMetadata.captured)
        assertEquals(AdCaptureMethod.ADAPTER, polledCaptureMethod.captured)
    }

    @Test
    fun `interstitial verified result flows from install through reward earned with custom data correlation`() {
        val token = RewardVerificationToken(
            customData = "custom-data",
            clientTransactionId = "client-transaction-id",
            appUserID = "app-user-id",
        )
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.generateRewardVerificationToken("interstitial-response-id") } returns token
        val polledClientTransactionId = slot<String>()
        val polledTrackingMetadata = slot<RewardedAdTrackingMetadata?>()
        val polledCaptureMethod = slot<AdCaptureMethod>()
        coEvery {
            mockPurchases.pollRewardVerification(
                capture(polledClientTransactionId),
                captureNullable(polledTrackingMetadata),
                capture(polledCaptureMethod),
                any<suspend (String) -> Outcome>(),
            )
        } returns RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "coins", amount = 3))

        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        every { ad.getResponseInfo().responseId } returns "interstitial-response-id"
        val ssvOptions = slot<ServerSideVerificationOptions>()
        every { ad.setServerSideVerificationOptions(capture(ssvOptions)) } answers {}

        RewardVerificationManager.install(ad)

        assertTrue(ssvOptions.isCaptured)
        assertEquals("app-user-id", ssvOptions.captured.userId)
        assertEquals("custom-data", ssvOptions.captured.customData)

        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)
        RewardVerificationManager.handleRewardEarned(
            ad = ad,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { result ->
                completedResult = result
                completed.countDown()
            },
        )

        val delivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(delivered)
        assertNotNull(completedResult)
        assertFalse(completedResult!!.failed)
        assertEquals("client-transaction-id", polledClientTransactionId.captured)
        assertNull(polledTrackingMetadata.captured)
        assertEquals(AdCaptureMethod.ADAPTER, polledCaptureMethod.captured)
    }

    @Test
    fun `tracked ad forwards reward metadata with the latest placement to the poll`() {
        val token = RewardVerificationToken(
            customData = "custom-data",
            clientTransactionId = "client-transaction-id",
            appUserID = "app-user-id",
        )
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.generateRewardVerificationToken("ad-response-id") } returns token
        val polledTrackingMetadata = slot<RewardedAdTrackingMetadata?>()
        coEvery {
            mockPurchases.pollRewardVerification(
                any(),
                captureNullable(polledTrackingMetadata),
                AdCaptureMethod.ADAPTER,
                any<suspend (String) -> Outcome>(),
            )
        } returns RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "gems", amount = 7))

        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.responseId } returns "ad-response-id"
        every { responseInfo.adapterClassName } returns "com.example.SomeAdapter"
        val trackingCallback = TrackingRewardedAdEventCallback(
            initialDelegate = null,
            initialPlacement = "load-time-placement",
            adUnitId = "ad-unit-id",
            responseInfoProvider = { responseInfo },
        )
        val ad = mockk<RewardedAd>(relaxed = true)
        every { ad.getResponseInfo() } returns responseInfo
        every { ad.adEventCallback } returns trackingCallback

        RewardVerificationManager.install(ad)
        trackingCallback.placement = "show-time-placement"

        val completed = CountDownLatch(1)
        RewardVerificationManager.handleRewardEarned(
            ad = ad,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { completed.countDown() },
        )
        val delivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(delivered)
        assertEquals(
            RewardedAdTrackingMetadata(
                networkName = "com.example.SomeAdapter",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "show-time-placement",
                adUnitId = "ad-unit-id",
                impressionId = "ad-response-id",
            ),
            polledTrackingMetadata.captured,
        )
    }

    @Test
    fun `install with configured Purchases and missing ad response id does not attach options`() {
        val mockPurchases = mockk<Purchases>(relaxed = true)
        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val ad = mockk<RewardedAd>(relaxed = true)
        every { ad.getResponseInfo().responseId } returns null

        RewardVerificationManager.install(ad)

        verify(exactly = 0) { ad.setServerSideVerificationOptions(any()) }
        assertTrue(ShadowLog.getLogs().any { it.msg == RewardVerificationStrings.MISSING_AD_RESPONSE_ID })
    }

    @Test
    fun `install with configured Purchases and unavailable runtime does not attach options`() {
        val mockPurchases = mockk<Purchases>(relaxed = true)
        Purchases.backingFieldSharedInstance = mockPurchases
        // Intentionally skip dispatcher.initialize() so there is no runtime for this configuration.

        val ad = mockk<RewardedAd>(relaxed = true)
        every { ad.getResponseInfo().responseId } returns "ad-response-id"

        RewardVerificationManager.install(ad)

        verify(exactly = 0) { ad.setServerSideVerificationOptions(any()) }
        assertTrue(ShadowLog.getLogs().any { it.msg == RewardVerificationStrings.RUNTIME_NOT_READY })
    }

    @Test
    fun `tracked interstitial forwards reward metadata with its format and latest placement to the poll`() {
        val token = RewardVerificationToken(
            customData = "custom-data",
            clientTransactionId = "client-transaction-id",
            appUserID = "app-user-id",
        )
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.generateRewardVerificationToken("interstitial-response-id") } returns token
        val polledTrackingMetadata = slot<RewardedAdTrackingMetadata?>()
        coEvery {
            mockPurchases.pollRewardVerification(
                any(),
                captureNullable(polledTrackingMetadata),
                AdCaptureMethod.ADAPTER,
                any<suspend (String) -> Outcome>(),
            )
        } returns RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "coins", amount = 3))

        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.responseId } returns "interstitial-response-id"
        every { responseInfo.adapterClassName } returns "com.example.InterstitialAdapter"
        val trackingCallback = TrackingRewardedInterstitialAdEventCallback(
            initialDelegate = null,
            initialPlacement = "load-time-placement",
            adUnitId = "interstitial-ad-unit-id",
            responseInfoProvider = { responseInfo },
        )
        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        every { ad.getResponseInfo() } returns responseInfo
        every { ad.adEventCallback } returns trackingCallback

        RewardVerificationManager.install(ad)
        trackingCallback.placement = "show-time-placement"

        val completed = CountDownLatch(1)
        RewardVerificationManager.handleRewardEarned(
            ad = ad,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { completed.countDown() },
        )
        val delivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(delivered)
        assertEquals(
            RewardedAdTrackingMetadata(
                networkName = "com.example.InterstitialAdapter",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED_INTERSTITIAL,
                placement = "show-time-placement",
                adUnitId = "interstitial-ad-unit-id",
                impressionId = "interstitial-response-id",
            ),
            polledTrackingMetadata.captured,
        )
    }

    @Test
    fun `install before Purchases is configured does not store transaction id and reward earned fails safely`() {
        // Intentionally skip backingFieldSharedInstance and dispatcher.initialize().
        val ad = mockk<RewardedAd>(relaxed = true)

        RewardVerificationManager.install(ad)

        // Without a stored client transaction id there is nothing to correlate, so no SSV data is attached.
        verify(exactly = 0) { ad.setServerSideVerificationOptions(any()) }

        var startedCount = 0
        var completedResult: RewardVerificationResult? = null
        RewardVerificationManager.handleRewardEarned(
            ad = ad,
            rewardVerificationStarted = { startedCount++ },
            rewardVerificationCompleted = { completedResult = it },
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, startedCount)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
    }
}
