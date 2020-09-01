package com.revenuecat.purchases.common

import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

internal class SyncDispatcher : Dispatcher(mockk()) {

    private var closed = false

    override fun enqueue(command: Runnable) {
        if (closed) {
            throw RejectedExecutionException()
        }
        command.run()
    }

    override fun enqueue(command: () -> Unit) {
        enqueue(Runnable { command.invoke() })
    }

    override fun close() {
        closed = true
    }

    override fun isClosed(): Boolean {
        return closed
    }
}
