package com.revenuecat.purchases.utils

import android.os.Handler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

object MockHandlerFactory {
    fun createMockHandler(): Handler {
        val handler = mockk<Handler>()
        val slot = slot<Runnable>()
        every {
            handler.post(capture(slot))
        } answers {
            slot.captured.run()
            true
        }
        val delayedSlot = slot<Runnable>()
        every {
            handler.postDelayed(capture(delayedSlot), any())
        } answers {
            delayedSlot.captured.run()
            true
        }
        return handler
    }
}
