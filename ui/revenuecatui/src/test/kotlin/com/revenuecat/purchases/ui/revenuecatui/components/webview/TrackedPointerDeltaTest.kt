package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TrackedPointerDeltaTest {

    private fun pointerEvent(action: Int, vararg pointers: Triple<Int, Float, Float>): MotionEvent {
        val properties = pointers.map { (pointerId, _, _) ->
            PointerProperties().apply { id = pointerId }
        }.toTypedArray()
        val coords = pointers.map { (_, pointerX, pointerY) ->
            PointerCoords().apply {
                x = pointerX
                y = pointerY
            }
        }.toTypedArray()
        return MotionEvent.obtain(
            0L,
            0L,
            action,
            pointers.size,
            properties,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0,
        )
    }

    @Test
    fun `resolves the delta of the tracked pointer when it's the only one down`() {
        val event = pointerEvent(MotionEvent.ACTION_MOVE, Triple(0, 130f, 550f))

        val delta = trackedPointerDelta(event, trackedPointerId = 0, downX = 100f, downY = 500f)

        assertThat(delta).isEqualTo(30f to 50f)
    }

    @Test
    fun `follows the tracked pointer id when a second finger shifted the index`() {
        // Pointer 0 (the tracked finger) is now at index 1; a naive index-0 read would return pointer 1.
        val event = pointerEvent(
            MotionEvent.ACTION_MOVE,
            Triple(1, 900f, 900f),
            Triple(0, 130f, 550f),
        )

        val delta = trackedPointerDelta(event, trackedPointerId = 0, downX = 100f, downY = 500f)

        assertThat(delta).isEqualTo(30f to 50f)
    }

    @Test
    fun `returns null once the tracked pointer has lifted, even if another finger remains`() {
        // Only pointer 1 remains; index 0 now belongs to a finger that never set downX/downY.
        val event = pointerEvent(MotionEvent.ACTION_MOVE, Triple(1, 900f, 900f))

        val delta = trackedPointerDelta(event, trackedPointerId = 0, downX = 100f, downY = 500f)

        assertThat(delta).isNull()
    }
}
