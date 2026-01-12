package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatStoreAPI
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

@InternalRevenueCatStoreAPI
typealias SerialRequest = (finish: () -> Unit) -> Unit

@InternalRevenueCatStoreAPI
class SerialRequestExecutor {

    private val lock = Any()
    private val pendingRequests = ArrayDeque<SerialRequest>()
    private var hasActiveRequest: Boolean = false

    /**
     * Queues [request] to run after any in-flight request finishes. The provided [finish] callback
     * **must** be called exactly once by the request, from either its success or error path, to allow
     * the next queued request to start.
     */
    fun executeSerially(request: SerialRequest) {
        val shouldStartNext = synchronized(lock) {
            pendingRequests.addLast(request)
            if (!hasActiveRequest) {
                hasActiveRequest = true
                true
            } else {
                false
            }
        }

        if (shouldStartNext) {
            executeNextRequest()
        }
    }

    private fun executeNextRequest() {
        val nextRequest = synchronized(lock) {
            pendingRequests.firstOrNull().also { request ->
                if (request == null) {
                    hasActiveRequest = false
                }
            }
        } ?: return

        val hasFinished = AtomicBoolean(false)
        nextRequest {
            if (hasFinished.compareAndSet(false, true)) {
                onRequestFinished()
            }
        }
    }

    private fun onRequestFinished() {
        val hasNextRequest = synchronized(lock) {
            if (pendingRequests.isNotEmpty()) {
                pendingRequests.removeFirst()
            }
            val hasMoreRequests = pendingRequests.isNotEmpty()
            if (!hasMoreRequests) {
                hasActiveRequest = false
            }
            hasMoreRequests
        }

        if (hasNextRequest) {
            executeNextRequest()
        }
    }
}
