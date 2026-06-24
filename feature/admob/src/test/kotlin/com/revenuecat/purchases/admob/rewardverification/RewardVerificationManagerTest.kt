@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.revenuecat.purchases.admob.rewardverification

import android.app.Activity
import android.os.Looper
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesServiceDispatcher
import com.revenuecat.purchases.admob.enableRewardVerification
import com.revenuecat.purchases.admob.show as showWithRewardVerification
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
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
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
    fun `verified result flows from enable through show to completed callback`() {
        val token = RewardVerificationToken(
            customData = "custom-data",
            clientTransactionId = "client-transaction-id",
            appUserID = "app-user-id",
        )
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.generateRewardVerificationToken("ad-response-id") } returns token
        val polledClientTransactionId = slot<String>()
        coEvery { mockPurchases.pollRewardVerification(capture(polledClientTransactionId)) } returns
            RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "gems", amount = 7))

        // Configure Purchases and drive the dispatcher so the reward verification runtime is created.
        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val ad = mockk<RewardedAd>(relaxed = true)
        every { ad.responseInfo.responseId } returns "ad-response-id"
        val ssvOptions = slot<ServerSideVerificationOptions>()
        every { ad.setServerSideVerificationOptions(capture(ssvOptions)) } answers {}
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.enableRewardVerification()

        // enableRewardVerification() must attach the token's custom data and user id so the backend can correlate.
        assertTrue(ssvOptions.isCaptured)
        assertEquals("app-user-id", ssvOptions.captured.userId)
        assertEquals("custom-data", ssvOptions.captured.customData)

        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)
        ad.showWithRewardVerification(activity = activity) { result ->
            completedResult = result
            completed.countDown()
        }
        rewardListenerSlot.captured.onUserEarnedReward(mockk<RewardItem>(relaxed = true))

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
    fun `interstitial verified result flows from enable through show with custom data correlation`() {
        val token = RewardVerificationToken(
            customData = "custom-data",
            clientTransactionId = "client-transaction-id",
            appUserID = "app-user-id",
        )
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.generateRewardVerificationToken("interstitial-response-id") } returns token
        val polledClientTransactionId = slot<String>()
        coEvery { mockPurchases.pollRewardVerification(capture(polledClientTransactionId)) } returns
            RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "coins", amount = 3))

        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceDispatcher.initialize(mockPurchases)

        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        every { ad.responseInfo.responseId } returns "interstitial-response-id"
        val ssvOptions = slot<ServerSideVerificationOptions>()
        every { ad.setServerSideVerificationOptions(capture(ssvOptions)) } answers {}
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.enableRewardVerification()

        assertTrue(ssvOptions.isCaptured)
        assertEquals("app-user-id", ssvOptions.captured.userId)
        assertEquals("custom-data", ssvOptions.captured.customData)

        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)
        ad.showWithRewardVerification(activity = activity) { result ->
            completedResult = result
            completed.countDown()
        }
        rewardListenerSlot.captured.onUserEarnedReward(mockk<RewardItem>(relaxed = true))

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
    fun `install before Purchases is configured does not store transaction id and show fails safely`() {
        // Intentionally skip backingFieldSharedInstance and dispatcher.initialize().
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.enableRewardVerification()

        // Without a stored client transaction id there is nothing to correlate, so no SSV data is attached.
        verify(exactly = 0) { ad.setServerSideVerificationOptions(any()) }

        var startedCount = 0
        var completedResult: RewardVerificationResult? = null
        ad.showWithRewardVerification(
            activity = activity,
            rewardVerificationStarted = { startedCount++ },
        ) { completedResult = it }
        rewardListenerSlot.captured.onUserEarnedReward(mockk<RewardItem>(relaxed = true))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, startedCount)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
    }
}
