//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import com.revenuecat.purchases.PurchasesError
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.ExecutorService

open class Dispatcher(
    private val executorService: ExecutorService
) {

    abstract class AsyncCall : Runnable {
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
        synchronized(this.executorService) {
            this.executorService.execute(call)
        }
    }

    open fun close() {
        synchronized(this.executorService) {
            this.executorService.shutdownNow()
        }
    }

    open fun isClosed() = synchronized(this.executorService) { this.executorService.isShutdown }
}
