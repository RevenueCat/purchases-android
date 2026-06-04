package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
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
import org.robolectric.shadows.ShadowLog

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
        val adResponseId = "ad-response-id"
        var started = false
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
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
            poll = { RewardVerificationResult.verified(verifiedReward) },
        )
        val adResponseId = "ad-response-id"
        var startedThread: Thread? = null
        var completedThread: Thread? = null
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
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
    fun `verified virtual currency reward invalidates virtual currencies cache`() {
        var invalidationCount = 0
        val runtime = runtimeDeliveringResult(
            result = RewardVerificationResult.verified(VerifiedReward.VirtualCurrency(code = "gems", amount = 5)),
            invalidateVirtualCurrenciesCache = { invalidationCount++ },
        )

        deliverRewardEarned(runtime)

        assertEquals(1, invalidationCount)
    }

    @Test
    fun `verified no reward does not invalidate virtual currencies cache`() {
        var invalidationCount = 0
        val runtime = runtimeDeliveringResult(
            result = RewardVerificationResult.verified(VerifiedReward.NoReward),
            invalidateVirtualCurrenciesCache = { invalidationCount++ },
        )

        deliverRewardEarned(runtime)

        assertEquals(0, invalidationCount)
    }

    @Test
    fun `verified unsupported reward does not invalidate virtual currencies cache`() {
        var invalidationCount = 0
        val runtime = runtimeDeliveringResult(
            result = RewardVerificationResult.verified(VerifiedReward.UnsupportedReward),
            invalidateVirtualCurrenciesCache = { invalidationCount++ },
        )

        deliverRewardEarned(runtime)

        assertEquals(0, invalidationCount)
    }

    @Test
    fun `failed result does not invalidate virtual currencies cache`() {
        var invalidationCount = 0
        val runtime = runtimeDeliveringResult(
            result = RewardVerificationResult.failed,
            invalidateVirtualCurrenciesCache = { invalidationCount++ },
        )

        deliverRewardEarned(runtime)

        assertEquals(0, invalidationCount)
    }

    private fun runtimeDeliveringResult(
        result: RewardVerificationResult,
        invalidateVirtualCurrenciesCache: () -> Unit,
    ): RewardVerificationRuntime {
        return RewardVerificationRuntime(
            mainHandler = Handler(Looper.getMainLooper()),
            createVerificationScope = {
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            },
            poll = { result },
            invalidateVirtualCurrenciesCache = invalidateVirtualCurrenciesCache,
        )
    }

    private fun deliverRewardEarned(runtime: RewardVerificationRuntime) {
        val adResponseId = "ad-response-id"
        val completed = CountDownLatch(1)

        runtime.setClientTransactionId(adResponseId, "client-transaction-id")

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            rewardVerificationStarted = null,
            rewardVerificationCompleted = { completed.countDown() },
        )
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }
        assertTrue(completionDelivered)
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
        val adResponseId = "ad-response-id"
        var startedCount = 0
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        // Intentionally skip setClientTransactionId.

        runtime.handleRewardEarned(
            adResponseId = adResponseId,
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
