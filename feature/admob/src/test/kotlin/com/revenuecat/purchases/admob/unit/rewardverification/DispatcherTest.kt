package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class DispatcherTest {

    @Test
    fun `deliverResultOnce invokes callback once when called repeatedly`() {
        val completionDelivered = AtomicBoolean(false)
        val receivedResults = mutableListOf<RewardVerificationResult>()

        Dispatcher.deliverResultOnce(
            consumeCompletionDeliveredToken = { completionDelivered.compareAndSet(false, true) },
            rewardVerificationResult = { receivedResults += it },
            result = RewardVerificationResult.failed,
            deliver = { it() },
        )
        Dispatcher.deliverResultOnce(
            consumeCompletionDeliveredToken = { completionDelivered.compareAndSet(false, true) },
            rewardVerificationResult = { receivedResults += it },
            result = RewardVerificationResult.failed,
            deliver = { it() },
        )

        assertEquals(1, receivedResults.size)
        assertTrue(receivedResults.single().failed)
    }
}
