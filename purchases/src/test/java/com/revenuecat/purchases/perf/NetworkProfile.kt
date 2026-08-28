package com.revenuecat.purchases.perf

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.TimeUnit

enum class NetworkProfile(val perRequestDelayMs: Long) {
    GOOD(0),
    BAD(400),
    FLAKY(400),
}

fun NetworkProfile.decorate(base: Dispatcher, failMatch: String? = null): Dispatcher {
    val profile = this
    return object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path ?: ""
            val response = if (profile == NetworkProfile.FLAKY && failMatch != null && path.contains(failMatch)) {
                MockResponse().setResponseCode(500).setBody("{}")
            } else {
                base.dispatch(request)
            }
            if (profile.perRequestDelayMs > 0) {
                response.setHeadersDelay(profile.perRequestDelayMs, TimeUnit.MILLISECONDS)
            }
            return response
        }
    }
}

/**
 * Wraps [base] so any request whose path contains [match] gets an extra [delayMs] added on top of
 * whatever [base] would otherwise respond with (unlike [NetworkProfile.decorate], which delays every
 * request uniformly). Used to simulate a slow single endpoint (e.g. `/config`) in isolation, so a
 * test can measure exactly how much of that one hop's latency leaks into the overall critical path.
 */
fun Dispatcher.withPathDelay(match: String, delayMs: Long): Dispatcher {
    val base = this
    return object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val response = base.dispatch(request)
            if (delayMs > 0 && (request.path ?: "").contains(match)) {
                response.setHeadersDelay(delayMs, TimeUnit.MILLISECONDS)
            }
            return response
        }
    }
}
