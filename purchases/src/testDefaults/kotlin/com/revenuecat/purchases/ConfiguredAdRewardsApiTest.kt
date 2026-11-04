package com.revenuecat.purchases

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ads.AdRewardConfiguration
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class ConfiguredAdRewardsApiTest : BasePurchasesTest() {
    private val configured = ConfiguredAdRewards(listOf(AdRewardConfiguration.VirtualCurrency("coins", 10)))

    @Test
    fun `callback delivers configured rewards empty rewards and unavailable on the main thread`() {
        val completion = slot<(ConfiguredAdRewards?) -> Unit>()
        every { mockConfiguredAdRewardsProvider.getRewards("AdUnit", capture(completion)) } returns Unit
        val thread = AtomicReference<Thread>()
        val results = mutableListOf<ConfiguredAdRewards?>()
        val callback = mockk<GetAdRewardsCallback>(relaxed = true)
        every { callback.onReceived(any()) } answers {
            thread.set(Thread.currentThread())
            results.add(firstArg())
            Unit
        }
        listOf(configured, ConfiguredAdRewards(emptyList()), null).forEach { rewards ->
            purchases.getAdRewards("AdUnit", callback)
            Thread { completion.captured(rewards) }.apply { start(); join() }
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertThat(thread.get()).isEqualTo(Looper.getMainLooper().thread)
        assertThat(results).containsExactly(configured, ConfiguredAdRewards(emptyList()), null)
    }

    @Test
    fun `coroutine returns configured rewards empty rewards and unavailable`() = runTest {
        listOf(configured, ConfiguredAdRewards(emptyList()), null).forEach { rewards ->
            every { mockConfiguredAdRewardsProvider.getRewards(any(), any()) } answers {
                secondArg<(ConfiguredAdRewards?) -> Unit>()(rewards)
            }
            assertThat(purchases.awaitGetAdRewards("unit")).isEqualTo(rewards)
        }
    }

    @Test
    fun `callbacks after coroutine cancellation are safe`() = runTest {
        val completion = slot<(ConfiguredAdRewards?) -> Unit>()
        every { mockConfiguredAdRewardsProvider.getRewards(any(), capture(completion)) } returns Unit
        listOf(configured, null).forEach { rewards ->
            val job = launch { purchases.awaitGetAdRewards("unit") }
            testScheduler.runCurrent()
            job.cancel()
            completion.captured(rewards)
            testScheduler.runCurrent()
            assertThat(job.isCancelled).isTrue()
        }
    }
}
