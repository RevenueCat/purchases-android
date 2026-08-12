package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PaywallViewModelStoreHolderTest {

    @Test
    fun `overlapping leases share store and clear after final release`() {
        val holder = PaywallViewModelStoreHolder()
        val first = holder.acquire("paywall")
        val second = holder.acquire("paywall")
        val tracked = ViewModelProvider(first.owner)[TrackingViewModel::class.java]

        first.release(clear = true)

        assertThat(tracked.clearCount).isZero()
        assertThat(second.owner.viewModelStore).isSameAs(first.owner.viewModelStore)

        second.release(clear = true)

        assertThat(tracked.clearCount).isEqualTo(1)
    }

    @Test
    fun `configuration release retains store for next lease`() {
        val holder = PaywallViewModelStoreHolder()
        val first = holder.acquire("paywall")
        val tracked = ViewModelProvider(first.owner)[TrackingViewModel::class.java]

        first.release(clear = false)
        val second = holder.acquire("paywall")

        assertThat(ViewModelProvider(second.owner)[TrackingViewModel::class.java]).isSameAs(tracked)
    }

    @Test
    fun `normal release makes next presentation use new store`() {
        val holder = PaywallViewModelStoreHolder()
        val first = holder.acquire("paywall")
        val firstViewModel = ViewModelProvider(first.owner)[TrackingViewModel::class.java]

        first.release(clear = true)
        val second = holder.acquire("paywall")
        val secondViewModel = ViewModelProvider(second.owner)[TrackingViewModel::class.java]

        assertThat(firstViewModel.clearCount).isEqualTo(1)
        assertThat(secondViewModel).isNotSameAs(firstViewModel)
    }

    @Test
    fun `distinct keys with the same hash use separate stores`() {
        val holder = PaywallViewModelStoreHolder()
        val first = holder.acquire(CollidingKey())
        val second = holder.acquire(CollidingKey())

        assertThat(second.owner.viewModelStore).isNotSameAs(first.owner.viewModelStore)
    }

    @Test
    fun `releasing a lease twice does not clear an active sibling`() {
        val holder = PaywallViewModelStoreHolder()
        val first = holder.acquire("paywall")
        val second = holder.acquire("paywall")
        val tracked = ViewModelProvider(first.owner)[TrackingViewModel::class.java]

        first.release(clear = true)
        first.release(clear = true)

        assertThat(tracked.clearCount).isZero()

        second.release(clear = true)

        assertThat(tracked.clearCount).isEqualTo(1)
    }

    @Test
    fun `clearing parent defers active child cleanup until final release`() {
        val parentStore = ViewModelStore()
        val parentOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = parentStore
        }
        val holder = ViewModelProvider(parentOwner)[PaywallViewModelStoreHolder::class.java]
        val lease = holder.acquire("paywall")
        val tracked = ViewModelProvider(lease.owner)[TrackingViewModel::class.java]

        parentStore.clear()

        assertThat(tracked.clearCount).isZero()

        lease.release(clear = false)

        assertThat(tracked.clearCount).isEqualTo(1)
    }

    @Test
    fun `clearing parent clears child retained after configuration release`() {
        val parentStore = ViewModelStore()
        val parentOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = parentStore
        }
        val holder = ViewModelProvider(parentOwner)[PaywallViewModelStoreHolder::class.java]
        val lease = holder.acquire("paywall")
        val tracked = ViewModelProvider(lease.owner)[TrackingViewModel::class.java]
        lease.release(clear = false)

        parentStore.clear()

        assertThat(tracked.clearCount).isEqualTo(1)
    }

    private class CollidingKey {
        override fun hashCode(): Int = 0
    }

    class TrackingViewModel : ViewModel() {
        var clearCount = 0
            private set

        override fun onCleared() {
            clearCount++
        }
    }
}
