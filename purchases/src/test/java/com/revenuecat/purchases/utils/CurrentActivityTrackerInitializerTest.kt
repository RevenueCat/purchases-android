package com.revenuecat.purchases.utils

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class CurrentActivityTrackerInitializerTest {

    private val callbacks = slot<ActivityLifecycleCallbacks>()
    private val application = mockk<Application>(relaxed = true) {
        every { registerActivityLifecycleCallbacks(capture(callbacks)) } returns Unit
    }
    private val context = mockk<Context> {
        every { applicationContext } returns application
    }

    private val startedActivity = mockk<Activity> {
        every { isFinishing } returns false
        every { isDestroyed } returns false
    }

    // The tracker is process-wide, so leave it as found.
    @After
    fun tearDown() {
        CurrentActivityTracker.onActivityStopped(startedActivity)
    }

    @Test
    fun `tracks an activity that starts before Purchases is configured`() {
        CurrentActivityTrackerInitializer().create(context)

        callbacks.captured.onActivityStarted(startedActivity)

        assertThat(CurrentActivityTracker.currentActivity).isEqualTo(startedActivity)
    }

    @Test
    fun `stops tracking an activity that stops`() {
        CurrentActivityTrackerInitializer().create(context)
        callbacks.captured.onActivityStarted(startedActivity)

        callbacks.captured.onActivityStopped(startedActivity)

        assertThat(CurrentActivityTracker.currentActivity).isNull()
    }

    @Test
    fun `registers a single callback on the application`() {
        CurrentActivityTrackerInitializer().create(context)

        verify(exactly = 1) { application.registerActivityLifecycleCallbacks(any()) }
    }
}
