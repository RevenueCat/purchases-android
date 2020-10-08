package com.revenuecat.purchases

import com.revenuecat.purchases.common.Dispatcher
import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

class SyncDispatcher : Dispatcher(mockk()) {

    private var closed = false

    override fun enqueue(command: Runnable, randomDelay: Boolean) {
        if (closed) {
            throw RejectedExecutionException()
        }
        command.run()
    }

    override fun close() {
        closed = true
    }

    override fun isClosed(): Boolean {
        return closed
    }
}
