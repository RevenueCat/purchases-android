@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.revenuecat.purchases.admob.nextgen.rewardverification

import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.ServerSideVerificationOptions
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesServiceDispatcher
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.rewardverification.Outcome
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationToken
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
        coEvery {
            mockPurchases.pollRewardVerification(
                capture(polledClientTransactionId),
                null,
                AdCaptureMethod.MANUAL,
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
        coEvery {
            mockPurchases.pollRewardVerification(
                capture(polledClientTransactionId),
                null,
                AdCaptureMethod.MANUAL,
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
