package com.revenuecat.purchases.utils

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CurrentActivityTrackerTest {

    private val tracker = CurrentActivityTracker()

    @Test
    fun `currentActivity is null before any activity starts`() {
        assertThat(tracker.currentActivity).isNull()
    }

    @Test
    fun `currentActivity is the last started activity`() {
        val firstActivity = mockActivity()
        val secondActivity = mockActivity()

        tracker.onActivityStarted(firstActivity)
        tracker.onActivityStarted(secondActivity)

        assertThat(tracker.currentActivity).isEqualTo(secondActivity)
    }

    @Test
    fun `currentActivity is null after the tracked activity stops`() {
        val activity = mockActivity()
        tracker.onActivityStarted(activity)

        tracker.onActivityStopped(activity)

        assertThat(tracker.currentActivity).isNull()
    }

    @Test
    fun `stopping a different activity keeps the tracked one`() {
        val activity = mockActivity()
        tracker.onActivityStarted(activity)

        tracker.onActivityStopped(mockActivity())

        assertThat(tracker.currentActivity).isEqualTo(activity)
    }

    @Test
    fun `stopping an overlay activity restores the previously started one`() {
        val activity = mockActivity()
        val overlayActivity = mockActivity()
        tracker.onActivityStarted(activity)
        tracker.onActivityStarted(overlayActivity)

        tracker.onActivityStopped(overlayActivity)

        assertThat(tracker.currentActivity).isEqualTo(activity)
    }

    @Test
    fun `a finishing activity is skipped in favor of the usable one underneath`() {
        val activity = mockActivity()
        val finishingActivity = mockActivity(finishing = true)
        tracker.onActivityStarted(activity)
        tracker.onActivityStarted(finishingActivity)

        assertThat(tracker.currentActivity).isEqualTo(activity)
    }

    @Test
    fun `a destroyed activity is skipped in favor of the usable one underneath`() {
        val activity = mockActivity()
        val destroyedActivity = mockActivity(destroyed = true)
        tracker.onActivityStarted(activity)
        tracker.onActivityStarted(destroyedActivity)

        assertThat(tracker.currentActivity).isEqualTo(activity)
    }

    @Test
    fun `currentActivity is null when the only started activity is finishing`() {
        tracker.onActivityStarted(mockActivity(finishing = true))

        assertThat(tracker.currentActivity).isNull()
    }

    private fun mockActivity(finishing: Boolean = false, destroyed: Boolean = false): Activity = mockk {
        every { isFinishing } returns finishing
        every { isDestroyed } returns destroyed
    }
}
