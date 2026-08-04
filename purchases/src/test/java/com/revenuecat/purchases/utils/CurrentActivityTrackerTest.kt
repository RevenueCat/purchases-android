package com.revenuecat.purchases.utils

import android.app.Activity
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
        val firstActivity = mockk<Activity>()
        val secondActivity = mockk<Activity>()

        tracker.onActivityStarted(firstActivity)
        tracker.onActivityStarted(secondActivity)

        assertThat(tracker.currentActivity).isEqualTo(secondActivity)
    }

    @Test
    fun `currentActivity is null after the tracked activity stops`() {
        val activity = mockk<Activity>()
        tracker.onActivityStarted(activity)

        tracker.onActivityStopped(activity)

        assertThat(tracker.currentActivity).isNull()
    }

    @Test
    fun `stopping a different activity keeps the tracked one`() {
        val activity = mockk<Activity>()
        tracker.onActivityStarted(activity)

        tracker.onActivityStopped(mockk<Activity>())

        assertThat(tracker.currentActivity).isEqualTo(activity)
    }
}
