package com.revenuecat.purchases.strings

object NetworkStrings {
    const val API_REQUEST_COMPLETED = "API request completed with status: %s %s %s"
    const val API_REQUEST_STARTED = "API request started: %s %s"
    const val HTTP_RESPONSE_PAYLOAD_NULL = "HTTP Response payload is null"
    const val ETAG_RETRYING_CALL = "We were expecting to be able to return a cached response, but we can't find it. " +
        "Retrying call with a new ETag"
    const val ETAG_CALL_ALREADY_RETRIED = "We can't find the cached response, but call has already been retried. " +
        "Returning result from backend: %s"
    const val SAME_CALL_ALREADY_IN_PROGRESS = "Same call already in progress, adding to callbacks map with key: %s"
    const val PROBLEM_CONNECTING = "Unable to start a network connection due to a network configuration issue: %s"
}
