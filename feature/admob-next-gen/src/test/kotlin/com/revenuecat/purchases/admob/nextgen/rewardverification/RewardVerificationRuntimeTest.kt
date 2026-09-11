package com.revenuecat.purchases.admob.nextgen.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.ads.rewardverification.RewardedAdTrackingMetadata
import com.revenuecat.purchases.ads.rewardverification.VerifiedReward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class RewardVerificationRuntimeTest {

    @Test
    fun `cancelling runtime while verification is in flight completes with failed result`() {
        val pollStarted = CountDownLatch(1)
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { _, _ ->
                pollStarted.countDown()
                awaitCancellation()
            },
        )
        val adResponseId = "ad-response-id"
        var started = false
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            trackingMetadata = null,
            rewardVerificationStarted = { started = true },
            rewardVerificationCompleted = {
                completedResult = it
                completed.countDown()
            },
        )
        assertTrue(pollStarted.await(1, TimeUnit.SECONDS))
        runtime.close()
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(started)
        assertTrue(completionDelivered)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
        // Cancellation is logged so it isn't bucketed as a silent failure.
        assertTrue(ShadowLog.getLogs().any { it.msg == RewardVerificationStrings.CANCELLED })
    }

    @Test
    fun `verified poll result delivers started and verified reward on main thread`() {
        val verifiedReward = VerifiedReward.VirtualCurrency(code = "gems", amount = 5)
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { _, _ -> RewardVerificationResult.verified(verifiedReward) },
        )
        val adResponseId = "ad-response-id"
        var startedThread: Thread? = null
        var completedThread: Thread? = null
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            trackingMetadata = null,
            rewardVerificationStarted = { startedThread = Thread.currentThread() },
            rewardVerificationCompleted = {
                completedThread = Thread.currentThread()
                completedResult = it
                completed.countDown()
            },
        )
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(completionDelivered)
        assertSame(Looper.getMainLooper().thread, startedThread)
        assertSame(Looper.getMainLooper().thread, completedThread)
        assertNotNull(completedResult)
        assertFalse(completedResult!!.failed)
        assertEquals(verifiedReward, completedResult!!.verifiedReward)
    }

    @Test
    fun `missing ad response id skips started callback and delivers failed`() {
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { _, _ -> error("poll should not run when ad response id is missing") },
        )
        var startedCount = 0
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.handleRewardEarned(
            adResponseId = null,
            trackingMetadata = null,
            rewardVerificationStarted = { startedCount++ },
            rewardVerificationCompleted = {
                completedResult = it
                completed.countDown()
            },
        )
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(completionDelivered)
        assertEquals(0, startedCount)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
        assertTrue(ShadowLog.getLogs().any { it.msg == RewardVerificationStrings.NOT_SET_UP_FOR_AD })
    }

    @Test
    fun `missing client transaction id skips started callback and delivers failed`() {
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { _, _ -> error("poll should not run when no client transaction id is registered") },
        )
        val adResponseId = "ad-response-id"
        var startedCount = 0
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        // Intentionally skip setClientTransactionId.

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            trackingMetadata = null,
            rewardVerificationStarted = { startedCount++ },
            rewardVerificationCompleted = {
                completedResult = it
                completed.countDown()
            },
        )
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(completionDelivered)
        assertEquals(0, startedCount)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
        assertTrue(ShadowLog.getLogs().any { it.msg == RewardVerificationStrings.NOT_SET_UP_FOR_AD })
    }

    @Test
    fun `handleRewardEarned forwards tracking metadata to poll`() {
        val trackingMetadata = RewardedAdTrackingMetadata(
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
        )
        var receivedTrackingMetadata: RewardedAdTrackingMetadata? = null
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { _, metadata ->
                receivedTrackingMetadata = metadata
                RewardVerificationResult.failed
            },
        )
        val adResponseId = "ad-response-id"
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            trackingMetadata = trackingMetadata,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { completed.countDown() },
        )
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(completionDelivered)
        assertSame(trackingMetadata, receivedTrackingMetadata)
    }

    @Test
    fun `handleRewardEarned forwards null tracking metadata when the ad was not tracked`() {
        var receivedTrackingMetadata: RewardedAdTrackingMetadata? = RewardedAdTrackingMetadata(
            networkName = null,
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = null,
            adUnitId = "sentinel",
            impressionId = "sentinel",
        )
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { _, metadata ->
                receivedTrackingMetadata = metadata
                RewardVerificationResult.failed
            },
        )
        val adResponseId = "ad-response-id"
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            trackingMetadata = null,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { completed.countDown() },
        )
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(completionDelivered)
        assertNull(receivedTrackingMetadata)
    }
}
