//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import org.json.JSONException
import java.io.IOException
import java.util.concurrent.ExecutorService

internal open class Dispatcher(
    private val executorService: ExecutorService
) {

    internal abstract class AsyncCall : Runnable {
        @Throws(JSONException::class, IOException::class)
        abstract fun call(): HTTPClient.Result

        open fun onError(error: PurchasesError) {}
        open fun onCompletion(result: HTTPClient.Result) {}

        override fun run() {
            try {
                onCompletion(call())
            } catch (e: JSONException) {
                onError(e.toPurchasesError())
            } catch (e: IOException) {
                onError(e.toPurchasesError())
            } catch (e: SecurityException) {
                // This can happen if a user disables the INTERNET permission.
                onError(e.toPurchasesError())
            }
        }
    }

    open fun enqueue(call: AsyncCall) {
        this.executorService.execute(call)
    }

    open fun close() {
        this.executorService.shutdownNow()
    }

    open fun isClosed() = this.executorService.isShutdown
}
