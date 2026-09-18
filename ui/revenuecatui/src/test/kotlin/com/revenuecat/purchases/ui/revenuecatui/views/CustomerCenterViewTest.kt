package com.revenuecat.purchases.ui.revenuecatui.views

import android.os.Looper
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.revenuecat.purchases.Purchases
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowChoreographer

@RunWith(RobolectricTestRunner::class)
class CustomerCenterViewTest {

    private val mockPurchases = mockk<Purchases>(relaxed = true)
    private lateinit var activity: CustomerCenterViewHostActivity

    @Before
    fun setUp() {
        mockkObject(Purchases)
        every { Purchases.sharedInstance } returns mockPurchases
        every { mockPurchases.customerCenterListener } returns null
        every { mockPurchases.preferredUILocaleOverride } returns null
        every { mockPurchases.track(any()) } just Runs
        activity = Robolectric.buildActivity(CustomerCenterViewHostActivity::class.java).setup().get()
        ShadowChoreographer.setPaused(true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `navigateBack dismisses the view when there is no screen to go back to`() {
        var dismissed = false
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val view = CustomerCenterView(context = activity, dismissHandler = { dismissed = true })
        container.addView(view)
        shadowOf(Looper.getMainLooper()).idle()

        view.navigateBack()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `navigateBack does nothing before the view is attached`() {
        var dismissed = false
        val view = CustomerCenterView(context = activity, dismissHandler = { dismissed = true })

        view.navigateBack()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(dismissed).isFalse()
    }
}

class CustomerCenterViewHostActivity : ComponentActivity()
