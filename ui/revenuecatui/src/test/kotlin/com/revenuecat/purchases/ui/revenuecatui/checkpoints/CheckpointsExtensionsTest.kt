package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointsExtensionsTest {

    @Test
    fun `the manager is created once and kept on the Purchases instance`() {
        val purchases = purchasesWithSlot()

        val manager = purchases.checkpointsManager

        assertThat(purchases.checkpointsManager).isSameAs(manager)
        assertThat(purchases.internalCpManagerSlot).isSameAs(manager)
    }

    @Test
    fun `each Purchases instance gets its own manager`() {
        val first = purchasesWithSlot()
        val second = purchasesWithSlot()

        assertThat(first.checkpointsManager).isNotSameAs(second.checkpointsManager)
    }

    // Mirrors the real slot: a per-instance field the UI module reads and writes.
    private fun purchasesWithSlot(): Purchases {
        var slot: Any? = null
        return mockk {
            every { internalCpManagerSlot } answers { slot }
            every { internalCpManagerSlot = any() } answers { slot = firstArg() }
        }
    }
}
