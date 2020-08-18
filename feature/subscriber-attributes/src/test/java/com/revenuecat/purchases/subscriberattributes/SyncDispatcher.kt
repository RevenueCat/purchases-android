package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.common.Dispatcher
import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

class SyncDispatcher : Dispatcher(mockk()) {

    private var closed = false

    override fun enqueue(call: AsyncCall) {
        if (closed) {
            throw RejectedExecutionException()
        }
        call.run()
    }

    override fun close() {
        closed = true
    }

    override fun isClosed(): Boolean {
        return closed
    }
}
