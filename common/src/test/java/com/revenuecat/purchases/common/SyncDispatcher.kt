package com.revenuecat.purchases.common

import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

internal class SyncDispatcher : Dispatcher(mockk()) {

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
