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
import com.revenuecat.purchases.PurchasesService
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import com.revenuecat.purchases.admob.enableRewardVerification
import com.revenuecat.purchases.admob.show as showWithRewardVerification
import com.revenuecat.purchases.interfaces.GetRewardVerificationResultCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
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
import com.revenuecat.purchases.RewardVerificationResult as CoreRewardVerificationResult
import com.revenuecat.purchases.VerifiedReward as CoreVerifiedReward

@RunWith(RobolectricTestRunner::class)
internal class RewardVerificationManagerTest {

    private lateinit var originalServiceForwarder: PurchasesService

    @Before
    fun setUp() {
        originalServiceForwarder = Purchases.serviceForwarder
    }

    @After
    fun tearDown() {
        // Close whatever Purchases mock the registry believes is configured so the singleton
        // runtime resets its state for subsequent tests.
        Purchases.backingFieldSharedInstance?.let { configured ->
            originalServiceForwarder.close(configured)
        }
        Purchases.backingFieldSharedInstance = null
        unmockkAll()
    }

    @Test
    fun `verified result flows from enable through show to completed callback via singleton`() {
        val verifiedReward = CoreVerifiedReward.VirtualCurrency(code = "gems", amount = 7)
        val mockPurchases = mockk<Purchases>(relaxed = true)
        val polledClientTransactionId = slot<String>()
        every {
            mockPurchases.getRewardVerificationResult(capture(polledClientTransactionId), any())
        } answers {
            secondArg<GetRewardVerificationResultCallback>().onReceived(
                CoreRewardVerificationResult.Verified(verifiedReward),
            )
        }

        // Configure Purchases and notify the default registry so the singleton manager's
        // runtime initializes.
        every { mockPurchases.currentConfiguration.apiKey } returns "test_api_key"
        every { mockPurchases.appUserID } returns "app-user-id"
        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceForwarder.initialize(mockPurchases)

        val ad = mockk<RewardedAd>(relaxed = true)
        every { ad.responseInfo.responseId } returns "ad-response-id"
        val ssvOptions = slot<ServerSideVerificationOptions>()
        every { ad.setServerSideVerificationOptions(capture(ssvOptions)) } answers {}
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.enableRewardVerification()

        // enableRewardVerification() must attach the api key, app user id, and a client transaction id so the backend
        // can correlate the reward.
        assertTrue(ssvOptions.isCaptured)
        assertEquals("app-user-id", ssvOptions.captured.userId)
        val payload = JSONObject(ssvOptions.captured.customData)
        assertEquals("test_api_key", payload.getString("api_key"))
        val attachedClientTransactionId = payload.getString("client_transaction_id")
        assertTrue(attachedClientTransactionId.isNotBlank())
        // The serialized payload must match the format shared with the other SDKs hitting the same endpoint.
        assertEquals(
            "{\"api_key\":\"test_api_key\",\"client_transaction_id\":\"$attachedClientTransactionId\"}",
            ssvOptions.captured.customData,
        )

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
        // The id polled from the backend must match the custom data attached to the ad so correlation round-trips.
        assertEquals(attachedClientTransactionId, polledClientTransactionId.captured)
    }

    @Test
    fun `interstitial verified result flows from enable through show with custom data correlation`() {
        val verifiedReward = CoreVerifiedReward.VirtualCurrency(code = "coins", amount = 3)
        val mockPurchases = mockk<Purchases>(relaxed = true)
        val polledClientTransactionId = slot<String>()
        every {
            mockPurchases.getRewardVerificationResult(capture(polledClientTransactionId), any())
        } answers {
            secondArg<GetRewardVerificationResultCallback>().onReceived(
                CoreRewardVerificationResult.Verified(verifiedReward),
            )
        }

        every { mockPurchases.currentConfiguration.apiKey } returns "test_api_key"
        every { mockPurchases.appUserID } returns "app-user-id"
        Purchases.backingFieldSharedInstance = mockPurchases
        originalServiceForwarder.initialize(mockPurchases)

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
        val payload = JSONObject(ssvOptions.captured.customData)
        assertEquals("test_api_key", payload.getString("api_key"))
        val attachedClientTransactionId = payload.getString("client_transaction_id")
        assertTrue(attachedClientTransactionId.isNotBlank())

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
        assertEquals(attachedClientTransactionId, polledClientTransactionId.captured)
    }

    @Test
    fun `install before Purchases is configured does not store transaction id and show fails safely`() {
        // Intentionally skip backingFieldSharedInstance and forwarder.initialize().
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
