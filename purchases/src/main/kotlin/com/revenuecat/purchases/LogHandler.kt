package com.revenuecat.purchases

/**
 * Interface that allows handling logs manually.
 * See also [Purchases.logHandler]
 */
public interface LogHandler {
    public fun v(tag: String, msg: String)
    public fun d(tag: String, msg: String)
    public fun i(tag: String, msg: String)
    public fun w(tag: String, msg: String)
    public fun e(tag: String, msg: String, throwable: Throwable?)
}
