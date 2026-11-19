package com.revenuecat.purchases.common

import com.revenuecat.purchases.interfaces.StorefrontProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StorefrontAwaiterTest {

    private var cachedStorefront: String? = null
    private val storefrontProvider = mockk<StorefrontProvider>().also {
        every { it.getStorefront() } answers { cachedStorefront }
    }
    private val scheduledTimeouts = mutableListOf<Pair<Duration, () -> Unit>>()
    private val awaiter = StorefrontAwaiter(
        storefrontProvider = storefrontProvider,
        scheduleTimeout = { delay, task -> scheduledTimeouts.add(delay to task) },
        timeout = 1500.milliseconds,
    )

    @Test
    fun `runs immediately when the storefront is already cached`() {
        cachedStorefront = "US"
        var ran = false

        awaiter.runWhenStorefrontAvailable { ran = true }

        assertThat(ran).isTrue
        assertThat(scheduledTimeouts).isEmpty()
    }

    @Test
    fun `holds actions until released`() {
        val ran = mutableListOf<Int>()

        awaiter.runWhenStorefrontAvailable { ran.add(1) }
        awaiter.runWhenStorefrontAvailable { ran.add(2) }
        assertThat(ran).isEmpty()

        awaiter.release()

        assertThat(ran).containsExactly(1, 2)
    }

    @Test
    fun `releases held actions when the timeout fires`() {
        var ran = false

        awaiter.runWhenStorefrontAvailable { ran = true }
        assertThat(scheduledTimeouts).hasSize(1)
        assertThat(scheduledTimeouts.single().first).isEqualTo(1500.milliseconds)

        scheduledTimeouts.single().second()

        assertThat(ran).isTrue
    }

    @Test
    fun `schedules a single timeout for several held actions`() {
        awaiter.runWhenStorefrontAvailable { }
        awaiter.runWhenStorefrontAvailable { }

        assertThat(scheduledTimeouts).hasSize(1)
    }

    @Test
    fun `never waits again after being released`() {
        awaiter.release()
        var ran = false

        awaiter.runWhenStorefrontAvailable { ran = true }

        assertThat(ran).isTrue
        assertThat(scheduledTimeouts).isEmpty()
        verify(exactly = 0) { storefrontProvider.getStorefront() }
    }

    @Test
    fun `never waits again after finding a cached storefront`() {
        cachedStorefront = "US"
        awaiter.runWhenStorefrontAvailable { }
        cachedStorefront = null
        var ran = false

        awaiter.runWhenStorefrontAvailable { ran = true }

        assertThat(ran).isTrue
        assertThat(scheduledTimeouts).isEmpty()
    }

    @Test
    fun `runs each held action once when released by both billing and the timeout`() {
        var runs = 0
        awaiter.runWhenStorefrontAvailable { runs++ }

        awaiter.release()
        scheduledTimeouts.single().second()

        assertThat(runs).isEqualTo(1)
    }
}
