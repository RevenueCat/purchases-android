package com.revenuecat.purchases.common

import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

internal class SyncDispatcher : Dispatcher(mockk()) {

    private var closed = false
    var calledWithRandomDelay: Boolean? = null

    override fun enqueue(command: Runnable, useRandomDelay: Boolean) {
        calledWithRandomDelay = useRandomDelay
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
