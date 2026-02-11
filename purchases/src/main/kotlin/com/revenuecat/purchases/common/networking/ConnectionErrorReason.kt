package com.revenuecat.purchases.common.networking

import java.io.IOException

internal enum class ConnectionErrorReason {
    TIMEOUT,
    NO_NETWORK,
    OTHER,
    ;

    public companion object {
        public fun fromIOException(ioException: IOException): ConnectionErrorReason {
            return when (ioException) {
                is java.net.SocketTimeoutException -> TIMEOUT
                is java.net.ConnectException,
                is java.net.UnknownHostException,
                -> NO_NETWORK
                else -> OTHER
            }
        }
    }
}
