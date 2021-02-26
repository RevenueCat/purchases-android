package com.revenuecat.purchases.utils

import com.revenuecat.purchases.common.Dispatcher
import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

class SyncDispatcher : Dispatcher(mockk()) {

    private var closed = false

    override fun enqueue(command: Runnable, useRandomDelay: Boolean) {
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
