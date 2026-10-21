package com.revenuecat.purchases

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ads.AdRewardConfiguration
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFailsWith

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class ConfiguredAdRewardsApiTest : BasePurchasesTest() {
    private val configured = ConfiguredAdRewards(AdRewardConfiguration.VirtualCurrency("coins", 10), emptyList())

    @Test
    fun `callback delivers configured rewards and no reward on the main thread`() {
        val completion = slot<(ConfiguredAdRewards) -> Unit>()
        every { mockConfiguredAdRewardsProvider.getRewards("AdUnit", capture(completion), any()) } returns Unit
        val thread = AtomicReference<Thread>()
        val results = mutableListOf<ConfiguredAdRewards>()
        val callback = mockk<GetAdRewardsCallback>(relaxed = true)
        every { callback.onReceived(any()) } answers {
            thread.set(Thread.currentThread())
            results.add(firstArg())
            Unit
        }
        listOf(configured, ConfiguredAdRewards(null, emptyList())).forEach { rewards ->
            purchases.getAdRewards("AdUnit", callback)
            Thread { completion.captured(rewards) }.apply { start(); join() }
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertThat(thread.get()).isEqualTo(Looper.getMainLooper().thread)
        assertThat(results).containsExactly(configured, ConfiguredAdRewards(null, emptyList()))
        verify(exactly = 0) { callback.onError(any()) }
    }

    @Test
    fun `callback preserves error diagnostics and runs on the main thread`() {
        val failure = PurchasesError(PurchasesErrorCode.UnknownError, "The configured ad rewards blob could not be resolved")
        val completion = slot<(PurchasesError) -> Unit>()
        every { mockConfiguredAdRewardsProvider.getRewards("unit", any(), capture(completion)) } returns Unit
        val thread = AtomicReference<Thread>()
        val callback = mockk<GetAdRewardsCallback>(relaxed = true)
        every { callback.onError(any()) } answers { thread.set(Thread.currentThread()) }
        purchases.getAdRewards("unit", callback)
        Thread { completion.captured(failure) }.apply { start(); join() }
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(thread.get()).isEqualTo(Looper.getMainLooper().thread)
        verify(exactly = 1) { callback.onError(failure) }
        verify(exactly = 0) { callback.onReceived(any()) }
    }

    @Test
    fun `coroutine returns configured rewards and no configured reward`() = runTest {
        listOf(configured, ConfiguredAdRewards(null, emptyList())).forEach { rewards ->
            every { mockConfiguredAdRewardsProvider.getRewards(any(), any(), any()) } answers {
                secondArg<(ConfiguredAdRewards) -> Unit>()(rewards)
            }
            assertThat(purchases.awaitGetAdRewards("unit")).isEqualTo(rewards)
        }
    }

    @Test
    fun `coroutine throws PurchasesException with provider error diagnostics`() = runTest {
        listOf(
            PurchasesError(PurchasesErrorCode.UnknownError, "The configured ad rewards blob could not be resolved"),
            PurchasesError(PurchasesErrorCode.UnknownError, "Expected an ad unit object"),
        ).forEach { failure ->
            every { mockConfiguredAdRewardsProvider.getRewards(any(), any(), any()) } answers {
                thirdArg<(PurchasesError) -> Unit>()(failure)
            }
            val exception = assertFailsWith<PurchasesException> { purchases.awaitGetAdRewards("unit") }
            assertThat(exception.error).isSameAs(failure)
        }
    }

    @Test
    fun `success and error callbacks after coroutine cancellation are safe`() = runTest {
        val success = slot<(ConfiguredAdRewards) -> Unit>()
        val error = slot<(PurchasesError) -> Unit>()
        every {
            mockConfiguredAdRewardsProvider.getRewards(any(), capture(success), capture(error))
        } returns Unit
        repeat(2) { index ->
            val job = launch { purchases.awaitGetAdRewards("unit") }
            testScheduler.runCurrent()
            job.cancel()
            if (index == 0) success.captured(configured)
            else error.captured(PurchasesError(PurchasesErrorCode.NetworkError))
            assertThat(job.isCancelled).isTrue()
        }
    }
}
