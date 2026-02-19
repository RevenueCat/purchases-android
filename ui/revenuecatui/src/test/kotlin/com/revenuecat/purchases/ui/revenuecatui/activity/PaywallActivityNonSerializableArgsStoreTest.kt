package com.revenuecat.purchases.ui.revenuecatui.activity

import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class PaywallActivityNonSerializableArgsStoreTest {

    @After
    fun tearDown() {
        PaywallActivityNonSerializableArgsStore.clear()
    }

    @Test
    fun `store returns key and get retrieves by that key`() {
        val args = PaywallActivityNonSerializableArgs(
            purchaseLogic = mockk<PurchaseLogic>(),
            listener = mockk<PaywallListener>(),
        )
        val key = PaywallActivityNonSerializableArgsStore.store(args)

        val retrieved = PaywallActivityNonSerializableArgsStore.get(key)

        assertThat(retrieved).isSameAs(args)
    }

    @Test
    fun `get returns null for unknown key`() {
        val result = PaywallActivityNonSerializableArgsStore.get(12345)

        assertThat(result).isNull()
    }

    @Test
    fun `remove deletes entry for given key`() {
        val args = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val key = PaywallActivityNonSerializableArgsStore.store(args)

        PaywallActivityNonSerializableArgsStore.remove(key)

        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isNull()
    }

    @Test
    fun `remove does not affect other entries`() {
        val args1 = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val args2 = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val key1 = PaywallActivityNonSerializableArgsStore.store(args1)
        val key2 = PaywallActivityNonSerializableArgsStore.store(args2)

        PaywallActivityNonSerializableArgsStore.remove(key1)

        assertThat(PaywallActivityNonSerializableArgsStore.get(key1)).isNull()
        assertThat(PaywallActivityNonSerializableArgsStore.get(key2)).isSameAs(args2)
    }

    @Test
    fun `multiple entries can be stored and retrieved independently`() {
        val args1 = PaywallActivityNonSerializableArgs(
            purchaseLogic = mockk<PurchaseLogic>(),
        )
        val args2 = PaywallActivityNonSerializableArgs(
            listener = mockk<PaywallListener>(),
        )
        val args3 = PaywallActivityNonSerializableArgs(
            purchaseLogic = mockk<PurchaseLogic>(),
            listener = mockk<PaywallListener>(),
        )

        val key1 = PaywallActivityNonSerializableArgsStore.store(args1)
        val key2 = PaywallActivityNonSerializableArgsStore.store(args2)
        val key3 = PaywallActivityNonSerializableArgsStore.store(args3)

        assertThat(key1).isNotEqualTo(key2)
        assertThat(key2).isNotEqualTo(key3)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key1)).isSameAs(args1)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key2)).isSameAs(args2)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key3)).isSameAs(args3)
    }

    @Test
    fun `clear removes all entries`() {
        val args1 = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val args2 = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val key1 = PaywallActivityNonSerializableArgsStore.store(args1)
        val key2 = PaywallActivityNonSerializableArgsStore.store(args2)

        PaywallActivityNonSerializableArgsStore.clear()

        assertThat(PaywallActivityNonSerializableArgsStore.get(key1)).isNull()
        assertThat(PaywallActivityNonSerializableArgsStore.get(key2)).isNull()
    }

    @Test
    fun `store uses System identityHashCode as key`() {
        val args = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val expectedKey = System.identityHashCode(args)

        val key = PaywallActivityNonSerializableArgsStore.store(args)

        assertThat(key).isEqualTo(expectedKey)
    }

    @Test
    fun `store with only purchaseLogic works`() {
        val purchaseLogic = mockk<PurchaseLogic>()
        val args = PaywallActivityNonSerializableArgs(purchaseLogic = purchaseLogic)
        val key = PaywallActivityNonSerializableArgsStore.store(args)

        val retrieved = PaywallActivityNonSerializableArgsStore.get(key)

        assertThat(retrieved?.purchaseLogic).isSameAs(purchaseLogic)
        assertThat(retrieved?.listener).isNull()
    }

    @Test
    fun `store with only listener works`() {
        val listener = mockk<PaywallListener>()
        val args = PaywallActivityNonSerializableArgs(listener = listener)
        val key = PaywallActivityNonSerializableArgsStore.store(args)

        val retrieved = PaywallActivityNonSerializableArgsStore.get(key)

        assertThat(retrieved?.purchaseLogic).isNull()
        assertThat(retrieved?.listener).isSameAs(listener)
    }

    @Test
    fun `remove with unknown key does not throw`() {
        PaywallActivityNonSerializableArgsStore.remove(99999)
    }
}
