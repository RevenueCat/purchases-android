package com.revenuecat.purchases.utils

import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import io.mockk.mockk
import java.util.concurrent.RejectedExecutionException

internal class SyncDispatcher : Dispatcher(mockk(), MockHandlerFactory.createMockHandler()) {

    private var closed = false

    override fun enqueue(command: Runnable, delay: Delay) {
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
