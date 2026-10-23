package com.revenuecat.purchases.virtualcurrencies

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs one operation at a time per user. Each operation gets a `done` function and must call it when it
 * finishes; the user's next operation starts then. Different users don't wait for each other.
 */
internal class PerUserSerialQueue {

    private val pending = mutableMapOf<String, ArrayDeque<(done: () -> Unit) -> Unit>>()

    fun enqueue(appUserID: String, operation: (done: () -> Unit) -> Unit) {
        val isOnlyOperation = synchronized(pending) {
            val queue = pending.getOrPut(appUserID) { ArrayDeque() }
            queue.addLast(operation)
            queue.size == 1
        }
        if (isOnlyOperation) run(appUserID, operation)
    }

    private fun run(appUserID: String, operation: (done: () -> Unit) -> Unit) {
        // Only the first `done` call counts, so calling it twice can't start an operation early.
        val finished = AtomicBoolean(false)
        operation {
            if (finished.compareAndSet(false, true)) finish(appUserID)
        }
    }

    private fun finish(appUserID: String) {
        val next = synchronized(pending) {
            val queue = pending.getValue(appUserID)
            queue.removeFirst()
            if (queue.isEmpty()) {
                pending.remove(appUserID)
                null
            } else {
                queue.first()
            }
        }
        next?.let { run(appUserID, it) }
    }
}
