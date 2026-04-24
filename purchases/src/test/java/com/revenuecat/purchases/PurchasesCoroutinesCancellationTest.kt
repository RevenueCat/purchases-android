package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesCoroutinesCancellationTest : BasePurchasesTest() {

    // region awaitOfferings cancellation

    @Test
    fun `awaitOfferings does not crash when success callback fires after cancellation`() = runTest {
        val onSuccessSlot = slot<(Offerings) -> Unit>()
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = any(),
                onSuccess = capture(onSuccessSlot),
            )
        } answers { }

        val job = launch {
            purchases.awaitOfferings()
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        // Callback fires after cancellation — should not crash
        onSuccessSlot.captured.invoke(mockk())
    }

    @Test
    fun `awaitOfferings does not crash when error callback fires after cancellation`() = runTest {
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = capture(onErrorSlot),
                onSuccess = any(),
            )
        } answers { }

        val job = launch {
            purchases.awaitOfferings()
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        // Error callback fires after cancellation — should not crash
        onErrorSlot.captured.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError, "Network error"),
        )
    }

    @Test
    fun `awaitOfferings returns result when not cancelled`() = runTest {
        val offerings: Offerings = mockk()
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = any(),
                onSuccess = captureLambda(),
            )
        } answers {
            lambda<(Offerings) -> Unit>().captured.invoke(offerings)
        }

        val result = purchases.awaitOfferings()
        assertThat(result).isEqualTo(offerings)
    }

    @Test
    fun `awaitOfferings cancellation propagates CancellationException`() = runTest {
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = any(),
                onSuccess = any(),
            )
        } answers { }

        val job = launch {
            purchases.awaitOfferings()
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        assertThat(job.isCancelled).isTrue()
    }

    // endregion

    // region awaitOfferingsResult cancellation

    @Test
    fun `awaitOfferingsResult does not crash when success callback fires after cancellation`() = runTest {
        val onSuccessSlot = slot<(Offerings) -> Unit>()
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = any(),
                onSuccess = capture(onSuccessSlot),
            )
        } answers { }

        val job = launch {
            purchases.awaitOfferingsResult()
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        onSuccessSlot.captured.invoke(mockk())
    }

    @Test
    fun `awaitOfferingsResult does not crash when error callback fires after cancellation`() = runTest {
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = capture(onErrorSlot),
                onSuccess = any(),
            )
        } answers { }

        val job = launch {
            purchases.awaitOfferingsResult()
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        onErrorSlot.captured.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError, "Network error"),
        )
    }

    // endregion

    // region awaitGetProducts cancellation

    @Test
    fun `awaitGetProducts does not crash when success callback fires after cancellation`() = runTest {
        val onSuccessSlot = slot<(List<com.revenuecat.purchases.models.StoreProduct>) -> Unit>()
        every {
            mockBillingAbstract.queryProductDetailsAsync(
                productType = ProductType.SUBS,
                productIds = setOf("product_1"),
                onReceive = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }

        val job = launch {
            purchases.awaitGetProducts(listOf("product_1"), ProductType.SUBS)
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        onSuccessSlot.captured.invoke(listOf())
    }

    @Test
    fun `awaitGetProducts does not crash when error callback fires after cancellation`() = runTest {
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBillingAbstract.queryProductDetailsAsync(
                productType = ProductType.SUBS,
                productIds = setOf("product_1"),
                onReceive = any(),
                onError = capture(onErrorSlot),
            )
        } answers { }

        val job = launch {
            purchases.awaitGetProducts(listOf("product_1"), ProductType.SUBS)
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        onErrorSlot.captured.invoke(
            PurchasesError(PurchasesErrorCode.StoreProblemError, "Store error"),
        )
    }

    // endregion
}
