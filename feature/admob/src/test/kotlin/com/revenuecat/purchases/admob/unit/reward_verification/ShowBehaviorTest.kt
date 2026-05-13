@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.reward_verification

import android.app.Activity
import android.os.Looper
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.show as showWithRewardVerification
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShowBehaviorTest {

    @Test
    fun `show delivers failed result as fail-safe when reward verification is not enabled`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        var callbackCount = 0
        var latestResult: RewardVerificationResult? = null

        ad.showWithRewardVerification(activity = activity, placement = "placement") { result ->
            callbackCount++
            latestResult = result
        }
        rewardListenerSlot.captured.onUserEarnedReward(mockk<RewardItem>(relaxed = true))

        assertEquals(1, callbackCount)
        assertNotNull(latestResult)
        assertTrue(latestResult!!.failed)
    }

    @Test
    fun `show delivers result once on main thread even if rewarded callback fires twice`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        var callbackCount = 0
        var callbackThread: Thread? = null

        ad.showWithRewardVerification(activity = activity, placement = "placement") { result ->
            callbackCount++
            callbackThread = Thread.currentThread()
            assertTrue(result.failed)
        }

        val worker = Thread {
            val rewardItem = mockk<RewardItem>(relaxed = true)
            rewardListenerSlot.captured.onUserEarnedReward(rewardItem)
            rewardListenerSlot.captured.onUserEarnedReward(rewardItem)
        }
        worker.start()
        worker.join()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, callbackCount)
        assertSame(Looper.getMainLooper().thread, callbackThread)
    }
}
