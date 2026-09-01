package com.revenuecat.purchases.perf

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Wraps [base] so any request whose path contains [match] fails with [code] instead of its normal
 * response. Used to verify the SDK degrades gracefully when a non-critical endpoint fails — no
 * latency is injected, since wall-clock timing is deliberately not asserted by these tests.
 */
fun Dispatcher.withPathFailure(match: String, code: Int = 500): Dispatcher {
    val base = this
    return object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            if ((request.path ?: "").contains(match)) {
                return MockResponse().setResponseCode(code).setBody("{}")
            }
            return base.dispatch(request)
        }
    }
}
