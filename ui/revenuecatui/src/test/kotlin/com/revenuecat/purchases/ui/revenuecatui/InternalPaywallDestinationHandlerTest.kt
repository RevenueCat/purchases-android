package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.Runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InternalPaywallDestinationHandlerTest {

    @Test
    fun `launchCustomerCenter adds NEW_TASK flag without Activity context`() {
        val context = mockk<Context>(relaxed = true)
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        context.launchCustomerCenter()

        verify(exactly = 1) { context.startActivity(any()) }
        assertThat(intentSlot.captured.component?.className).isEqualTo(CustomerCenterActivity::class.java.name)
        assertThat(intentSlot.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `launchCustomerCenter does not add NEW_TASK flag with Activity context`() {
        val context = mockk<Activity>(relaxed = true)
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        context.launchCustomerCenter()

        verify(exactly = 1) { context.startActivity(any()) }
        assertThat(intentSlot.captured.component?.className).isEqualTo(CustomerCenterActivity::class.java.name)
        assertThat(intentSlot.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(0)
        assertThat(context.getActivity()).isNotNull()
    }
}
