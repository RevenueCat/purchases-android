package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.RewardVerificationResult
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
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
            poll = {
                pollStarted.countDown()
                awaitCancellation()
            },
        )
        val ad = Any()
        var started = false
        var completedResult: RewardVerificationResult? = null
        val completed = CountDownLatch(1)

        runtime.onPurchasesConfigured(mockk<Purchases>(relaxed = true))
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
        runtime.onPurchasesClosed(mockk<Purchases>(relaxed = true))
        val completionDelivered = (1..10).any {
            shadowOf(Looper.getMainLooper()).idle()
            completed.await(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(started)
        assertTrue(completionDelivered)
        assertNotNull(completedResult)
        assertTrue(completedResult!!.failed)
    }
}
