package com.revenuecat.purchases.common.networking

internal object RCHTTPStatusCodes {
    const val SUCCESS = 200
    const val CREATED = 201
    const val UNSUCCESSFUL = 300
    const val NOT_MODIFIED = 304
    const val BAD_REQUEST = 400
    const val NOT_FOUND = 404
    const val ERROR = 500

    fun isSuccessful(statusCode: Int) = statusCode < BAD_REQUEST
    fun isServerError(statusCode: Int) = statusCode >= ERROR
}
