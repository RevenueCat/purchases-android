package com.revenuecat.purchases.logging

/**
 * Interface that allows handling logs manually.
 * See also [Purchases.logHandler]
 */
interface VerboseLogHandler {
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        elements: Array<StackTraceElement>?
    )
}
