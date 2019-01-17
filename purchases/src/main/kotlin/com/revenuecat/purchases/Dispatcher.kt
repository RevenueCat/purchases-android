//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper

import java.util.concurrent.ExecutorService

internal open class Dispatcher(private val executorService: ExecutorService) {

    internal abstract class AsyncCall : Runnable {
        @Throws(HTTPClient.HTTPErrorException::class)
        abstract fun call(): HTTPClient.Result

        open fun onError(code: Int, message: String) {}
        open fun onCompletion(result: HTTPClient.Result) {}

        override fun run() {
            val mainHandler = Handler(Looper.getMainLooper())
            try {
                val result = call()
                mainHandler.post { onCompletion(result) }
            } catch (e: HTTPClient.HTTPErrorException) {
                mainHandler.post { onError(0, e.message!!) }
            }
        }
    }

    open fun enqueue(call: AsyncCall) {
        this.executorService.submit(call)
    }

    open fun close() {
        this.executorService.shutdownNow()
    }

    open fun isClosed() = this.executorService.isShutdown
}
