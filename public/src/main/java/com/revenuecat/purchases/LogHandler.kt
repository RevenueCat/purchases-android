package com.revenuecat.purchases

/**
 * Interface that allows handling logs manually.
 * See also [Purchases.logHandler]
 */
interface LogHandler {
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String, throwable: Throwable?)
}
