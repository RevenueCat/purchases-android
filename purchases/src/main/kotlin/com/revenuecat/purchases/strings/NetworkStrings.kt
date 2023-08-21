package com.revenuecat.purchases.strings

internal object NetworkStrings {
    const val API_REQUEST_COMPLETED = "API request completed with status: %s %s %s"
    const val API_REQUEST_STARTED = "API request started: %s %s"
    const val HTTP_RESPONSE_PAYLOAD_NULL = "HTTP Response payload is null"
    const val ETAG_RETRYING_CALL = "We were expecting to be able to return a cached response, but we can't find it. " +
        "Retrying call with a new ETag"
    const val ETAG_CALL_ALREADY_RETRIED = "We can't find the cached response, but call has already been retried. " +
        "Returning result from backend: %s"
    const val SAME_CALL_SCHEDULED_WITHOUT_JITTER = "Request already scheduled without jitter delay, adding " +
        "callbacks to unjittered request with key: %s"
    const val SAME_CALL_SCHEDULED_WITH_JITTER = "Request already scheduled with jitter delay, adding existing " +
        "callbacks to unjittered request with key: %s"
    const val SAME_CALL_ALREADY_IN_PROGRESS = "Same call already in progress, adding to callbacks map with key: %s"
    const val PROBLEM_CONNECTING = "Unable to start a network connection due to a network configuration issue: %s"
    const val VERIFICATION_MISSING_SIGNATURE = "Verification: Request to '%s' requires a signature but none provided."
    const val VERIFICATION_INTERMEDIATE_KEY_FAILED = "Verification: Request to '%s' provided an intermediate key that" +
        " did not verify correctly. Reason %s"
    const val VERIFICATION_MISSING_REQUEST_TIME = "Verification: Request to '%s' requires a request time" +
        " but none provided."
    const val VERIFICATION_MISSING_BODY_OR_ETAG = "Verification: Request to '%s' requires a body or etag" +
        " but none provided."
    const val VERIFICATION_INVALID_SIZE = "Verification: Request to '%s' has signature with wrong size. '%s'"
    const val VERIFICATION_ERROR = "Verification: Request to '%s' failed verification."
    const val VERIFICATION_SUCCESS = "Verification: Request to '%s' verified successfully."
}
