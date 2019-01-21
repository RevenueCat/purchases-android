//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import java.util.concurrent.ExecutorService

internal open class Dispatcher(
    private val executorService: ExecutorService
) {

    internal abstract class AsyncCall : Runnable {
        @Throws(HTTPClient.HTTPErrorException::class)
        abstract fun call(): HTTPClient.Result

        open fun onError(code: Int, message: String) {}
        open fun onCompletion(result: HTTPClient.Result) {}

        override fun run() {
            try {
                onCompletion(call())
            } catch (e: HTTPClient.HTTPErrorException) {
                onError(0, e.message!!)
            }
        }
    }

    open fun enqueue(call: Dispatcher.AsyncCall) {
        this.executorService.submit(call)
    }

    open fun close() {
        this.executorService.shutdownNow()
    }

    open fun isClosed() = this.executorService.isShutdown
}
