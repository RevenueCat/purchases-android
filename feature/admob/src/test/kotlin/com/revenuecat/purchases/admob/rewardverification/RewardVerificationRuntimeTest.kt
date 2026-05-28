package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class RewardVerificationRuntimeTest {

    @Test
    fun `cancelling runtime while verification is in flight completes with failed result`() {
        val pollStarted = CountDownLatch(1)
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = {
                pollStarted.countDown()
                awaitCancellation()
            },
        )
        val ad = Any()
        var started = false
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.initialize(mockk<Purchases>(relaxed = true))
        runtime.setClientTransactionId(ad, "client-transaction-id")

        runtime.handleRewardEarned(
            onAd = ad,
            rewardVerificationStarted = { started = true },
            rewardVerificationCompleted = {
                completedResult = it
                completed.countDown()
            },
        )
        assertTrue(pollStarted.await(1, TimeUnit.SECONDS))
        runtime.close(mockk<Purchases>(relaxed = true))
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(started)
        assertTrue(completionDelivered)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
    }

    @Test
    fun `verified poll result delivers started and verified reward on main thread`() {
        val verifiedReward = VerifiedReward.VirtualCurrency(code = "gems", amount = 5)
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { RewardVerificationResult.verified(verifiedReward) },
        )
        val ad = Any()
        var startedThread: Thread? = null
        var completedThread: Thread? = null
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.initialize(mockk<Purchases>(relaxed = true))
        runtime.setClientTransactionId(ad, "client-transaction-id")

        runtime.handleRewardEarned(
            onAd = ad,
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
    fun `missing client transaction id skips started callback and delivers failed`() {
        val runtime = RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { error("poll should not run when no client transaction id is registered") },
        )
        val ad = Any()
        var startedCount = 0
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.initialize(mockk<Purchases>(relaxed = true))
        // Intentionally skip setClientTransactionId.

        runtime.handleRewardEarned(
            onAd = ad,
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
    }
}
