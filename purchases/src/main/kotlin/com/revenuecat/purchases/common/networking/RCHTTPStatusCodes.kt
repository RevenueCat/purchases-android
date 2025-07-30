package com.revenuecat.purchases.common.networking

internal object RCHTTPStatusCodes {
    const val SUCCESS = 200
    const val CREATED = 201
    const val UNSUCCESSFUL = 300
    const val NOT_MODIFIED = 304
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val ERROR = 500

    fun isSuccessful(statusCode: Int) = statusCode < BAD_REQUEST
    fun isServerError(statusCode: Int) = statusCode >= ERROR

    // Note: this means that all 4xx (except 404) are considered as successfully synced.
    // The reason is because it's likely due to a client error, so continuing to retry
    // won't yield any different results and instead kill pandas.
    fun isSynced(statusCode: Int) = isSuccessful(statusCode) || !(isServerError(statusCode) || statusCode == NOT_FOUND)
}
