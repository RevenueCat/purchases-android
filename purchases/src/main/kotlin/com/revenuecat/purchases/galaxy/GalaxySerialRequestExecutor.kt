package com.revenuecat.purchases.galaxy

import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

internal typealias GalaxySerialRequest = (finish: () -> Unit) -> Unit

internal class GalaxySerialRequestExecutor {

    private val lock = Any()
    private val pendingRequests = ArrayDeque<GalaxySerialRequest>()
    private var hasActiveRequest: Boolean = false

    /**
     * Queues [request] to run after any in-flight request finishes. The provided [finish] callback
     * **must** be called exactly once by the request, from either its success or error path, to allow
     * the next queued request to start.
     */
    fun executeSerially(request: GalaxySerialRequest) {
        val shouldStartNext = synchronized(lock) {
            pendingRequests.addLast(request)
            println("GalaxySerialRequestExecutor: enqueued request; total queued=${pendingRequests.size}")
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
            pendingRequests.firstOrNull()
        } ?: run {
            synchronized(lock) { hasActiveRequest = false }
            return
        }

        val hasFinished = AtomicBoolean(false)
        println("GalaxySerialRequestExecutor: executing next request; remaining queued=${pendingRequests.size}")
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
            println("GalaxySerialRequestExecutor: advancing to next request; remaining queued=${pendingRequests.size}")
            executeNextRequest()
        } else {
            println("GalaxySerialRequestExecutor: queue drained; no pending requests")
        }
    }
}
